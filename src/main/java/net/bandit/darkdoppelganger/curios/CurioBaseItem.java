package net.bandit.darkdoppelganger.curios;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.function.Function;

public class CurioBaseItem extends Item implements ICurioItem {

    private String attributeSlot = Curios.RING_SLOT; // Default slot type
    private Function<Integer, Multimap<Holder<Attribute>, AttributeModifier>> attributes = index -> ImmutableMultimap.of(); // Default empty attributes

    public CurioBaseItem(Item.Properties properties) {
        super(properties);
    }

    // Utility method to check if the curio is equipped by an entity
    public boolean isEquippedBy(@Nullable LivingEntity entity) {
        return entity != null && CuriosApi.getCuriosInventory(entity)
                .map(inv -> inv.findFirstCurio(this).isPresent())
                .orElse(false);
    }

    @NotNull
    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        // Apply attributes only if the slot matches
        if (slotContext.identifier().equals(this.attributeSlot)) {
            return attributes.apply(slotContext.index());
        }
        return ICurioItem.super.getAttributeModifiers(slotContext, id, stack);
    }

    /**
     * Configure attributes for the curio item with a specified slot.
     *
     * @param slot       The identifier for the curio slot.
     * @param attributes Attribute containers defining the item's effects.
     * @return This instance for chaining.
     */
    public CurioBaseItem withAttributes(String slot, AttributeContainer... attributes) {
        this.attributeSlot = slot;
        this.attributes = createAttributeFunction(slot, attributes);
        return this;
    }

    /**
     * Configure attributes for the curio item with the default slot.
     *
     * @param attributes Attribute containers defining the item's effects.
     * @return This instance for chaining.
     */
    public CurioBaseItem withAttributes(AttributeContainer... attributes) {
        return withAttributes(Curios.RING_SLOT, attributes);
    }

    /**
     * Helper method to create a function for attribute application.
     *
     * @param slot       The curio slot identifier.
     * @param attributes Attribute containers defining the effects.
     * @return A function that maps slot index to attributes.
     */
    private Function<Integer, Multimap<Holder<Attribute>, AttributeModifier>> createAttributeFunction(String slot, AttributeContainer[] attributes) {
        return index -> {
            ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder = ImmutableMultimap.builder();
            for (AttributeContainer holder : attributes) {
                String id = String.format("%s_%d", slot, index);
                builder.put(holder.attribute(), holder.createModifier(id));
            }
            return builder.build();
        };
    }
}
