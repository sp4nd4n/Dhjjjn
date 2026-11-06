package com.spandan.taczsoldiers;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;

/** A player counts as disguised only when wearing a COMPLETE leather armor set dyed green. */
public final class Disguise {
    private Disguise() {
    }

    public static boolean isDisguised(Player player) {
        return greenLeather(player.getItemBySlot(EquipmentSlot.HEAD))
                && greenLeather(player.getItemBySlot(EquipmentSlot.CHEST))
                && greenLeather(player.getItemBySlot(EquipmentSlot.LEGS))
                && greenLeather(player.getItemBySlot(EquipmentSlot.FEET));
    }

    private static boolean greenLeather(ItemStack stack) {
        if (!(stack.getItem() instanceof DyeableArmorItem dyeable)) {
            return false;
        }
        if (!((DyeableLeatherItem) dyeable).hasCustomColor(stack)) {
            return false;
        }
        int color = dyeable.getColor(stack);
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        return g >= 60 && g - r >= 15 && g - b >= 15;
    }
}
