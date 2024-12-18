package net.bandit.darkdoppelganger.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

public class DoppelgangerRingItem extends Item implements ICurioItem {

    private static final UUID HEALTH_BOOST_UUID = UUID.fromString("1d1a82d8-c9d2-11ed-afa1-0242ac120002");
    private static final UUID MAX_MANA_UUID = UUID.fromString("2d2a92d8-c9d2-11ed-afa1-0242ac120003");

    public DoppelgangerRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        modifiers.put(Attributes.MAX_HEALTH,
                new AttributeModifier(HEALTH_BOOST_UUID, "Ring health boost", 20.0, AttributeModifier.Operation.ADDITION));
        //Irons addon
        modifiers.put(AttributeRegistry.MAX_MANA.get(),
            new AttributeModifier(MAX_MANA_UUID, "Ring mana boost", 100.0, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "ring".equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        Level level = slotContext.entity().level();
        if (!level.isClientSide) {
//            System.out.println("Doppelganger Ring equipped!");
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        Level level = slotContext.entity().level();
        if (!level.isClientSide) {
//            System.out.println("Doppelganger Ring unequipped!");
        }
    }

    @NotNull
    public ICurio.@NotNull SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_CHAIN, 1.0F, 1.0F);
    }

}
