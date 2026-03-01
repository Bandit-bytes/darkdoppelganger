package net.bandit.darkdoppelganger.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;

@Mod.EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSounds {


    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DarkDoppelgangerMod.MOD_ID);

    public static final RegistryObject<SoundEvent> BOSS_FIGHT_MUSIC = SOUND_EVENTS.register("boss_fight",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(DarkDoppelgangerMod.MOD_ID, "boss_fight")));

    public static final RegistryObject<SoundEvent> BOSS_STUN = SOUND_EVENTS.register("boss_stun",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(DarkDoppelgangerMod.MOD_ID, "boss_stun")));

    public static final RegistryObject<SoundEvent> BOSS_LAUGH= SOUND_EVENTS.register("boss_laugh",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(DarkDoppelgangerMod.MOD_ID, "boss_laugh")));

    public static final RegistryObject<SoundEvent> BOSS_ROAR = SOUND_EVENTS.register("boss_roar",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(DarkDoppelgangerMod.MOD_ID, "boss_roar")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
