package net.bandit.darkdoppelganger.event;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerMinionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID)
public class ServerEvents {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        AABB box = new AABB(event.getEntity().blockPosition()).inflate(128);
        level.getEntitiesOfClass(DarkDoppelgangerMinionEntity.class, box, e -> true)
                .forEach(LivingEntity::discard);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        AABB box = new AABB(event.getEntity().blockPosition()).inflate(128);
        level.getEntitiesOfClass(DarkDoppelgangerMinionEntity.class, box, e -> true)
                .forEach(LivingEntity::discard);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        AABB box = new AABB(event.getEntity().blockPosition()).inflate(128);
        level.getEntitiesOfClass(DarkDoppelgangerMinionEntity.class, box, e -> true)
                .forEach(LivingEntity::discard);
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        AABB box = new AABB(-3_000_000, -256, -3_000_000, 3_000_000, 320, 3_000_000);
        level.getEntitiesOfClass(DarkDoppelgangerMinionEntity.class, box, e -> true)
                .forEach(LivingEntity::discard);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        event.getServer().getAllLevels().forEach(level -> {
            AABB box = new AABB(-3_000_000, -256, -3_000_000, 3_000_000, 320, 3_000_000);
            level.getEntitiesOfClass(DarkDoppelgangerMinionEntity.class, box, e -> true)
                    .forEach(LivingEntity::discard);
        });
    }
}
