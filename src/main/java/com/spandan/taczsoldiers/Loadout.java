package com.spandan.taczsoldiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** What is inside the supply drop for a mission: armor, guns, ammo, attachments, grenades, food. */
public final class Loadout {
    private Loadout() {
    }

    public static List<ItemStack> build(MissionType type, Difficulty difficulty, RandomSource rnd) {
        List<ItemStack> items = new ArrayList<>();

        boolean heavy = type == MissionType.ASSAULT || type == MissionType.DEMOLITION;
        boolean tough = difficulty == Difficulty.HARD || difficulty == Difficulty.EXTREME;
        Item[] armor;
        if (!heavy) {
            armor = new Item[]{Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS};
        } else if (tough) {
            armor = new Item[]{Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
        } else {
            armor = new Item[]{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS};
        }
        for (Item piece : armor) {
            items.add(new ItemStack(piece));
        }

        Set<ResourceLocation> used = new HashSet<>();
        for (String[] gunClasses : type.guns) {
            ItemStack gun = TaczBridge.createGunOfTypes(gunClasses, rnd, used);
            if (gun.isEmpty()) {
                continue;
            }
            items.add(gun);
            int mag = TaczBridge.magSize(gun);
            int rounds = mag <= 3 ? 6 : Math.min(192, Math.max(90, mag * 4));
            items.addAll(TaczBridge.ammoFor(gun, rounds));
            items.addAll(TaczBridge.attachmentsFor(gun, rnd, gunClasses[0].equals("sniper") ? 3 : 2));
        }

        int grenades = type.grenades + (difficulty == Difficulty.EXTREME ? 2 : 0);
        items.add(new ItemStack(TaczSoldiers.GRENADE.get(), grenades));
        items.add(new ItemStack(Items.COOKED_BEEF, 16));
        items.add(new ItemStack(Items.GOLDEN_APPLE, 2));
        return items;
    }
}
