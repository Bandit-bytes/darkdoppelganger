package net.bandit.darkdoppelganger.client;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.cutscene.CutsceneTracker;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.network.CutscenePayloads;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
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
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Plays the intro on this client, built around the player's own position:
 * <ol>
 *     <li>From the player's eyes, the view aims at the portal and zooms in as it opens and the boss rises.</li>
 *     <li>The zoom eases out while the camera swings out to the player's right, to a side-on shot of the
 *     player and the boss facing each other: the player on the left, the boss on the right.</li>
 *     <li>After a short hold, the camera swings back into the player's eyes and control returns.</li>
 * </ol>
 * The server decides when it starts and what the boss does. While it plays there are letterbox
 * bars, no HUD and no input, and the player is held in place.
 */
@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, value = Dist.CLIENT)
public final class CutsceneClient {
    private static final int LETTERBOX_TICKS = 30;
    /** A slow push in from the start, still moving while the portal opens. */
    private static final int ZOOM_IN_TICKS = 45;
    /** Once the boss has risen the shot holds on it, then swings out to the side, holds, and swings home. */
    private static final int HOLD_TICKS = 20;
    private static final int PULL_TICKS = 55;
    private static final int SETTLE_TICKS = 15;
    private static final int RISE_TICKS = DarkDoppelgangerEntity.SPAWN_ANIMATION_TICKS;

    /** Height of the view at the boss when zoomed in, in blocks, so every player gets the same framing. */
    private static final double FRAME_HEIGHT = 6.0;
    private static final double MIN_FOV = 10.0;
    /** Field of view for the side shot. Never wider than the player's own. */
    private static final double SIDE_FOV = 60.0;
    /** The side shot stands off this far from the midpoint, as a share of the player's distance to the boss, so both fit. */
    private static final double SIDE_DISTANCE = 0.78;
    private static final double MIN_SIDE_DISTANCE = 4.0;
    private static final double MAX_SIDE_DISTANCE = 16.0;
    /** Below eye level, so the side shot looks up at the two of them against the sky. */
    private static final double SIDE_UP = -1.0;
    /** The side shot looks at the middle of the two, at chest height. */
    private static final double CHEST = 1.1;
    /** The camera leaves and re-enters the eyes from the right, so the player's head never fills the frame. */
    private static final double SWING_IN = 1.5;
    /** Where to look, in blocks above the boss's feet: the middle of the portal, which is also the boss's chest once risen. */
    private static final double LOOK_HEIGHT = CutsceneTracker.LOOK_HEIGHT;
    /** The player's own model is hidden while the camera is this close to their eyes, inside their head. */
    private static final double HIDE_SELF_DISTANCE = 0.7;

    @Nullable
    private static Active active;

    private CutsceneClient() {
    }

    private static final class Active {
        final int duration;
        final int emerge;
        final int pullStart;
        final int slideStart;
        final CameraType previousCameraType;
        final Marker camera;
        final Vec3 feet;
        final Vec3 eyes;
        final Vec3 right;
        final Vec3 side;
        final Vec3 swingOut;
        final Vec3 swingIn;
        final Vec3 middle;
        final double zoomFov;
        int tick = 0;
        /** Where the player was at the start of this tick, to put them back at the end of it. */
        @Nullable
        Vec3 heldPosition;
        float heldFallDistance;

