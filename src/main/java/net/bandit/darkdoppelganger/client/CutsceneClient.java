package net.bandit.darkdoppelganger.client;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.network.CutscenePayloads;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

/**
 * Plays the intro on this client, from the player's own eyes. The view turns to the boss's portal
 * and zooms in, holds while the boss rises, then pulls back slowly. The pull-back runs on after
 * the cutscene ends, so it carries into the fight. While locked there are letterbox bars, no HUD
 * and no input. The server decides when it starts and what the boss does.
 */
@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, value = Dist.CLIENT)
public final class CutsceneClient {
    /** The bars take this long to slide in and to slide out. */
    private static final int LETTERBOX_TICKS = 30;
    /** The turn takes longer the further the player has to turn, so it never whips round. */
    private static final float TURN_DEGREES_PER_TICK = 3.0F;
    private static final int MIN_TURN_TICKS = 25;
    private static final int MAX_TURN_TICKS = 45;
    /** The zoom starts once the turn is under way and settles just after the portal opens. */
    private static final int ZOOM_IN_START = 10;
    private static final int ZOOM_IN_SETTLE_AFTER_EMERGE = 5;
    /** A short hold once the boss has risen, then a pull-back that finishes just after the player has control again. */
    private static final int ZOOM_OUT_DELAY = 10;
    private static final int ZOOM_OUT_TICKS = 55;
    /** Height of the view at the boss when zoomed in, in blocks, so every player gets the same framing. */
    private static final double FRAME_HEIGHT = 6.0;
    private static final double MIN_FOV = 10.0;
    /** Where to look, in blocks above the boss's feet: at the portal while it opens, at the chest once risen. */
    private static final double LOOK_LOW = 0.7;
    private static final double LOOK_HIGH = 1.3;
    private static final int RISE_TICKS = DarkDoppelgangerEntity.SPAWN_ANIMATION_TICKS;

    @Nullable
    private static Active active;

    private CutsceneClient() {
    }

    private static final class Active {
        final Vec3 feet;
        /** Ticks the player is locked in the cutscene. The zoom runs on past this. */
        final int duration;
        final int emerge;
        final int zoomOutStart;
        final int zoomEnd;
        final double zoomFov;
        final CameraType previousCameraType;
        final float startYaw;
        final float startPitch;
        final int turnTicks;
        int tick = 0;

        Active(CutscenePayloads.Start start, LocalPlayer player, CameraType cameraType) {
            feet = new Vec3(start.x(), start.y(), start.z());
            duration = start.duration();
            emerge = start.emerge();
            zoomOutStart = emerge + RISE_TICKS + ZOOM_OUT_DELAY;
            zoomEnd = zoomOutStart + ZOOM_OUT_TICKS;
            previousCameraType = cameraType;
            startYaw = player.getYRot();
            startPitch = player.getXRot();

            double distance = feet.add(0.0, LOOK_HIGH, 0.0).distanceTo(player.getEyePosition());
            zoomFov = Math.max(MIN_FOV, Math.toDegrees(2.0 * Math.atan(FRAME_HEIGHT / 2.0 / Math.max(distance, 0.1))));

            float[] aim = aim(player, lookTarget(0));
            float angle = Math.max(Math.abs(Mth.wrapDegrees(aim[0] - startYaw)), Math.abs(aim[1] - startPitch));
            turnTicks = Mth.clamp(Math.round(angle / TURN_DEGREES_PER_TICK), MIN_TURN_TICKS, MAX_TURN_TICKS);
        }

        boolean locked() {
            return tick < duration;
        }

        /** Tilts up from the portal to the boss's chest as it rises. */
        Vec3 lookTarget(double time) {
            double rise = ease((time - emerge) / RISE_TICKS);
            return feet.add(0.0, Mth.lerp(rise, LOOK_LOW, LOOK_HIGH), 0.0);
        }

        /** 0 is the player's normal view, 1 is fully zoomed in. */
        double zoom(double time) {
            if (time < zoomOutStart) {
                return ease((time - ZOOM_IN_START) / (emerge + ZOOM_IN_SETTLE_AFTER_EMERGE - ZOOM_IN_START));
            }
            return 1.0 - ease((time - zoomOutStart) / ZOOM_OUT_TICKS);
        }
    }

