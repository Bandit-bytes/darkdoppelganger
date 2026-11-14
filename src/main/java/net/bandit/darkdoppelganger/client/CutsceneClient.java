package net.bandit.darkdoppelganger.client;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.cutscene.CutsceneTracker;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.network.CutscenePayloads;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
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
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.Comparator;

/**
 * Plays the intro on this client as four slow shots joined by hard cuts, paced like a duel standoff:
 * <ol>
 *     <li>Over the player's right shoulder: the portal opens, the boss rises and stands silent.</li>
 *     <li>A medium close-up of the boss, which speaks its line, then holds in silence.</li>
 *     <li>The same framing on the player, silent.</li>
 *     <li>A wide, low, side-on shot of the two facing each other, small against the sky.</li>
 * </ol>
 * No shot zooms. Each drifts a little at a steady pace, so it is already moving when it cuts in.
 * Every shot is taken from the player's right of the line between the two, so they face each other
 * across the cuts. Ash drifts through the air while it plays. Control returns with a cut back to the
 * player's eyes. The server decides when it starts and what the boss does. While it plays there are letterbox bars, no HUD (as with
 * F1) and no input, and the player is held in place.
 */
@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, value = Dist.CLIENT)
public final class CutsceneClient {
    private static final int LETTERBOX_TICKS = 30;
    private static final int BOSS_SHOT = DarkDoppelgangerEntity.INTRO_BOSS_SHOT;
    private static final int PLAYER_SHOT = DarkDoppelgangerEntity.INTRO_PLAYER_SHOT;
    private static final int BOTH_SHOT = DarkDoppelgangerEntity.INTRO_BOTH_SHOT;

    // Shot 1: behind and to the right of the player's head, easing in a little.
    private static final double SHOULDER_BACK_FROM = 2.8;
    private static final double SHOULDER_BACK_TO = 2.5;
    private static final double SHOULDER_SIDE = 0.95;
    private static final double SHOULDER_UP = 0.3;
    private static final double SHOULDER_FOV = 40.0;
    // Shots 2 and 3: this far off the line between the two, easing in a little, looking up slightly.
    private static final double FACE_ANGLE = Math.toRadians(25.0);
    private static final double FACE_FOV = 32.0;
    /** The camera sits this far below the point it aims at. */
    private static final double FACE_LOW = 0.1;
    /** The close-ups aim this far past the face, into the space it looks at, so the face sits off centre. */
    private static final double FACE_LEAD = 0.35;
    /** The boss's model is drawn taller than its hitbox's eye height. Its face is about this high above its feet. */
    private static final double BOSS_FACE = 1.7;
    private static final double BOSS_FROM = 2.5;
    private static final double BOSS_TO = 2.2;
    /** The player's close-up aims this far below their eyes. */
    private static final double PLAYER_AIM_DROP = 0.15;
    private static final double PLAYER_FROM = 2.7;
    private static final double PLAYER_TO = 2.4;
    // Shot 4: low and side-on. Each of the two stands this share of the half-width from the centre.
    private static final double WIDE_FOV = 40.0;
    private static final double WIDE_SPREAD = 0.55;
    private static final double MIN_WIDE_DISTANCE = 6.0;
    private static final double MAX_WIDE_DISTANCE = 24.0;
    /** Share of its distance the wide shot closes over its length. */
    private static final double WIDE_PUSH = 0.07;
    private static final double WIDE_UP = 0.5;
    private static final double WIDE_LOOK_UP = 1.5;

    private static final int ASH_PER_TICK = 14;
    /** High enough to reach the top of the wide shot's frame behind the two. */
    private static final double ASH_HEIGHT = 13.0;
    private static final double ASH_CLEARANCE = 3.0;
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
        final CameraType previousCameraType;
        final boolean previousHideGui;
        final Marker camera;
        final Vec3 feet;
        final Vec3 eyes;
        /** Level, from the player toward the boss. */
        final Vec3 forward;
        /** Level, to the player's right: the side every shot is taken from. */
        final Vec3 right;
        final double apart;
        /** Between the two, at ground height. */
        final Vec3 middle;
        final double shoulderFov;
        final double faceFov;
        final double wideFov;
        final double wideDistance;
        int tick = 0;
        /** The shot the camera was last placed for. A new shot snaps the camera instead of gliding to it. */
        int shot = -1;
        @Nullable
        DarkDoppelgangerEntity boss;
        /** Where the player was at the start of this tick, to put them back at the end of it. */
        @Nullable
        Vec3 heldPosition;
        float heldFallDistance;

