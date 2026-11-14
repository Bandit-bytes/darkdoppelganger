package net.bandit.darkdoppelganger.entity.model;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.bandit.darkdoppelganger.DarkDoppelgangerMod;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;

public class DarkDoppelgangerEntityModel extends AbstractSpellCastingMobModel {
    public static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "animations/doppel_casting_animations.json");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(DarkDoppelgangerMod.MOD_ID, "textures/entity/dark_doppelganger.png");

    /** Its own skin until it mirrors its summoner, then theirs. Its own skin also when theirs is not known here. */
    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        if (object instanceof DarkDoppelgangerEntity boss && boss.isMirrored()) {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            PlayerInfo summoner = boss.getSkinOwner().map(uuid -> connection == null ? null : connection.getPlayerInfo(uuid)).orElse(null);
            if (summoner != null) return summoner.getSkinLocation();
        }
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractSpellCastingMob animatable) {
        return ANIMATIONS;
    }
}
