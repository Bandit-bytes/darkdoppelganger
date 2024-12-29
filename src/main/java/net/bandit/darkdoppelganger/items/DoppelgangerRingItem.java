package net.bandit.darkdoppelganger.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;
import java.util.function.Function;

public class DoppelgangerRingItem extends Item implements ICurioItem {

    private static final ResourceLocation HEALTH_BOOST_ID = ResourceLocation.fromNamespaceAndPath("darkdoppelganger", "health_boost");
    private static final ResourceLocation MAX_MANA_ID = ResourceLocation.fromNamespaceAndPath("darkdoppelganger", "max_mana");

    private String attributeSlot = "ring";
    private Function<Integer, Multimap<Holder<Attribute>, AttributeModifier>> attributes;

    public DoppelgangerRingItem(Properties properties) {
        super(properties);
        this.attributes = (index) -> {
            ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Holder.direct(AttributeRegistry.MAX_MANA.get()),
                    new AttributeModifier(MAX_MANA_ID, 100.0, AttributeModifier.Operation.ADD_VALUE));
            builder.put(Holder.direct(AttributeRegistry.SUMMON_DAMAGE.get()),
                    new AttributeModifier(HEALTH_BOOST_ID, 20.0, AttributeModifier.Operation.ADD_VALUE));
            return builder.build();
        };
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals(this.attributeSlot);
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @NotNull
    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        return slotContext.identifier().equals(this.attributeSlot) ? attributes.apply(slotContext.index()) : ICurioItem.super.getAttributeModifiers(slotContext, id, stack);
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
    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.0f, 1.0f);
    }
}
