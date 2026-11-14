package net.bandit.darkdoppelganger.registry;

import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DarkDoppelgangerMod.MOD_ID);

    public static final RegistryObject<SoundEvent> BOSS_FIGHT_MUSIC = SOUND_EVENTS.register("boss_fight",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "boss_fight")));

    public static final RegistryObject<SoundEvent> BOSS_STUN = SOUND_EVENTS.register("boss_stun",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "boss_stun")));

    // Boss voice lines. One event per line so every player hears the same line and the subtitle shows its words.
    public static final RegistryObject<SoundEvent> VOICE_INTRO = voice("intro");
    public static final RegistryObject<SoundEvent> VOICE_PREDICTABLE = voice("predictable");
    public static final RegistryObject<SoundEvent> VOICE_KNOW_THAT_MOVE = voice("know_that_move");
    public static final RegistryObject<SoundEvent> VOICE_STILL_MAKING_THAT_MISTAKE = voice("still_making_that_mistake");
    public static final RegistryObject<SoundEvent> VOICE_TOO_SLOW = voice("too_slow");
    public static final RegistryObject<SoundEvent> VOICE_YOUR_BEST = voice("your_best");
    public static final RegistryObject<SoundEvent> VOICE_ALREADY_WON = voice("already_won");
    public static final RegistryObject<SoundEvent> VOICE_HOW_THIS_ENDS = voice("how_this_ends");
    public static final RegistryObject<SoundEvent> VOICE_EMBARRASSING_US = voice("embarrassing_us");
    public static final RegistryObject<SoundEvent> VOICE_SHADOW = voice("shadow");

    // Intro ambience, made from vanilla sounds referenced in sounds.json.
    public static final RegistryObject<SoundEvent> CUTSCENE_WIND = event("cutscene.wind");
    public static final RegistryObject<SoundEvent> CUTSCENE_WIND_BED = event("cutscene.wind_bed");
    public static final RegistryObject<SoundEvent> CUTSCENE_DRONE = event("cutscene.drone");

    private static RegistryObject<SoundEvent> event(String id) {
        return SOUND_EVENTS.register(id,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, id)));
    }

    private static RegistryObject<SoundEvent> voice(String line) {
        String id = "voice." + line;
        return SOUND_EVENTS.register(id,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, id)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
