package com.spandan.taczsoldiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/**
 * Every soldier role: which gun class it carries, how it fights and what it wears.
 * Tweak the numbers here to rebalance a role.
 */
public enum SoldierRole {

    //          id         name              gun types (first one installed wins)   hp   speed  engage prefer retreat burst   pause   reload inacc strafe react
    RIFLEMAN("rifleman", "Rifleman",         new String[]{"rifle", "smg"},          20f, 0.27,  45,    26,    8,     3, 6,   10, 25,  50,   2.5f, true,  10, 1,
            p(Items.IRON_HELMET, -1), p(Items.LEATHER_CHESTPLATE, 0x4B5320), p(Items.LEATHER_LEGGINGS, 0x4B5320), p(Items.LEATHER_BOOTS, 0x2F2F1F),
            0x4B5320, 0x8A9A5B),

    SNIPER("sniper", "Sniper",               new String[]{"sniper", "rifle"},       16f, 0.23,  80,    60,    22,    1, 1,   30, 60,  70,   0.6f, false, 20, 0,
            p(Items.LEATHER_HELMET, 0x3B3A26), p(Items.LEATHER_CHESTPLATE, 0x5C5A3A), p(Items.LEATHER_LEGGINGS, 0x5C5A3A), p(Items.LEATHER_BOOTS, 0x3B3A26),
            0x3B3A26, 0xC2B280),

    GUNNER("gunner", "Machine Gunner",       new String[]{"mg", "rifle"},           34f, 0.20,  45,    30,    6,     10, 20, 25, 45,  120,  4.5f, false, 15, 0,
            p(Items.IRON_HELMET, -1), p(Items.CHAINMAIL_CHESTPLATE, -1), p(Items.LEATHER_LEGGINGS, 0x4B5320), p(Items.LEATHER_BOOTS, 0x2F2F1F),
            0x556B2F, 0x2F4F4F),

    BREACHER("breacher", "Breacher",         new String[]{"shotgun", "smg"},        26f, 0.33,  22,    8,     0,     1, 2,   18, 30,  70,   2.0f, true,  8, 2,
            p(Items.IRON_HELMET, -1), p(Items.LEATHER_CHESTPLATE, 0xC2A878), p(Items.LEATHER_LEGGINGS, 0xC2A878), p(Items.LEATHER_BOOTS, 0x6B5B3A),
            0xC2A878, 0x6B5B3A),

    OPERATOR("operator", "Special Ops",      new String[]{"smg", "rifle"},          24f, 0.34,  35,    16,    6,     4, 8,   8, 18,   45,   2.0f, true,  6, 2,
            p(Items.NETHERITE_HELMET, -1), p(Items.LEATHER_CHESTPLATE, 0x1A1A1A), p(Items.LEATHER_LEGGINGS, 0x1A1A1A), p(Items.LEATHER_BOOTS, 0x111111),
            0x1A1A1A, 0x6E6E6E),

    OFFICER("officer", "Officer",            new String[]{"pistol", "smg"},         40f, 0.27,  32,    20,    10,    2, 3,   15, 30,  40,   2.0f, true,  10, 1,
            p(Items.LEATHER_HELMET, 0x6B0F1A), p(Items.IRON_CHESTPLATE, -1), p(Items.LEATHER_LEGGINGS, 0x3D4F2A), p(Items.LEATHER_BOOTS, 0x1A1A1A),
            0x6B0F1A, 0xD4AF37),

    MEDIC("medic", "Medic",                  new String[]{"smg", "pistol", "rifle"}, 20f, 0.30, 30,    18,    10,    3, 5,   15, 30,  45,   3.0f, true,  10, 0,
            p(Items.LEATHER_HELMET, 0xF2F2F2), p(Items.LEATHER_CHESTPLATE, 0xE8E8E8), p(Items.LEATHER_LEGGINGS, 0x4B5320), p(Items.LEATHER_BOOTS, 0x2F2F1F),
            0xF2F2F2, 0xD62828),

