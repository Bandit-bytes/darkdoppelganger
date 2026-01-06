package net.bandit.darkdoppelganger.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.spells.DoppelPortalSpell;
import net.bandit.darkdoppelganger.spells.SummonDoppelMinionSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SpellRegistry {
    private static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY, DarkDoppelgangerMod.MOD_ID);
    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }

    private static DeferredHolder<AbstractSpell, AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static final DeferredHolder<AbstractSpell, AbstractSpell> DOPPEL_PORTAL = registerSpell(new DoppelPortalSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MINION_SPELL = registerSpell(new SummonDoppelMinionSpell());
}
