package net.bandit.darkdoppelganger.entity.renderer;

import net.bandit.darkdoppelganger.entity.model.DarkDoppelgangerEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class DarkDoppelgangerRenderer extends AzureCompatibleDoppelgangerRenderer {
    public DarkDoppelgangerRenderer(EntityRendererProvider.Context context) {
        super(context, new DarkDoppelgangerEntityModel());
    }
}
