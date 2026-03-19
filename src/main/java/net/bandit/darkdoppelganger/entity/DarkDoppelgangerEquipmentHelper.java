package net.bandit.darkdoppelganger.entity;

import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.bandit.darkdoppelganger.Config;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class DarkDoppelgangerEquipmentHelper {

    private DarkDoppelgangerEquipmentHelper() {
    }

    public static void applyPlayerLoadout(Mob mob, Player player) {
        if (mob == null || player == null) return;

        List<? extends String> banned = Config.DOPPELGANGER_BANNED_ARMOR.get();

        // Main hand copies directly, same as current boss behavior
        ItemStack mainHand = player.getMainHandItem().copy();
        if (!mainHand.isEmpty()) {
            mainHand.setCount(1);
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, mainHand);

        // Offhand stays empty
        mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        // Armor follows the same banned/fallback rules as the boss
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;

            ItemStack playerArmor = player.getItemBySlot(slot);
            boolean bannedArmor = isArmorBanned(playerArmor, banned);

            ItemStack equip = (!playerArmor.isEmpty() && !bannedArmor)
                    ? playerArmor.copy()
                    : switch (slot) {
                case HEAD -> new ItemStack(ItemRegistry.NETHERITE_MAGE_HELMET.get());
                case CHEST -> new ItemStack(ItemRegistry.NETHERITE_MAGE_CHESTPLATE.get());
                case LEGS -> new ItemStack(ItemRegistry.NETHERITE_MAGE_LEGGINGS.get());
                case FEET -> new ItemStack(ItemRegistry.NETHERITE_MAGE_BOOTS.get());
                default -> ItemStack.EMPTY;
            };

            if (!equip.isEmpty()) {
                equip.setCount(1);
            }

            mob.setItemSlot(slot, equip);
        }

        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
        mob.setDropChance(EquipmentSlot.CHEST, 0.0F);
        mob.setDropChance(EquipmentSlot.LEGS, 0.0F);
        mob.setDropChance(EquipmentSlot.FEET, 0.0F);
    }

    public static boolean isArmorBanned(ItemStack stack, List<? extends String> bannedList) {
        if (stack.isEmpty()) return false;

        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        for (String ban : bannedList) {
            if (ban.endsWith("*")) {
                if (itemId.startsWith(ban.substring(0, ban.length() - 1))) return true;
            } else if (itemId.equals(ban)) {
                return true;
            }
        }
        return false;
    }
}