        Active(CutscenePayloads.Start start, Minecraft mc, LocalPlayer player) {
            duration = start.duration();
            previousCameraType = mc.options.getCameraType();
            previousHideGui = mc.options.hideGui;
            camera = new Marker(EntityType.MARKER, mc.level);

            feet = new Vec3(start.x(), start.y(), start.z());
            eyes = player.getEyePosition();
            Vec3 flat = new Vec3(feet.x - eyes.x, 0.0, feet.z - eyes.z);
            forward = flat.lengthSqr() < 0.01 ? Vec3.directionFromRotation(0.0F, player.getYRot()) : flat.normalize();
            right = new Vec3(-forward.z, 0.0, forward.x);
            apart = flat.length();
            middle = new Vec3((feet.x + player.getX()) / 2.0, (feet.y + player.getY()) / 2.0, (feet.z + player.getZ()) / 2.0);

            double own = mc.options.fov().get();
            shoulderFov = Math.min(own, SHOULDER_FOV);
            faceFov = Math.min(own, FACE_FOV);
            wideFov = Math.min(own, WIDE_FOV);
            double aspect = (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
            double halfWidth = Math.tan(Math.toRadians(wideFov) / 2.0) * aspect;
            wideDistance = Mth.clamp(apart / (2.0 * WIDE_SPREAD * halfWidth), MIN_WIDE_DISTANCE, MAX_WIDE_DISTANCE);
        }

        int shotAt(double time) {
            if (time < BOSS_SHOT) return 0;
            if (time < PLAYER_SHOT) return 1;
            if (time < BOTH_SHOT) return 2;
            return 3;
        }

        Vec3 bossFace() {
            Vec3 base = boss != null && boss.isAlive() ? boss.position() : feet;
            return base.add(0.0, BOSS_FACE, 0.0);
        }

        Vec3 playerAim() {
            return eyes.subtract(0.0, PLAYER_AIM_DROP, 0.0);
        }

        Vec3 position(double time) {
            return switch (shotAt(time)) {
                case 0 -> eyes.add(forward.scale(-Mth.lerp(drift(time, 0, BOSS_SHOT), SHOULDER_BACK_FROM, SHOULDER_BACK_TO)))
                        .add(right.scale(SHOULDER_SIDE)).add(0.0, SHOULDER_UP, 0.0);
                case 1 -> closeUp(bossFace(), forward.scale(-1.0), Mth.lerp(drift(time, BOSS_SHOT, PLAYER_SHOT - BOSS_SHOT), BOSS_FROM, BOSS_TO));
                case 2 -> closeUp(playerAim(), forward, Mth.lerp(drift(time, PLAYER_SHOT, BOTH_SHOT - PLAYER_SHOT), PLAYER_FROM, PLAYER_TO));
                default -> middle.add(0.0, WIDE_UP, 0.0)
                        .add(right.scale(wideDistance * (1.0 - WIDE_PUSH * drift(time, BOTH_SHOT, duration - BOTH_SHOT))));
            };
        }

        /** In front of a face, turned off the line between the two toward the player's right. */
        Vec3 closeUp(Vec3 face, Vec3 facing, double distance) {
            return face.add(facing.scale(distance * Math.cos(FACE_ANGLE)))
                    .add(right.scale(distance * Math.sin(FACE_ANGLE)))
                    .subtract(0.0, FACE_LOW, 0.0);
        }

        /** Past the face, toward the side of the frame it looks at, so it has room to look into. */
        Vec3 leadOf(Vec3 face, Vec3 facing) {
            return face.add(facing.scale(Math.sin(FACE_ANGLE)).subtract(right.scale(Math.cos(FACE_ANGLE))).scale(FACE_LEAD));
        }

        /** Fixed within each shot, so the framing never swings. */
        Vec3 lookTarget(double time) {
            return switch (shotAt(time)) {
                case 0 -> feet.add(0.0, LOOK_HEIGHT, 0.0);
                case 1 -> leadOf(bossFace(), forward.scale(-1.0));
                case 2 -> leadOf(playerAim(), forward);
                default -> middle.add(0.0, WIDE_LOOK_UP, 0.0);
            };
        }

        double fov(double time) {
            return switch (shotAt(time)) {
                case 0 -> shoulderFov;
                case 1, 2 -> faceFov;
                default -> wideFov;
            };
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
        cutscene.boss = findBoss(mc, cutscene.feet);
        // The player's own close-up shows them looking at the boss.
        faceTarget(mc.player, cutscene.bossFace());
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        // As F1 does. Other mods' overlays (Jade's tooltip, for one) check this, not the HUD layers.
        mc.options.hideGui = true;
        placeCamera(cutscene);
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
        mc.options.hideGui = cutscene.previousHideGui;
    }

    @Nullable
    private static DarkDoppelgangerEntity findBoss(Minecraft mc, Vec3 feet) {
        if (mc.level == null) return null;
        return mc.level.getEntitiesOfClass(DarkDoppelgangerEntity.class, new AABB(feet, feet).inflate(2.0, 3.0, 2.0),
                        DarkDoppelgangerEntity::isAlive).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(feet)))
                .orElse(null);
    }

    // ------------------------------------------------------------------ path

    /** Smootherstep: starts and stops with no jolt. */
    private static double smooth(double x) {
        x = Mth.clamp(x, 0.0, 1.0);
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    /** Progress through a shot at a steady pace: a constant drift, with no start or stop. */
    private static double drift(double time, double from, double length) {
        return Mth.clamp((time - from) / length, 0.0, 1.0);
    }

    private static float[] aim(Vec3 from, Vec3 target) {
        Vec3 look = target.subtract(from);
        float yaw = (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);
        return new float[]{yaw, pitch};
    }

    /** Places the camera for this tick. The renderer smooths between ticks within a shot; a new shot is a cut. */
    private static void placeCamera(Active c) {
        Minecraft mc = Minecraft.getInstance();
        int shot = c.shotAt(c.tick);
        boolean cut = shot != c.shot;
        c.shot = shot;
        Vec3 target = c.lookTarget(c.tick);
        Vec3 position = clip(mc, c.camera, target, c.position(c.tick));
        float[] aim = aim(position, target);
        Marker camera = c.camera;
        float yaw = cut ? aim[0] : camera.getYRot() + Mth.wrapDegrees(aim[0] - camera.getYRot());
        camera.setOldPosAndRot();
        camera.setPos(position);
        camera.setYRot(yaw);
        camera.setXRot(aim[1]);
        if (cut) camera.setOldPosAndRot();
    }

    /** Keeps the camera on the near side of walls between it and what it looks at. */
    private static Vec3 clip(Minecraft mc, Marker camera, Vec3 target, Vec3 desired) {
        if (mc.level == null) return desired;
        HitResult hit = mc.level.clip(new ClipContext(target, desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, camera));
        if (hit.getType() == HitResult.Type.MISS) return desired;
        return hit.getLocation().add(target.subtract(desired).normalize().scale(0.3));
    }

    /** Turns the player, head and body, to look at the target. */
    private static void faceTarget(Player player, Vec3 target) {
        float[] aim = aim(player.getEyePosition(), target);
        float yaw = player.getYRot() + Mth.wrapDegrees(aim[0] - player.getYRot());
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.setXRot(aim[1]);
        player.yRotO = yaw;
        player.yHeadRotO = yaw;
        player.yBodyRotO = yaw;
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
        if (c.boss == null || !c.boss.isAlive()) c.boss = findBoss(mc, c.feet);
        if (++c.tick >= c.duration) {
            faceTarget(mc.player, c.bossFace());
            stop();
            // Raise the held items into view, as after switching items, rather than popping them in.
            mc.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.MAIN_HAND);
            mc.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.OFF_HAND);
            return;
        }
        if (mc.getCameraEntity() != c.camera) mc.setCameraEntity(c.camera);
        placeCamera(c);
        driftAsh(mc, c);
    }

    /**
     * Ash hanging in the air around the two, so the still shots are not lifeless. None is spawned
     * close to where any shot's camera stands, so no speck passes right in front of the lens.
     */
    private static void driftAsh(Minecraft mc, Active c) {
        if (mc.level == null) return;
        RandomSource random = mc.level.random;
        Vec3[] lenses = {
                c.position(0), c.position(BOSS_SHOT), c.position(PLAYER_SHOT - 1), c.position(PLAYER_SHOT),
                c.position(BOTH_SHOT - 1), c.position(BOTH_SHOT), c.position(c.duration)};
        for (int i = 0; i < ASH_PER_TICK; i++) {
            Vec3 at = c.middle
                    .add(c.forward.scale((random.nextDouble() - 0.5) * (c.apart + 12.0)))
                    .add(c.right.scale((random.nextDouble() - 0.5) * 16.0))
                    .add(0.0, random.nextDouble() * ASH_HEIGHT, 0.0);
            if (nearAny(at, lenses)) continue;
            mc.level.addParticle(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    private static boolean nearAny(Vec3 point, Vec3[] lenses) {
        for (Vec3 lens : lenses) {
            if (point.distanceToSqr(lens) < ASH_CLEARANCE * ASH_CLEARANCE) return true;
        }
        return false;
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
        // Vanilla's tick turns the model away again, so the player is turned back to face the boss each tick.
        faceTarget(player, c.bossFace());
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
        event.setFOV(c.fov(c.tick + event.getPartialTick()));
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

    /** With a screen open the HUD draws even with hideGui set, so its layers are hidden too. */
    @SubscribeEvent
    public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (active != null) event.setCanceled(true);
    }

    /**
     * The letterbox and the caption, over the finished frame: with hideGui set, the HUD
     * events never fire. Not drawn over an open screen.
     */
    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Post event) {
        Active c = active;
        Minecraft mc = Minecraft.getInstance();
        if (c == null || mc.screen != null) return;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Window window = mc.getWindow();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, window.getGuiScaledWidth(), window.getGuiScaledHeight(),
                0.0F, 1000.0F, 21000.0F), VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.translation(0.0F, 0.0F, -11000.0F);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double time = c.tick + partialTick;
        double slide = smooth(Math.min(time, c.duration - time) / LETTERBOX_TICKS);
        int bar = Math.round(height * 0.11F * (float) slide);
        graphics.fill(0, 0, width, bar, 0xFF000000);
        graphics.fill(0, height - bar, width, height, 0xFF000000);
        VoiceCaptions.render(graphics, partialTick, true);
        graphics.flush();

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
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
