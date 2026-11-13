package net.bandit.darkdoppelganger.cutscene;

import com.mojang.logging.LogUtils;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.network.CutscenePayloads;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the intro cutscene: who is watching and until when.
 * <p>
 * The boss runs the cutscene's events on its own ticks; this class only tracks the players.
 * Their client holds them in place, even in the air. A watcher who moves anyway stops watching.
 * Watchers cannot be hurt, except by damage that ignores invulnerability, such as /kill. The
 * intro ends early if every watcher leaves, dies or changes dimension.
 */
@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID)
public final class CutsceneTracker {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Players this close to the boss when a cutscene starts watch it, if they can see where it appears. */
    public static final double WATCH_RANGE = 48.0;
    /** Height above the boss's feet that the camera aims at, and that a viewer must be able to see. */
    public static final double LOOK_HEIGHT = 1.2;
    /** Move packets sent before the client got the cutscene can still arrive for this long. */
    private static final int SETTLE_TICKS = 20;
    private static final double MOVE_TOLERANCE = 1.0;

    private static final class Viewing {
        final ServerPlayer player;
        final UUID boss;
        final int endTick;
        final int settleTick;
        Vec3 anchor;

        Viewing(ServerPlayer player, UUID boss, int endTick, int settleTick) {
            this.player = player;
            this.boss = boss;
            this.endTick = endTick;
            this.settleTick = settleTick;
            this.anchor = player.position();
        }
    }

    private static final Map<UUID, Viewing> VIEWERS = new HashMap<>();
    private static boolean includeFakePlayers = false;

    private CutsceneTracker() {
    }

    /** Players who would watch a cutscene for this boss: nearby, able to see it, alive, not spectating, not fake. */
    public static List<ServerPlayer> nearbyViewers(Entity boss) {
        if (!(boss.level() instanceof ServerLevel level)) return List.of();
        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if ((player instanceof FakePlayer && !includeFakePlayers) || !player.isAlive() || player.isSpectator()) continue;
            if (isWatching(player)) continue; // already in another boss's cutscene
            if (player.distanceToSqr(boss) > WATCH_RANGE * WATCH_RANGE) continue;
            if (canSee(player, boss.position())) viewers.add(player);
        }
        return viewers;
    }

    /**
     * True when nothing blocks the line from the spot to the player's eyes. The camera starts and
     * ends on that line, so a viewer without it would get a camera pushed out of a wall.
     */
    public static boolean canSee(Player player, Vec3 spot) {
        Vec3 target = spot.add(0.0, LOOK_HEIGHT, 0.0);
        return player.level().clip(new ClipContext(target, player.getEyePosition(), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player))
                .getType() == HitResult.Type.MISS;
    }

    /** Starts the cutscene for these players, with the boss emerging after {@code emerge} ticks. */
    public static void start(Entity boss, int duration, int emerge, List<ServerPlayer> viewers) {
        if (boss.getServer() == null) return;
        int now = boss.getServer().getTickCount();
        for (ServerPlayer player : viewers) {
            if (player.hasDisconnected() || player.level() != boss.level()) continue;
            VIEWERS.put(player.getUUID(), new Viewing(player, boss.getUUID(), now + duration, now + SETTLE_TICKS));
            PacketDistributor.sendToPlayer(player, new CutscenePayloads.Start(boss.getX(), boss.getY(), boss.getZ(), duration, emerge));
        }
    }

    public static boolean isWatching(ServerPlayer player) {
        Viewing viewing = VIEWERS.get(player.getUUID());
        return viewing != null && player.getServer() != null && viewing.endTick > player.getServer().getTickCount();
    }

    /** True while any player is still watching this boss's cutscene. */
    public static boolean anyWatching(UUID boss, MinecraftServer server) {
        int now = server.getTickCount();
        for (Viewing viewing : VIEWERS.values()) {
            if (viewing.boss.equals(boss) && viewing.endTick > now) return true;
        }
        return false;
    }

    /** Ends the cutscene for everyone still watching this boss, and returns their cameras. */
    public static void endAll(UUID boss) {
        VIEWERS.values().removeIf(viewing -> {
            if (!viewing.boss.equals(boss)) return false;
            PacketDistributor.sendToPlayer(viewing.player, new CutscenePayloads.End());
            return true;
        });
    }

    /**
     * Game tests only. Fake players are left out of cutscenes in real play (mod machines must not
     * hold a cutscene open), but the tests have no other players to watch with.
     */
    public static void setIncludeFakePlayersForTests(boolean include) {
        includeFakePlayers = include;
    }

    private static void stopWatching(ServerPlayer player) {
        if (VIEWERS.remove(player.getUUID()) != null) {
            PacketDistributor.sendToPlayer(player, new CutscenePayloads.End());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (VIEWERS.isEmpty()) return;
        int now = event.getServer().getTickCount();
        Iterator<Viewing> it = VIEWERS.values().iterator();
        while (it.hasNext()) {
            Viewing viewing = it.next();
            if (viewing.endTick <= now) {
                it.remove();
                continue;
            }
            if (now < viewing.settleTick) {
                viewing.anchor = viewing.player.position();
            } else if (viewing.player.position().distanceToSqr(viewing.anchor) > MOVE_TOLERANCE * MOVE_TOLERANCE) {
                it.remove();
                PacketDistributor.sendToPlayer(viewing.player, new CutscenePayloads.End());
                continue;
            }
            clearFloating(viewing.player.connection);
        }
    }

    private static Field clientIsFloating;
    private static Field clientVehicleIsFloating;
    private static boolean floatingFieldsMissing;

    /**
     * A player held in the air sends no moves, so the server would count every tick of the
     * cutscene as floating and kick them for flying once it ends. Clearing the flag each tick
     * restarts that count.
     */
    private static void clearFloating(ServerGamePacketListenerImpl connection) {
        if (floatingFieldsMissing || connection == null) return;
        try {
            if (clientIsFloating == null) {
                clientIsFloating = ServerGamePacketListenerImpl.class.getDeclaredField("clientIsFloating");
                clientVehicleIsFloating = ServerGamePacketListenerImpl.class.getDeclaredField("clientVehicleIsFloating");
                clientIsFloating.setAccessible(true);
                clientVehicleIsFloating.setAccessible(true);
            }
            clientIsFloating.setBoolean(connection, false);
            clientVehicleIsFloating.setBoolean(connection, false);
        } catch (ReflectiveOperationException | RuntimeException e) {
            floatingFieldsMissing = true;
            LOGGER.warn("Cannot clear the floating flag; players held in the air by the cutscene may be kicked for flying", e);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isWatching(player)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stopWatching(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        VIEWERS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stopWatching(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stopWatching(player);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        VIEWERS.clear();
    }
}
