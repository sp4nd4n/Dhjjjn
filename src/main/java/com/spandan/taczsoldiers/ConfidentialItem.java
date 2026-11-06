package com.spandan.taczsoldiers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ConfidentialItem extends Item {
    public final ConfidentialType type;

    public ConfidentialItem(ConfidentialType type) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.type = type;
    }

    public static ItemStack create(ConfidentialType type, int baseId) {
        ItemStack stack = new ItemStack(TaczSoldiers.CONFIDENTIAL.get(type).get());
        stack.getOrCreateTag().putInt("BaseId", baseId);
        return stack;
    }

    public static boolean isFor(ItemStack stack, int baseId) {
        return stack.getItem() instanceof ConfidentialItem && stack.hasTag()
                && stack.getTag().getInt("BaseId") == baseId;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("TOP SECRET").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Secure it, then call extraction with /call").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
