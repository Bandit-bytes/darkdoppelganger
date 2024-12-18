package net.bandit.darkdoppelganger.client.renderer;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.bandit.darkdoppelganger.entity.DarkDopplegangerEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class DarkDoppelgangerRenderer extends AbstractSpellCastingMobRenderer {

    public DarkDoppelgangerRenderer(EntityRendererProvider.Context context) {
        super(context, new DarkDopplegangerEntityModel());

    }
}