    ROCKETGUNNER("rocketgunner", "Rocket Gunner", new String[]{"rpg", "rifle"},     28f, 0.24,  55,    38,    12,    1, 1,   70, 110, 100,  1.5f, false, 25, 1,
            p(Items.IRON_HELMET, -1), p(Items.LEATHER_CHESTPLATE, 0x6B4F2A), p(Items.LEATHER_LEGGINGS, 0x4B5320), p(Items.LEATHER_BOOTS, 0x2F2F1F),
            0x6B4F2A, 0xE07B00);

    /** An armour piece, optionally dyed (leather only). colour -1 = undyed. */
    public record Piece(Item item, int color) {
        public ItemStack create() {
            ItemStack stack = new ItemStack(item);
            if (color >= 0) {
                stack.getOrCreateTagElement("display").putInt("color", color);
            }
            return stack;
        }
    }

    private static Piece p(Item item, int color) {
        return new Piece(item, color);
    }

    public final String id;
    public final String displayName;
    /** Gun classes in priority order: "pistol", "smg", "rifle", "shotgun", "sniper", "mg". */
    public final String[] gunTypes;
    public final float health;
    public final double speed;
    /** Starts shooting when the target is inside this distance (and visible). */
    public final double engageRange;
    /** Walks toward the target until closer than this. */
    public final double preferredRange;
    /** Backs away when the target is closer than this. */
    public final double retreatRange;
    public final int burstMin, burstMax;
    public final int pauseMin, pauseMax;
    public final int reloadTicks;
    /** Aim error in degrees. */
    public final float inaccuracy;
    public final boolean strafes;
    /** Ticks the soldier must see you before opening fire. */
    public final int reactionTicks;
    /** Grenades carried. */
    public final int grenades;
    public final Piece head, chest, legs, feet;
    public final int eggPrimary, eggSecondary;
    public final ResourceLocation texture;

    SoldierRole(String id, String displayName, String[] gunTypes, float health, double speed,
                double engageRange, double preferredRange, double retreatRange,
                int burstMin, int burstMax, int pauseMin, int pauseMax, int reloadTicks,
                float inaccuracy, boolean strafes, int reactionTicks, int grenades,
                Piece head, Piece chest, Piece legs, Piece feet,
                int eggPrimary, int eggSecondary) {
        this.id = id;
        this.displayName = displayName;
        this.gunTypes = gunTypes;
        this.health = health;
        this.speed = speed;
        this.engageRange = engageRange;
        this.preferredRange = preferredRange;
        this.retreatRange = retreatRange;
        this.burstMin = burstMin;
        this.burstMax = burstMax;
        this.pauseMin = pauseMin;
        this.pauseMax = pauseMax;
        this.reloadTicks = reloadTicks;
        this.inaccuracy = inaccuracy;
        this.strafes = strafes;
        this.reactionTicks = reactionTicks;
        this.grenades = grenades;
        this.head = head;
        this.chest = chest;
        this.legs = legs;
        this.feet = feet;
        this.eggPrimary = eggPrimary;
        this.eggSecondary = eggSecondary;
        this.texture = new ResourceLocation(TaczSoldiers.MODID, "textures/entity/" + id + ".png");
    }

    public void equipArmor(Mob mob) {
        mob.setItemSlot(EquipmentSlot.HEAD, head.create());
        mob.setItemSlot(EquipmentSlot.CHEST, chest.create());
        mob.setItemSlot(EquipmentSlot.LEGS, legs.create());
        mob.setItemSlot(EquipmentSlot.FEET, feet.create());
    }

    @Nullable
    public static SoldierRole byId(String id) {
        for (SoldierRole role : values()) {
            if (role.id.equalsIgnoreCase(id)) {
                return role;
            }
        }
        return null;
    }
}
