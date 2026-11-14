package net.bandit.darkdoppelganger.registry;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, DarkDoppelgangerMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_FIGHT_MUSIC = SOUND_EVENTS.register("boss_fight",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "boss_fight")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_STUN = SOUND_EVENTS.register("boss_stun",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "boss_stun")));

    // Boss voice lines. One event per line so every player hears the same line and the subtitle shows its words.
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_INTRO = voice("intro");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_PREDICTABLE = voice("predictable");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_KNOW_THAT_MOVE = voice("know_that_move");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_STILL_MAKING_THAT_MISTAKE = voice("still_making_that_mistake");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_TOO_SLOW = voice("too_slow");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_YOUR_BEST = voice("your_best");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_ALREADY_WON = voice("already_won");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_HOW_THIS_ENDS = voice("how_this_ends");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_EMBARRASSING_US = voice("embarrassing_us");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_SHADOW = voice("shadow");

    // Intro ambience, made from vanilla sounds referenced in sounds.json.
    public static final DeferredHolder<SoundEvent, SoundEvent> CUTSCENE_WIND = event("cutscene.wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUTSCENE_WIND_BED = event("cutscene.wind_bed");
    public static final DeferredHolder<SoundEvent, SoundEvent> CUTSCENE_DRONE = event("cutscene.drone");

    private static DeferredHolder<SoundEvent, SoundEvent> event(String id) {
        return SOUND_EVENTS.register(id,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, id)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> voice(String line) {
        String id = "voice." + line;
        return SOUND_EVENTS.register(id,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, id)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
