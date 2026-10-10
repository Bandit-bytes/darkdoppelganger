package net.bandit.darkdoppelganger.entity.renderer;

import net.bandit.darkdoppelganger.entity.model.DarkDoppelgangerMinionEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class DarkDoppelgangerMinionRenderer extends AzureCompatibleDoppelgangerRenderer {
    public DarkDoppelgangerMinionRenderer(EntityRendererProvider.Context context) {
        super(context, new DarkDoppelgangerMinionEntityModel());
    }
}
