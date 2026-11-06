package com.spandan.taczsoldiers.world;

import com.spandan.taczsoldiers.ConfidentialType;
import com.spandan.taczsoldiers.world.BaseBuilder.Kind;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The three base designs. All coordinates are local; the gate always faces +z. */
public final class Structures {
    private Structures() {
    }

    private static BlockState s(net.minecraft.world.level.block.Block b) {
        return b.defaultBlockState();
    }

    // =================================================================================
    // Shared pieces
    // =================================================================================

    static void ground(BaseBuilder b, int r) {
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                b.set(x, -1, z, b.rnd.nextInt(5) == 0 ? b.t.ground2() : b.t.ground());
            }
        }
    }

    static void roads(BaseBuilder b, int r) {
        b.fill(-2, -1, -r, 2, -1, r, b.t.floor2());
        b.fill(-r, -1, -2, r, -1, 2, b.t.floor2());
        for (int i = -r; i <= r; i += 2) {
            b.set(0, -1, i, b.t.accent());
            b.set(i, -1, 0, b.t.accent());
        }
    }

    static void helipad(BaseBuilder b, int cx, int cz) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                if (Math.sqrt(x * x + z * z) <= 5.2) {
                    b.set(cx + x, -1, cz + z, s(Blocks.GRAY_CONCRETE));
                }
            }
        }
        b.ring(cx, cz, -1, 4.0, 4.7, s(Blocks.WHITE_CONCRETE));
        b.fill(cx - 1, -1, cz - 2, cx - 1, -1, cz + 2, s(Blocks.WHITE_CONCRETE));
        b.fill(cx + 1, -1, cz - 2, cx + 1, -1, cz + 2, s(Blocks.WHITE_CONCRETE));
        b.set(cx, -1, cz, s(Blocks.WHITE_CONCRETE));
        b.occupy(cx - 5, cz - 5, cx + 5, cz + 5);
        b.post(cx + 6, 0, cz, Kind.YARD);
    }

    /** A 7x7 corner/watch tower, 12 high with a ladder and a sniper deck. */
    static void tower(BaseBuilder b, int cx, int cz, int sx, int sz) {
        b.box(cx - 3, 0, cz - 3, cx + 3, 11, cz + 3, b.t.wall());
        b.air(cx - 2, 0, cz - 2, cx + 2, 10, cz + 2);
        b.fill(cx - 3, 6, cz - 3, cx + 3, 6, cz + 3, b.t.wall2());
        b.fill(cx - 3, 11, cz - 3, cx + 3, 11, cz + 3, b.t.floor2());
        for (int i = -3; i <= 3; i++) {
            if ((i + 3) % 2 == 0) {
                b.set(cx + i, 12, cz - 3, b.t.wall2());
                b.set(cx + i, 12, cz + 3, b.t.wall2());
                b.set(cx - 3, 12, cz + i, b.t.wall2());
                b.set(cx + 3, 12, cz + i, b.t.wall2());
            }
        }
        b.set(cx - 3, 13, cz - 3, b.t.light());
        b.set(cx + 3, 13, cz - 3, b.t.light());
        b.set(cx - 3, 13, cz + 3, b.t.light());
        b.set(cx + 3, 13, cz + 3, b.t.light());
        // windows
        for (int y : new int[]{3, 8}) {
            b.fill(cx, y, cz - 3, cx, y + 1, cz - 3, b.t.glass());
            b.fill(cx, y, cz + 3, cx, y + 1, cz + 3, b.t.glass());
            b.fill(cx - 3, y, cz, cx - 3, y + 1, cz, b.t.glass());
            b.fill(cx + 3, y, cz, cx + 3, y + 1, cz, b.t.glass());
        }
        // doorway towards the courtyard
        b.air(cx - 3 * sx, 0, cz - 2 * sz, cx - 3 * sx, 1, cz - 2 * sz);
        // ladder up to the deck
        Direction facing = sx > 0 ? Direction.WEST : Direction.EAST;
        for (int y = 0; y <= 12; y++) {
            b.ladder(cx + 2 * sx, y, cz, facing);
        }
        b.set(cx, 5, cz, b.t.light());
        b.post(cx, 12, cz, Kind.TOWER);
        b.post(cx - sx, 12, cz - sz, Kind.TOWER);
        b.occupy(cx - 3, cz - 3, cx + 3, cz + 3);
    }

    enum Interior { BARRACKS, ARMORY }

    static void building(BaseBuilder b, int cx, int cz, int hw, int hd, Interior kind) {
        BlockState wall = b.t.wall();
        b.box(cx - hw, 0, cz - hd, cx + hw, 5, cz + hd, wall);
        b.air(cx - hw + 1, 0, cz - hd + 1, cx + hw - 1, 4, cz + hd - 1);
        b.fill(cx - hw, 5, cz - hd, cx + hw, 5, cz + hd, b.t.roof());
        b.fill(cx - hw, 4, cz - hd, cx + hw, 4, cz - hd, b.t.wall2());
        // windows on both long faces
        for (int x = cx - hw + 2; x <= cx + hw - 2; x += 3) {
            b.fill(x, 2, cz - hd, x, 3, cz - hd, b.t.glass());
            b.fill(x, 2, cz + hd, x, 3, cz + hd, b.t.glass());
        }
        // door facing the middle of the base
        int dz = cz < 0 ? 1 : -1;
        int doorZ = cz + dz * hd;
        b.air(cx, 0, doorZ, cx, 2, doorZ);
        b.air(cx + 1, 0, doorZ, cx + 1, 2, doorZ);
        b.set(cx - 1, 3, doorZ, b.t.trim());
        // lights
        for (int x = cx - hw + 2; x <= cx + hw - 2; x += 4) {
            b.set(x, 4, cz, b.t.light());
        }
        if (kind == Interior.BARRACKS) {
            for (int x = cx - hw + 2; x <= cx + hw - 2; x += 3) {
                b.bed(x, 0, cz - hd + 1, Direction.SOUTH);
                b.bed(x, 0, cz + hd - 2, Direction.NORTH);
            }
        } else {
            for (int x = cx - hw + 2; x <= cx + hw - 2; x += 4) {
                b.chest(x, 0, cz - hd + 1, Direction.SOUTH, b.loot());
                b.set(x + 1, 0, cz - hd + 1, s(Blocks.BARREL));
                b.set(x, 0, cz + hd - 1, s(Blocks.BARREL));
                b.set(x + 1, 0, cz + hd - 1, s(Blocks.SMITHING_TABLE));
            }
        }
        b.post(cx - 2, 0, cz, Kind.INSIDE);
        b.post(cx + 2, 0, cz, Kind.INSIDE);
        b.occupy(cx - hw, cz - hd, cx + hw, cz + hd);
    }

    /** Open-fronted vehicle hangar (open towards +x). */
    static void garage(BaseBuilder b, int cx, int cz, int hw, int hd) {
        b.box(cx - hw, 0, cz - hd, cx - hw, 6, cz + hd, b.t.wall());
        b.box(cx - hw, 0, cz - hd, cx + hw, 6, cz - hd, b.t.wall());
        b.box(cx - hw, 0, cz + hd, cx + hw, 6, cz + hd, b.t.wall());
        b.fill(cx - hw, 7, cz - hd, cx + hw, 7, cz + hd, b.t.roof());
        b.air(cx - hw + 1, 0, cz - hd + 1, cx + hw, 6, cz + hd - 1);
        b.fill(cx - hw + 1, -1, cz - hd + 1, cx + hw, -1, cz + hd - 1, b.t.floor2());
        b.fill(cx + hw, 0, cz - hd, cx + hw, 6, cz - hd, b.t.trim());
        b.fill(cx + hw, 0, cz + hd, cx + hw, 6, cz + hd, b.t.trim());
        b.fill(cx - hw, 6, cz - hd + 1, cx + hw, 6, cz + hd - 1, b.t.wall2());
        for (int x = cx - hw + 3; x <= cx + hw - 3; x += 5) {
            b.set(x, 6, cz, b.t.light());
        }
        b.fill(cx - hw + 1, 0, cz - hd + 1, cx - hw + 2, 1, cz - hd + 1, s(Blocks.BARREL));
        b.set(cx - hw + 1, 0, cz + hd - 1, s(Blocks.BARREL));
        b.set(cx - hw + 1, 1, cz + hd - 1, s(Blocks.BARREL));
        b.occupy(cx - hw, cz - hd, cx + hw, cz + hd);
        b.tank(cx - 1, cz);
        b.post(cx + hw + 2, 0, cz, Kind.YARD);
    }

    // =================================================================================
    // Command building (compound centrepiece)
    // =================================================================================

    static void commandBuilding(BaseBuilder b, ConfidentialType item, int baseId) {
        int hw = 11;
        int hd = 8;
        b.box(-hw, 0, -hd, hw, 10, hd, b.t.wall());
        b.air(-hw + 1, 0, -hd + 1, hw - 1, 9, hd - 1);
        b.fill(-hw, 10, -hd, hw, 10, hd, b.t.roof());
        b.fill(-hw + 1, 5, -hd + 1, hw - 1, 5, hd - 1, b.t.floor2());
        // accent bands
        b.fill(-hw, 5, -hd, hw, 5, -hd, b.t.wall2());
        b.fill(-hw, 5, hd, hw, 5, hd, b.t.wall2());
        b.fill(-hw, 5, -hd, -hw, 5, hd, b.t.wall2());
        b.fill(hw, 5, -hd, hw, 5, hd, b.t.wall2());
        // windows
        for (int x = -9; x <= 9; x += 3) {
            for (int y : new int[]{2, 7}) {
                b.fill(x, y, -hd, x, y + 1, -hd, b.t.glass());
                b.fill(x, y, hd, x, y + 1, hd, b.t.glass());
            }
        }
        for (int z = -5; z <= 5; z += 3) {
            for (int y : new int[]{2, 7}) {
                b.fill(-hw, y, z, -hw, y + 1, z, b.t.glass());
                b.fill(hw, y, z, hw, y + 1, z, b.t.glass());
            }
        }
        // main entrance (south)
        b.air(-1, 0, hd, 1, 2, hd);
        b.fill(-2, 0, hd, -2, 3, hd, b.t.trim());
        b.fill(2, 0, hd, 2, 3, hd, b.t.trim());
        b.fill(-2, 3, hd, 2, 3, hd, b.t.trim());
        b.set(-2, 4, hd, b.t.light());
        b.set(2, 4, hd, b.t.light());
        // stairs to the upper floor
        b.air(8, 5, -6, 9, 5, -2);
        for (int i = 0; i < 5; i++) {
            b.stair(8, i, -6 + i, Blocks.STONE_BRICK_STAIRS, Direction.SOUTH);
            b.stair(9, i, -6 + i, Blocks.STONE_BRICK_STAIRS, Direction.SOUTH);
        }
        // vault
        b.box(-4, 0, -7, 4, 4, -2, s(Blocks.IRON_BLOCK));
        b.air(-3, 0, -6, 3, 3, -3);
        b.set(-2, 3, -4, b.t.light());
        b.set(2, 3, -4, b.t.light());
        b.set(0, 3, -5, b.t.light());
        b.air(0, 0, -2, 0, 1, -2);
        b.ironDoor(0, 0, -2, Direction.SOUTH);
        b.fill(1, 0, -2, 1, 3, -2, b.t.trim());
        b.button(1, 1, -1, Direction.SOUTH);
        b.secretChest(0, 0, -6, Direction.SOUTH, item, baseId);
        b.chest(-3, 0, -6, Direction.SOUTH, b.loot());
        b.chest(3, 0, -6, Direction.SOUTH, b.loot());
        // furniture and carpets
        b.fill(-1, 0, -1, 1, 0, hd - 1, s(Blocks.RED_CARPET));
        for (int x : new int[]{-9, -7, 7, 9}) {
            b.set(x, 0, 6, s(Blocks.CARTOGRAPHY_TABLE));
        }
        b.set(-9, 0, -1, s(Blocks.LECTERN));
        b.set(-9, 0, 1, s(Blocks.SMITHING_TABLE));
        b.set(9, 0, 1, s(Blocks.BLAST_FURNACE));
        // ceiling lights, ground floor
        for (int x = -8; x <= 8; x += 4) {
            for (int z = -5; z <= 5; z += 5) {
                b.set(x, 5, z, b.t.light());
            }
        }
        // upper floor: briefing table and roof lights
        b.fill(-4, 6, -1, 4, 6, 1, s(Blocks.DARK_OAK_SLAB));
        for (int x : new int[]{-6, 6}) {
            b.set(x, 6, -4, s(Blocks.LECTERN));
            b.set(x, 6, 4, s(Blocks.CARTOGRAPHY_TABLE));
        }
        for (int x = -8; x <= 8; x += 4) {
            b.set(x, 9, 0, b.t.light());
            b.set(x, 9, -5, b.t.light());
            b.set(x, 9, 5, b.t.light());
        }
        // roof parapet, mast and dish
        for (int i = -hw; i <= hw; i++) {
            if (i % 2 == 0) {
                b.set(i, 11, -hd, b.t.wall2());
                b.set(i, 11, hd, b.t.wall2());
            }
        }
        for (int i = -hd; i <= hd; i++) {
            if (i % 2 == 0) {
                b.set(-hw, 11, i, b.t.wall2());
                b.set(hw, 11, i, b.t.wall2());
            }
        }
        b.fill(-8, 11, -4, -8, 19, -4, s(Blocks.IRON_BLOCK));
        b.fill(-8, 20, -4, -8, 23, -4, s(Blocks.LIGHTNING_ROD));
        b.ring(6, -3, 12, 0, 3.2, s(Blocks.WHITE_CONCRETE));
        b.fill(6, 11, -3, 6, 11, -3, s(Blocks.IRON_BLOCK));
        b.set(6, 12, -3, s(Blocks.IRON_BLOCK));
        // posts
        b.post(-7, 0, 3, Kind.INSIDE);
        b.post(7, 0, 3, Kind.INSIDE);
        b.post(0, 0, 5, Kind.INSIDE);
        b.post(-3, 0, -1, Kind.VAULT);
        b.post(3, 0, -1, Kind.VAULT);
        b.post(0, 0, 1, Kind.VAULT);
        b.post(-7, 6, 0, Kind.INSIDE);
        b.post(7, 6, 2, Kind.INSIDE);
        b.post(0, 6, 4, Kind.INSIDE);
        b.post(-9, 11, 5, Kind.ROOF);
        b.post(9, 11, -5, Kind.ROOF);
        b.post(9, 11, 5, Kind.ROOF);
        b.occupy(-hw, -hd, hw, hd);
    }

    // =================================================================================
    // Research dome (facility centrepiece)
    // =================================================================================

    static void dome(BaseBuilder b, ConfidentialType item, int baseId) {
        int R = 13;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                double flat = Math.sqrt(x * x + z * z);
                if (flat <= R - 1) {
                    b.set(x, -1, z, b.t.floor2());
                    if (flat <= 5.5 && flat >= 4.5) {
                        b.set(x, -1, z, b.t.light());
                    }
                }
                for (int y = 0; y <= R; y++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d <= R + 0.3 && d > R - 1.3) {
                        boolean entrance = z > 0 && Math.abs(x) <= 2 && y <= 3;
                        if (!entrance) {
                            b.set(x, y, z, y <= 3 ? b.t.wall() : b.t.glass());
                        }
                    }
                }
            }
        }
        // entrance frame
        b.fill(-3, 0, R - 1, -3, 4, R, b.t.trim());
        b.fill(3, 0, R - 1, 3, 4, R, b.t.trim());
        b.fill(-3, 4, R - 1, 3, 4, R, b.t.trim());
        // secure cage in the centre with the secret on a pedestal
        b.box(-3, 0, -3, 3, 5, 3, s(Blocks.RED_STAINED_GLASS));
        b.air(-2, 0, -2, 2, 4, 2);
        b.fill(-1, 0, -1, 1, 1, 1, b.t.trim());
        b.secretChest(0, 2, 0, Direction.SOUTH, item, baseId);
        b.set(-2, 4, -2, b.t.light());
        b.set(2, 4, -2, b.t.light());
        b.set(-2, 4, 2, b.t.light());
        b.set(2, 4, 2, b.t.light());
        b.air(0, 0, 3, 0, 1, 3);
        b.ironDoor(0, 0, 3, Direction.SOUTH);
        b.set(1, 0, 3, b.t.trim());
        b.set(1, 1, 3, b.t.trim());
        b.button(1, 1, 4, Direction.SOUTH);
        // lab benches around the ring
        BlockState[] props = {s(Blocks.CARTOGRAPHY_TABLE), s(Blocks.SMITHING_TABLE), s(Blocks.LECTERN),
                s(Blocks.BLAST_FURNACE), s(Blocks.BREWING_STAND), s(Blocks.BARREL)};
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16;
            int x = (int) Math.round(Math.cos(a) * 9);
            int z = (int) Math.round(Math.sin(a) * 9);
            if (z > 6 && Math.abs(x) < 4) {
                continue;
            }
            b.set(x, 0, z, props[i % props.length]);
        }
        // upper catwalk ring with lights
        b.ring(0, 0, 5, 8.5, 10.5, b.t.floor2());
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            b.set((int) Math.round(Math.cos(a) * 9.5), 6, (int) Math.round(Math.sin(a) * 9.5), b.t.light());
        }
        // posts
        b.post(6, 0, 6, Kind.INSIDE);
        b.post(-6, 0, 6, Kind.INSIDE);
        b.post(6, 0, -6, Kind.INSIDE);
        b.post(-6, 0, -6, Kind.INSIDE);
        b.post(-4, 0, 4, Kind.VAULT);
        b.post(4, 0, 4, Kind.VAULT);
        b.post(0, 0, 6, Kind.VAULT);
        b.post(8, 0, 0, Kind.INSIDE);
        b.post(-8, 0, 0, Kind.INSIDE);
        b.occupy(-R, -R, R, R);
    }

    // =================================================================================
    // Vault bunker (outpost centrepiece)
    // =================================================================================

    static void vaultBunker(BaseBuilder b, int cx, int cz, int hw, int hd, ConfidentialType item, int baseId) {
        b.box(cx - hw, 0, cz - hd, cx + hw, 4, cz + hd, b.t.wall());
        b.air(cx - hw + 1, 0, cz - hd + 1, cx + hw - 1, 3, cz + hd - 1);
        b.fill(cx - hw, 4, cz - hd, cx + hw, 4, cz + hd, b.t.roof());
        int dz = cz < 0 ? 1 : -1;
        int doorZ = cz + dz * hd;
        Direction out = dz > 0 ? Direction.SOUTH : Direction.NORTH;
        b.air(cx, 0, doorZ, cx, 1, doorZ);
        b.ironDoor(cx, 0, doorZ, out);
        b.set(cx + 1, 0, doorZ, b.t.trim());
        b.set(cx + 1, 1, doorZ, b.t.trim());
        b.button(cx + 1, 1, doorZ + dz, out);
        b.set(cx, 3, cz, b.t.light());
        b.set(cx - hw + 2, 3, cz, b.t.light());
        b.set(cx + hw - 2, 3, cz, b.t.light());
        b.fill(cx - 1, 0, cz - dz * (hd - 1), cx + 1, 0, cz - dz * (hd - 1), s(Blocks.RED_CARPET));
        b.secretChest(cx, 0, cz - dz * (hd - 1), out, item, baseId);
        b.chest(cx - hw + 1, 0, cz - dz * (hd - 1), out, b.loot());
        b.chest(cx + hw - 1, 0, cz - dz * (hd - 1), out, b.loot());
        b.post(cx - 2, 0, doorZ + dz * 2, Kind.VAULT);
        b.post(cx + 3, 0, doorZ + dz * 2, Kind.VAULT);
        b.post(cx, 0, cz, Kind.INSIDE);
        b.occupy(cx - hw, cz - hd, cx + hw, cz + hd);
    }

    // =================================================================================
    // Walls
    // =================================================================================

    static void walls(BaseBuilder b, int r) {
        BlockState w = b.t.wall();
        BlockState w2 = b.t.wall2();
        b.fill(-r, 0, -r, r, 4, -r + 1, w);
        b.fill(-r, 0, r - 1, r, 4, r, w);
        b.fill(-r, 0, -r, -r + 1, 4, r, w);
        b.fill(r - 1, 0, -r, r, 4, r, w);
        // top cap accent and crenellations on the outer row
        for (int i = -r; i <= r; i++) {
            b.set(i, 4, -r + 1, w2);
            b.set(i, 4, r - 1, w2);
            b.set(-r + 1, 4, i, w2);
            b.set(r - 1, 4, i, w2);
            if (i % 2 == 0) {
                b.set(i, 5, -r, w2);
                b.set(i, 5, r, w2);
                b.set(-r, 5, i, w2);
                b.set(r, 5, i, w2);
            }
        }
        // firing slits
        for (int i = -r + 5; i <= r - 5; i += 6) {
            b.set(i, 2, -r, b.t.glass());
            b.set(i, 2, -r + 1, b.t.glass());
            b.set(i, 2, r, b.t.glass());
            b.set(i, 2, r - 1, b.t.glass());
            b.set(-r, 2, i, b.t.glass());
            b.set(-r + 1, 2, i, b.t.glass());
            b.set(r, 2, i, b.t.glass());
            b.set(r - 1, 2, i, b.t.glass());
        }
        // gate (south)
        b.air(-3, 0, r - 1, 3, 3, r);
        b.fill(-4, 0, r - 1, -4, 4, r, b.t.trim());
        b.fill(4, 0, r - 1, 4, 4, r, b.t.trim());
        b.set(-3, 3, r - 1, b.t.light());
        b.set(3, 3, r - 1, b.t.light());
        b.fill(-4, 6, r, -4, 6, r, b.t.light());
        b.fill(4, 6, r, 4, 6, r, b.t.light());
        // wall-top guards
        for (int i = -r + 8; i <= r - 8; i += 8) {
            b.post(i, 5, -r + 1, Kind.WALL);
            b.post(-r + 1, 5, i, Kind.WALL);
            b.post(r - 1, 5, i, Kind.WALL);
            if (Math.abs(i) > 5) {
                b.post(i, 5, r - 1, Kind.WALL);
            }
        }
        // gate guards, inside and outside
        b.post(-5, 0, r - 3, Kind.GATE);
        b.post(5, 0, r - 3, Kind.GATE);
        b.post(0, 0, r - 6, Kind.GATE);
        b.post(-6, 0, r + 4, Kind.GATE);
        b.post(6, 0, r + 4, Kind.GATE);
        b.post(0, 0, r + 7, Kind.GATE);
        // roaming guards outside
        b.post(r + 4, 0, 0, Kind.YARD);
        b.post(-r - 4, 0, 0, Kind.YARD);
        b.post(0, 0, -r - 4, Kind.YARD);
        // sandbag cover by the gate
        b.sandbags(-9, r - 8, -6, r - 8);
        b.sandbags(6, r - 8, 9, r - 8);
        b.sandbags(-9, r + 5, -6, r + 5);
        b.sandbags(6, r + 5, 9, r + 5);
    }

    static void scatterYardPosts(BaseBuilder b, int r, int n) {
        int placed = 0;
        for (int tries = 0; tries < 200 && placed < n; tries++) {
            int x = b.rnd.nextInt(2 * r - 6) - r + 3;
            int z = b.rnd.nextInt(2 * r - 6) - r + 3;
            if (!b.isOccupied(x, z)) {
                b.post(x, 0, z, Kind.YARD);
                placed++;
            }
        }
    }

    static void lamps(BaseBuilder b, int r) {
        for (int i = -r + 6; i <= r - 4; i += 8) {
            b.lamp(-4, i);
            b.lamp(4, i);
            b.lamp(i, -4);
            b.lamp(i, 4);
        }
    }

    // =================================================================================
    // Complete bases
    // =================================================================================

    public static void compound(BaseBuilder b, int r, boolean facility, ConfidentialType item, int baseId) {
        ground(b, r + 1);
        roads(b, r);
        walls(b, r);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                tower(b, sx * r, sz * r, sx, sz);
            }
        }
        if (facility) {
            dome(b, item, baseId);
            b.dish(0, -(r - 5));
            b.dish(-(r - 8), 4);
            b.dish(r - 8, 4);
        } else {
            commandBuilding(b, item, baseId);
        }
        building(b, -(r - 10), -(r - 9), 7, 4, Interior.BARRACKS);
        helipad(b, r - 8, -(r - 8));
        building(b, r - 9, -9, 6, 4, Interior.ARMORY);
        garage(b, -(r - 10), r - 9, 7, 5);
        b.container(r - 17, r - 6, s(Blocks.BLUE_CONCRETE));
        b.container(r - 17, r - 10, s(Blocks.RED_TERRACOTTA));
        b.container(r - 12, r - 6, s(Blocks.ORANGE_CONCRETE));
        b.fuelTank(r - 5, 9);
        b.fuelTank(r - 5, 14);
        b.tank(7, 15);
        b.tank(-6, 17);
        lamps(b, r);
        scatterYardPosts(b, r, 16);
    }

    public static void outpost(BaseBuilder b, int r, ConfidentialType item, int baseId) {
        ground(b, r + 1);
        b.fill(-2, -1, -r, 2, -1, r, b.t.floor2());
        // sandbag perimeter with a gate gap in the south
        for (int i = -r; i <= r; i++) {
            b.fill(i, 0, -r, i, 1, -r, s(Blocks.PACKED_MUD));
            b.fill(-r, 0, i, -r, 1, i, s(Blocks.PACKED_MUD));
            b.fill(r, 0, i, r, 1, i, s(Blocks.PACKED_MUD));
            if (Math.abs(i) > 3) {
                b.fill(i, 0, r, i, 1, r, s(Blocks.PACKED_MUD));
            }
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                b.fill(sx * r - 1, 0, sz * r - 1, sx * r + 1, 2, sz * r + 1, s(Blocks.PACKED_MUD));
                b.post(sx * (r - 2), 0, sz * (r - 2), Kind.WALL);
            }
        }
        b.post(-5, 0, r + 3, Kind.GATE);
        b.post(5, 0, r + 3, Kind.GATE);
        b.post(0, 0, r - 2, Kind.GATE);
        tower(b, 0, -(r - 5), 1, 1);
        vaultBunker(b, -(r - 7), 5, 5, 4, item, baseId);
        building(b, r - 8, 5, 4, 3, Interior.BARRACKS);
        b.container(-4, r - 4, s(Blocks.ORANGE_CONCRETE));
        b.container(2, -(r - 11) + 3, s(Blocks.BLUE_CONCRETE));
        b.fuelTank(r - 4, -(r - 5));
        b.sandbags(-7, r - 9, -4, r - 9);
        b.sandbags(4, r - 9, 7, r - 9);
        lamps(b, r);
        scatterYardPosts(b, r, 8);
    }
}