        Active(CutscenePayloads.Start start, Minecraft mc, LocalPlayer player) {
            duration = start.duration();
            emerge = start.emerge();
            pullStart = emerge + RISE_TICKS + HOLD_TICKS;
            slideStart = pullStart + PULL_TICKS + SETTLE_TICKS;
            previousCameraType = mc.options.getCameraType();
            camera = new Marker(EntityType.MARKER, mc.level);

            feet = new Vec3(start.x(), start.y(), start.z());
            eyes = player.getEyePosition();
            Vec3 forward = new Vec3(feet.x - eyes.x, 0.0, feet.z - eyes.z);
            forward = forward.lengthSqr() < 0.01 ? Vec3.directionFromRotation(0.0F, player.getYRot()) : forward.normalize();
            right = new Vec3(-forward.z, 0.0, forward.x);

            double apart = Math.sqrt(Math.pow(feet.x - eyes.x, 2) + Math.pow(feet.z - eyes.z, 2));
            double standOff = Mth.clamp(apart * SIDE_DISTANCE, MIN_SIDE_DISTANCE, MAX_SIDE_DISTANCE);
            Vec3 playerChest = player.position().add(0.0, CHEST, 0.0);
            middle = playerChest.add(feet.add(0.0, CHEST, 0.0)).scale(0.5);
            side = new Vec3(middle.x, eyes.y + SIDE_UP, middle.z).add(right.scale(standOff));
            swingOut = eyes.add(right.scale(standOff * 0.6)).add(forward.scale(apart * 0.15));
            swingIn = eyes.add(right.scale(SWING_IN));
            double distance = feet.add(0.0, LOOK_HEIGHT, 0.0).distanceTo(eyes);
            zoomFov = Math.max(MIN_FOV, Math.toDegrees(2.0 * Math.atan(FRAME_HEIGHT / 2.0 / Math.max(distance, 0.1))));
        }

        double progress(double time, double from, double length) {
            return smooth((time - from) / length);
        }

        Vec3 position(double time) {
            if (time < pullStart) return eyes;
            if (time < slideStart - SETTLE_TICKS) return bezier(eyes, swingOut, side, progress(time, pullStart, PULL_TICKS));
            if (time < slideStart) return side;
            return bezier(side, swingIn, eyes, progress(time, slideStart, duration - slideStart));
        }

        /** 0 while looking at the boss, 1 while looking at the middle of the two for the side shot. */
        double sideShare(double time) {
            return progress(time, pullStart, PULL_TICKS) - progress(time, slideStart, duration - slideStart);
        }

        /** Fixed on the portal, so the zoomed shot never drifts; it only moves for the side shot. */
        Vec3 lookTarget(double time) {
            return feet.add(0.0, LOOK_HEIGHT, 0.0).lerp(middle, sideShare(time));
        }

        /** Field of view, given the player's own. */
        double fov(double time, double own) {
            double zoomed = Math.min(own, zoomFov);
            double sideFov = Math.min(own, SIDE_FOV);
            if (time < pullStart) return logLerp(own, zoomed, progress(time, 0, ZOOM_IN_TICKS));
            if (time < slideStart) return logLerp(zoomed, sideFov, progress(time, pullStart, PULL_TICKS));
            return logLerp(sideFov, own, progress(time, slideStart, duration - slideStart));
        }

        boolean locked() {
            return tick < duration;
        }
    }

    public static boolean isActive() {
        return active != null;
    }

    public static void start(CutscenePayloads.Start payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        stop();
        Active cutscene = new Active(payload, mc, mc.player);
        active = cutscene;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        placeCamera(cutscene, true);
        mc.setCameraEntity(cutscene.camera);
        holdEyeHeight(0.0F);
    }

