package net.bandit.darkdoppelganger.entity.renderer;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.bandit.darkdoppelganger.entity.model.DarkDoppelgangerEntityModel;
import net.bandit.darkdoppelganger.entity.model.DarkDoppelgangerMinionEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class DarkDoppelgangerMinionRenderer extends AbstractSpellCastingMobRenderer {

    public DarkDoppelgangerMinionRenderer(EntityRendererProvider.Context context) {
        super(context, new DarkDoppelgangerMinionEntityModel());

    }
}
