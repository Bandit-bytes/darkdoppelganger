package net.bandit.darkdoppelganger.entity.renderer;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerMinionEntity;
import net.bandit.darkdoppelganger.entity.model.DarkDoppelgangerMinionEntityModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class DarkDoppelgangerMinionRenderer extends AbstractSpellCastingMobRenderer {

    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    DarkDoppelgangerMod.MOD_ID,
                    "textures/entity/dark_doppelganger_minion.png"
            );

    public DarkDoppelgangerMinionRenderer(EntityRendererProvider.Context context) {
        super(context, new DarkDoppelgangerMinionEntityModel());
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSpellCastingMob entity) {
        if (entity instanceof DarkDoppelgangerMinionEntity minion && minion.usesSummonerSkin()) {
            UUID uuid = minion.getSkinPlayerUUID();
            if (uuid != null) {
                Minecraft mc = Minecraft.getInstance();

                if (mc.getConnection() != null) {
                    PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
                    if (info != null) {
                        return info.getSkinLocation();
                    }
                }

                if (mc.level != null) {
                    Player levelPlayer = mc.level.getPlayerByUUID(uuid);
                    if (levelPlayer instanceof AbstractClientPlayer clientPlayer) {
                        return clientPlayer.getSkinTextureLocation();
                    }
                }

                return DefaultPlayerSkin.getDefaultSkin(uuid);
            }
        }

        return DEFAULT_TEXTURE;
    }
}