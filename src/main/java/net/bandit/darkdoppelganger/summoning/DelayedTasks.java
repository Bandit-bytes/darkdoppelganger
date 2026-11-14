package net.bandit.darkdoppelganger.summoning;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Runs tasks on the server thread after a delay in ticks, without blocking the tick loop. */
@Mod.EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID)
public final class DelayedTasks {
    private record Task(int dueTick, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private DelayedTasks() {
    }

    /** Must be called on the server thread. */
    public static void schedule(MinecraftServer server, int delayTicks, Runnable action) {
        TASKS.add(new Task(server.getTickCount() + delayTicks, action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) return;
        int now = event.getServer().getTickCount();
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(task -> {
            if (task.dueTick() > now) return false;
            due.add(task);
            return true;
        });
        due.forEach(task -> task.action().run());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
    }
}