    private static boolean locked() {
        return active != null && active.locked();
    }

    public static void start(CutscenePayloads.Start payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        stop();
        active = new Active(payload, mc.player, mc.options.getCameraType());
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.gui.clear(); // no title left over from an earlier card
    }

    /** Ends the cutscene at once, zoom included. */
    public static void stop() {
        Active cutscene = active;
        if (cutscene == null) return;
        active = null;
        if (cutscene.locked()) Minecraft.getInstance().options.setCameraType(cutscene.previousCameraType);
    }

    private static double ease(double x) {
        x = Mth.clamp(x, 0.0, 1.0);
        return x * x * (3.0 - 2.0 * x);
    }

    /** Yaw and pitch from the player's eyes to the target. */
    private static float[] aim(LocalPlayer player, Vec3 target) {
        Vec3 look = target.subtract(player.getEyePosition());
        float yaw = (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);
        return new float[]{yaw, pitch};
    }

    /** Eases the player's view from where it started to the boss, then keeps it there. */
    private static void turnToward(LocalPlayer player, Active c) {
        float[] aim = aim(player, c.lookTarget(c.tick));
        float progress = (float) ease(c.tick / (double) c.turnTicks);
        float yaw = c.startYaw + Mth.wrapDegrees(aim[0] - c.startYaw) * progress;
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(Mth.lerp(progress, c.startPitch, aim[1]));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Active c = active;
        if (c == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stop();
            return;
        }
        if (c.locked()) {
            // Drop these presses before vanilla handles them: no item drops or perspective changes.
            while (mc.options.keyDrop.consumeClick()) { }
            while (mc.options.keySwapOffhand.consumeClick()) { }
            while (mc.options.keyTogglePerspective.consumeClick()) { }
        }
        c.tick++;
        if (c.tick == c.duration) mc.options.setCameraType(c.previousCameraType);
        if (c.tick >= c.zoomEnd) active = null;
    }

    /** After the player's own tick, so the turn is smoothed between ticks like normal movement. */
    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Active c = active;
        Minecraft mc = Minecraft.getInstance();
        if (c != null && c.locked() && mc.player != null) turnToward(mc.player, c);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Active c = active;
        if (c == null || !event.usedConfiguredFov()) return; // leave the held-item view alone
        double zoom = c.zoom(c.tick + event.getPartialTick());
        double base = event.getFOV();
        double target = Math.min(base, c.zoomFov);
        // Interpolate the magnification, not the angle, so the zoom moves at an even pace throughout.
        double from = Math.log(Math.tan(Math.toRadians(base) / 2.0));
        double to = Math.log(Math.tan(Math.toRadians(target) / 2.0));
        event.setFOV(Math.toDegrees(2.0 * Math.atan(Math.exp(Mth.lerp(zoom, from, to)))));
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!locked()) return;
        var input = event.getInput();
        input.up = input.down = input.left = input.right = false;
        input.jumping = input.shiftKeyDown = false;
        input.forwardImpulse = input.leftImpulse = 0.0F;
    }

    @SubscribeEvent
    public static void onTurn(CalculatePlayerTurnEvent event) {
        if (!locked()) return;
        // Sensitivity -1/3 makes vanilla's turn factor zero, so the mouse does not fight the scripted turn.
        event.setMouseSensitivity(-1.0 / 3.0);
        event.setCinematicCameraEnabled(false);
    }

    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (!locked()) return;
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (locked()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (locked()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (locked()) event.setCanRender(TriState.FALSE);
    }

    @SubscribeEvent
    public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (locked()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Active c = active;
        if (c == null || !c.locked()) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double time = c.tick + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float slide = (float) ease(Math.min(time, c.duration - time) / LETTERBOX_TICKS);
        int bar = Math.round(height * 0.11F * slide);
        graphics.fill(0, 0, width, bar, 0xFF000000);
        graphics.fill(0, height - bar, width, height, 0xFF000000);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
    }

    /** Respawn and dimension changes replace the player entity; the cutscene ends with them. */
    @SubscribeEvent
    public static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        stop();
    }
}
