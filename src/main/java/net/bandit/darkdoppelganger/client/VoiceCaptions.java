package net.bandit.darkdoppelganger.client;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Shows the boss's voice lines as captions and tells the boss music when to duck under them.
 * Lines are caught as this client plays them, so only players who can hear a line see it.
 */
@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, value = Dist.CLIENT)
public final class VoiceCaptions {
    private static final String PREFIX = "voice.";
    /** Every line opens with a reverb swell this long before its first word. */
    private static final int WORDS_START = 9;
    /** When each line's last word ends, in ticks from the start of its sound. */
    private static final Map<String, Integer> WORDS_END = Map.of(
            "intro", 89,
            "predictable", 46,
            "know_that_move", 42,
            "still_making_that_mistake", 47,
            "too_slow", 63,
            "your_best", 40,
            "already_won", 56,
            "how_this_ends", 60,
            "embarrassing_us", 63,
            "shadow", 84);
    /** The caption stays up this long after the last word. */
    private static final int LINGER = 12;
    private static final int FADE_TICKS = 4;
    private static final int TEXT_COLOR = 0xE9DDFF;
    /** Gameplay captions sit this far above the bottom of the screen, clear of the hotbar and action bar. */
    private static final int HUD_CLEARANCE = 84;

    @Nullable
    private static Component caption;
    private static int age;
    private static int wordsEnd;

    private VoiceCaptions() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;
        ResourceLocation id = sound.getLocation();
        if (!DarkDoppelgangerMod.MOD_ID.equals(id.getNamespace()) || !id.getPath().startsWith(PREFIX)) return;
        String line = id.getPath().substring(PREFIX.length());
        Integer end = WORDS_END.get(line);
        if (end == null) return;
        caption = Component.translatable("subtitles.darkdoppelganger.voice." + line);
        age = 0;
        wordsEnd = end;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (caption == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            caption = null;
            return;
        }
        if (mc.isPaused()) return;
        if (++age > wordsEnd + LINGER) caption = null;
    }

    /** True from the start of a line until just after its last word. */
    public static boolean speaking() {
        return caption != null && age <= wordsEnd + 4;
    }

    /** During a cutscene the HUD is hidden and the cutscene draws the caption itself, in its letterbox. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (CutsceneClient.isActive()) return;
        render(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false), false);
    }

    static void render(GuiGraphics graphics, float partialTick, boolean inLetterbox) {
        Component line = caption;
        if (line == null) return;
        float time = age + partialTick;
        float alpha = Math.min(fade(time - (WORDS_START - 2)), fade(wordsEnd + LINGER - time));
        if (alpha <= 0.02F) return;

        Font font = Minecraft.getInstance().font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        List<FormattedCharSequence> rows = font.split(line, Math.max(80, width - 40));
        int rowHeight = font.lineHeight + 3;
        int blockHeight = rows.size() * rowHeight;
        // In the cutscene, centred in the bottom letterbox bar.
        int top = inLetterbox
                ? height - Math.round(height * 0.055F) - blockHeight / 2
                : height - HUD_CLEARANCE - blockHeight;
        int textAlpha = Math.max(4, Math.round(alpha * 255));
        int boxAlpha = Math.round(alpha * 140);
        for (int i = 0; i < rows.size(); i++) {
            FormattedCharSequence row = rows.get(i);
            int rowWidth = font.width(row);
            int x = (width - rowWidth) / 2;
            int y = top + i * rowHeight;
            graphics.fill(x - 4, y - 2, x + rowWidth + 4, y + font.lineHeight + 1, boxAlpha << 24);
            graphics.drawString(font, row, x, y, (textAlpha << 24) | TEXT_COLOR, true);
        }
    }

    private static float fade(float ticks) {
        return Mth.clamp(ticks / FADE_TICKS, 0.0F, 1.0F);
    }
}