    /** Ends the cutscene at once and gives the view back to the player. */
    public static void stop() {
        Active cutscene = active;
        if (cutscene == null) return;
        active = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            holdEyeHeight(mc.player.getEyeHeight());
        }
        mc.options.setCameraType(cutscene.previousCameraType);
    }

    // ------------------------------------------------------------------ path

    /** Smootherstep: starts and stops with no jolt. */
    private static double smooth(double x) {
        x = Mth.clamp(x, 0.0, 1.0);
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    private static Vec3 bezier(Vec3 from, Vec3 control, Vec3 to, double s) {
        double a = (1.0 - s) * (1.0 - s);
        double b = 2.0 * (1.0 - s) * s;
        double c = s * s;
        return from.scale(a).add(control.scale(b)).add(to.scale(c));
    }

    /** Interpolates the magnification, not the angle, so a zoom moves at an even pace. */
    private static double logLerp(double fromFov, double toFov, double s) {
        double from = Math.log(Math.tan(Math.toRadians(fromFov) / 2.0));
        double to = Math.log(Math.tan(Math.toRadians(toFov) / 2.0));
        return Math.toDegrees(2.0 * Math.atan(Math.exp(Mth.lerp(s, from, to))));
    }

    private static float[] aim(Vec3 from, Vec3 target) {
        Vec3 look = target.subtract(from);
        float yaw = (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);
        return new float[]{yaw, pitch};
    }

    /** Places the camera for this tick. The renderer smooths between ticks like any moving entity. */
    private static void placeCamera(Active c, boolean first) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 target = c.lookTarget(c.tick);
        Vec3 position = clip(mc, c.camera, target, c.position(c.tick));
        float[] aim = aim(position, target);
        Marker camera = c.camera;
        float yaw = first ? aim[0] : camera.getYRot() + Mth.wrapDegrees(aim[0] - camera.getYRot());
        camera.setOldPosAndRot();
        camera.setPos(position);
        camera.setYRot(yaw);
        camera.setXRot(aim[1]);
        if (first) camera.setOldPosAndRot();
    }

    /** Keeps the camera on the near side of walls between it and what it looks at. */
    private static Vec3 clip(Minecraft mc, Marker camera, Vec3 target, Vec3 desired) {
        if (mc.level == null) return desired;
        HitResult hit = mc.level.clip(new ClipContext(target, desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, camera));
        if (hit.getType() == HitResult.Type.MISS) return desired;
        return hit.getLocation().add(target.subtract(desired).normalize().scale(0.3));
    }

    /** Hands the view back facing where the camera last looked, so the switch does not jump. */
    private static void faceTarget(LocalPlayer player, Vec3 target) {
        float[] aim = aim(player.getEyePosition(), target);
        float yaw = player.getYRot() + Mth.wrapDegrees(aim[0] - player.getYRot());
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(aim[1]);
        player.yRotO = yaw;
        player.xRotO = aim[1];
    }

    // ------------------------------------------------------------------ ticking and input

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Active c = active;
        if (c == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || c.camera.level() != mc.level) {
            stop();
            return;
        }
        // Drop these presses before vanilla handles them. Pause still works.
        for (KeyMapping key : mc.options.keyHotbarSlots) consumeClicks(key);
        consumeClicks(mc.options.keyDrop);
        consumeClicks(mc.options.keySwapOffhand);
        consumeClicks(mc.options.keyTogglePerspective);
        consumeClicks(mc.options.keyInventory);
        consumeClicks(mc.options.keyChat);
        consumeClicks(mc.options.keyCommand);
        consumeClicks(mc.options.keyAdvancements);
        consumeClicks(mc.options.keySocialInteractions);

        // The server and the boss stop while singleplayer is paused, so the cutscene does too.
        if (mc.isPaused()) return;
        if (++c.tick >= c.duration) {
            faceTarget(mc.player, c.lookTarget(c.duration));
            stop();
            // Raise the held items into view, as after switching items, rather than popping them in.
            mc.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.MAIN_HAND);
            mc.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.OFF_HAND);
            return;
        }
        if (mc.getCameraEntity() != c.camera) mc.setCameraEntity(c.camera);
        placeCamera(c, false);
    }

    private static void consumeClicks(KeyMapping key) {
        while (key.consumeClick()) { }
    }

    /**
     * With the cutscene camera as the camera entity, vanilla stops reading input into the player
     * and stops sending their position, but keeps moving them with whatever motion they had.
     * Stop that motion, and put them back where they were, so they are still where the server last
     * saw them when control returns. A passenger is left to their vehicle, which only gets no input.
     */
    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        Active c = active;
        if (c == null || event.getEntity() != Minecraft.getInstance().player) return;
        Player player = event.getEntity();
        c.heldPosition = player.position();
        c.heldFallDistance = player.fallDistance;
        stopMoving(player);
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        Active c = active;
        if (c == null || c.heldPosition == null || event.getEntity() != Minecraft.getInstance().player) return;
        Player player = event.getEntity();
        stopMoving(player);
        if (!player.isPassenger()) {
            player.setPos(c.heldPosition);
            player.fallDistance = c.heldFallDistance;
        }
    }

    private static void stopMoving(Player player) {
        player.xxa = 0.0F;
        player.zza = 0.0F;
        player.setJumping(false);
        player.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * The camera only learns about a new entity when it next renders, so the tick in between eases its
     * eye height toward the old entity's. Setting it again after that tick keeps the view from dipping.
     */
    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        if (Float.isNaN(heldEyeHeight)) return;
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        setCameraEyeHeight(camera, heldEyeHeight);
        if (camera.getEntity() == mc.getCameraEntity()) heldEyeHeight = Float.NaN;
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Active c = active;
        if (c == null || !event.usedConfiguredFov()) return; // leave the held-item view alone
        event.setFOV(c.fov(c.tick + event.getPartialTick(), event.getFOV()));
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (active == null) return;
        var input = event.getInput();
        input.up = input.down = input.left = input.right = false;
        input.jumping = input.shiftKeyDown = false;
        input.forwardImpulse = input.leftImpulse = 0.0F;
    }

    @SubscribeEvent
    public static void onTurn(CalculatePlayerTurnEvent event) {
        if (active == null) return;
        // Sensitivity -1/3 makes vanilla's turn factor zero, so the mouse does not turn the player during the shot.
        event.setMouseSensitivity(-1.0 / 3.0);
        event.setCinematicCameraEnabled(false);
    }

    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (active == null) return;
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (active != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (active != null) event.setCanceled(true);
    }

    /** The camera starts and ends inside the player's head; hide the model there. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (active == null || event.getEntity() != mc.player) return;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        if (camera.distanceTo(mc.player.getEyePosition(event.getPartialTick())) < HIDE_SELF_DISTANCE) event.setCanceled(true);
    }

    /** No name tags in the shot, including the player's own, which the separate camera would otherwise show. */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (active != null) event.setCanRender(TriState.FALSE);
    }

    /** Hide the HUD, but keep the vignette, or it snaps back on when the cutscene ends. */
    @SubscribeEvent
    public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (active != null && !event.getName().equals(VanillaGuiLayers.CAMERA_OVERLAYS)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Active c = active;
        if (c == null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double time = c.tick + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double slide = smooth(Math.min(time, c.duration - time) / LETTERBOX_TICKS);
        int bar = Math.round(height * 0.11F * (float) slide);
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

    // ------------------------------------------------------------------ camera eye height

    private static Field eyeHeightField;
    private static Field eyeHeightOldField;
    private static boolean eyeFieldsMissing;
    /** Eye height to keep the camera at until it has switched entity, or NaN. */
    private static float heldEyeHeight = Float.NaN;

    private static void holdEyeHeight(float height) {
        heldEyeHeight = height;
        setCameraEyeHeight(Minecraft.getInstance().gameRenderer.getMainCamera(), height);
    }

    /**
     * The camera eases its eye height toward the camera entity's over several ticks, which would
     * make the view sink when switching to the cutscene camera (eye height 0) and rise on return.
     * Setting both values removes that drift.
     */
    private static void setCameraEyeHeight(Camera camera, float height) {
        if (eyeFieldsMissing) return;
        try {
            if (eyeHeightField == null) {
                eyeHeightField = Camera.class.getDeclaredField("eyeHeight");
                eyeHeightOldField = Camera.class.getDeclaredField("eyeHeightOld");
                eyeHeightField.setAccessible(true);
                eyeHeightOldField.setAccessible(true);
            }
            eyeHeightField.setFloat(camera, height);
            eyeHeightOldField.setFloat(camera, height);
        } catch (ReflectiveOperationException | RuntimeException e) {
            eyeFieldsMissing = true; // only cosmetic: the view eases into place instead
        }
    }
}
