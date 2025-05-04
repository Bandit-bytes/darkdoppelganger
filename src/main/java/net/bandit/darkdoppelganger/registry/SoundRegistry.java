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

    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_LAUGH= SOUND_EVENTS.register("boss_laugh",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "boss_laugh")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR = SOUND_EVENTS.register("boss_roar",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "boss_roar")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
