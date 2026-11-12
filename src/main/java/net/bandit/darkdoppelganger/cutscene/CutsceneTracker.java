package net.bandit.darkdoppelganger.cutscene;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.network.CutscenePayloads;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the intro cutscene: who is watching and until when.
 * <p>
 * The boss runs the cutscene's events on its own ticks; this class only tracks the players.
 * A watcher cannot be hurt while they cannot move. The intro ends early if every watcher leaves,
 * dies or changes dimension.
 */
@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID)
public final class CutsceneTracker {
    /** Players this close to the boss when a cutscene starts watch it. */
    public static final double WATCH_RANGE = 48.0;

    private record Viewing(UUID boss, int endTick) {
    }

    private static final Map<UUID, Viewing> VIEWERS = new HashMap<>();
    private static boolean includeFakePlayers = false;

    private CutsceneTracker() {
    }

    /** Players who would watch a cutscene for this boss: nearby, alive, not spectating, not fake. */
    public static List<ServerPlayer> nearbyViewers(Entity boss) {
        if (!(boss.level() instanceof ServerLevel level)) return List.of();
        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if ((player instanceof FakePlayer && !includeFakePlayers) || !player.isAlive() || player.isSpectator()) continue;
            if (isWatching(player)) continue; // already in another boss's cutscene
            if (player.distanceToSqr(boss) <= WATCH_RANGE * WATCH_RANGE) viewers.add(player);
        }
        return viewers;
    }

    /** Starts the cutscene for these players, with the boss emerging after {@code emerge} ticks. Returns false when nobody watches. */
    public static boolean start(Entity boss, int duration, int emerge, List<ServerPlayer> viewers) {
        if (viewers.isEmpty() || boss.getServer() == null) return false;
        int endTick = boss.getServer().getTickCount() + duration;
        for (ServerPlayer player : viewers) {
            VIEWERS.put(player.getUUID(), new Viewing(boss.getUUID(), endTick));
            PacketDistributor.sendToPlayer(player, new CutscenePayloads.Start(boss.getX(), boss.getY(), boss.getZ(), duration, emerge));
        }
        return true;
    }

    public static boolean isWatching(ServerPlayer player) {
        Viewing viewing = VIEWERS.get(player.getUUID());
        return viewing != null && player.getServer() != null && viewing.endTick() > player.getServer().getTickCount();
    }

    /** True while any player is still watching this boss's cutscene. */
    public static boolean anyWatching(UUID boss, MinecraftServer server) {
        int now = server.getTickCount();
        for (Viewing viewing : VIEWERS.values()) {
            if (viewing.boss().equals(boss) && viewing.endTick() > now) return true;
        }
        return false;
    }

    /** Ends the cutscene for everyone still watching this boss, and returns their cameras. */
    public static void endAll(UUID boss, MinecraftServer server) {
        VIEWERS.entrySet().removeIf(entry -> {
            if (!entry.getValue().boss().equals(boss)) return false;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) PacketDistributor.sendToPlayer(player, new CutscenePayloads.End());
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
        VIEWERS.values().removeIf(viewing -> viewing.endTick() <= now);
    }

    /** Watchers cannot be hurt while they cannot move, including by other players. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isWatching(player)) {
            event.setCanceled(true);
        }
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
