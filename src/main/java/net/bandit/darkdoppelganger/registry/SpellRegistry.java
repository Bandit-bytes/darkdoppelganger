package net.bandit.darkdoppelganger.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.spells.DoppelPortalSpell;
import net.bandit.darkdoppelganger.spells.SummonDoppelMinionSpell;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SpellRegistry {
    private static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY, DarkDoppelgangerMod.MOD_ID);
    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }

    private static RegistryObject<AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static final RegistryObject<AbstractSpell> DOPPEL_PORTAL = registerSpell(new DoppelPortalSpell());
    public static final RegistryObject<AbstractSpell> MINION_SPELL = registerSpell(new SummonDoppelMinionSpell());
}
