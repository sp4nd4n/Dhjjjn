package com.spandan.taczsoldiers.world;

import com.spandan.taczsoldiers.ConfidentialItem;
import com.spandan.taczsoldiers.ConfidentialType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.ArrayList;
import java.util.List;

/**
 * Places blocks in a local coordinate system (x east, z south, y up, origin = centre of the base at floor level)
 * that is rotated randomly into the world. Also records where soldiers should stand guard.
 */
public final class BaseBuilder {
    public enum Kind { TOWER, WALL, GATE, YARD, INSIDE, VAULT, ROOF }

    public record Post(BlockPos pos, Kind kind) {
    }

    private static final int FLAGS = 2 | 16;
    public static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final ServerLevel level;
    private final BlockPos origin;
    private final Rotation rot;
    public final RandomSource rnd;
    public final Theme t;
    public final List<Post> posts = new ArrayList<>();
    private final List<int[]> occupied = new ArrayList<>();

    public BaseBuilder(ServerLevel level, BlockPos origin, Rotation rot, Theme theme) {
        this.level = level;
        this.origin = origin;
        this.rot = rot;
        this.rnd = level.getRandom();
        this.t = theme;
    }

    // ---- coordinates ---------------------------------------------------------------

    public BlockPos w(int x, int y, int z) {
        int rx;
        int rz;
        switch (rot) {
            case CLOCKWISE_90 -> {
                rx = -z;
                rz = x;
            }
            case CLOCKWISE_180 -> {
                rx = -x;
                rz = -z;
            }
            case COUNTERCLOCKWISE_90 -> {
                rx = z;
                rz = -x;
            }
            default -> {
                rx = x;
                rz = z;
            }
        }
        return origin.offset(rx, y, rz);
    }

    // ---- basic placement -----------------------------------------------------------

    public void set(int x, int y, int z, BlockState state) {
        level.setBlock(w(x, y, z), state.rotate(rot), FLAGS);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    public void air(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, AIR);
    }

    /** Solid block (carve the inside yourself with air()). */
    public void box(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        fill(x1, y1, z1, x2, y2, z2, state);
    }

    public void ring(int cx, int cz, int y, double rInner, double rOuter, BlockState state) {
        int r = (int) Math.ceil(rOuter);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d >= rInner && d <= rOuter) {
                    set(cx + x, y, cz + z, state);
                }
            }
        }
    }

    public void stair(int x, int y, int z, Block block, Direction ascend) {
        set(x, y, z, block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, ascend)
                .setValue(BlockStateProperties.HALF, Half.BOTTOM));
    }

    public void ladder(int x, int y, int z, Direction facing) {
        set(x, y, z, Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
    }

    public void ironDoor(int x, int y, int z, Direction facing) {
        BlockState lower = Blocks.IRON_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        set(x, y, z, lower);
        set(x, y + 1, z, lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
    }

    public void button(int x, int y, int z, Direction facing) {
        set(x, y, z, Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
    }

    public void bed(int x, int y, int z, Direction facing) {
        BlockState foot = Blocks.GREEN_BED.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
        set(x, y, z, foot);
        set(x + facing.getStepX(), y, z + facing.getStepZ(), foot.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
    }

    /** Chest (or barrel) filled with the given items. */
    public void chest(int x, int y, int z, Direction facing, List<ItemStack> items) {
        BlockPos p = w(x, y, z);
        level.setBlock(p, Blocks.CHEST.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing).rotate(rot), 2);
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof Container container) {
            for (int i = 0; i < items.size() && i < container.getContainerSize(); i++) {
                container.setItem(i, items.get(i));
            }
        }
    }

    public void secretChest(int x, int y, int z, Direction facing, ConfidentialType type, int baseId) {
        List<ItemStack> items = new ArrayList<>();
        items.add(ConfidentialItem.create(type, baseId));
        chest(x, y, z, facing, items);
    }

    public List<ItemStack> loot() {
        List<ItemStack> list = new ArrayList<>();
        int n = 3 + rnd.nextInt(4);
        for (int i = 0; i < n; i++) {
            switch (rnd.nextInt(9)) {
                case 0 -> list.add(new ItemStack(Items.IRON_INGOT, 8 + rnd.nextInt(17)));
                case 1 -> list.add(new ItemStack(Items.GOLD_INGOT, 4 + rnd.nextInt(9)));
                case 2 -> list.add(new ItemStack(Items.DIAMOND, 1 + rnd.nextInt(3)));
                case 3 -> list.add(new ItemStack(Items.EMERALD, 2 + rnd.nextInt(5)));
                case 4 -> list.add(new ItemStack(Items.COOKED_BEEF, 8 + rnd.nextInt(9)));
                case 5 -> list.add(new ItemStack(Items.GOLDEN_APPLE, 1 + rnd.nextInt(2)));
                case 6 -> list.add(new ItemStack(Items.GUNPOWDER, 8 + rnd.nextInt(9)));
                case 7 -> list.add(new ItemStack(Items.COAL, 8 + rnd.nextInt(17)));
                default -> list.add(new ItemStack(Items.BREAD, 6 + rnd.nextInt(7)));
            }
        }
        return list;
    }

    // ---- posts & footprints --------------------------------------------------------

    public void post(int x, int y, int z, Kind kind) {
        posts.add(new Post(w(x, y, z), kind));
    }

    public void occupy(int x1, int z1, int x2, int z2) {
        occupied.add(new int[]{Math.min(x1, x2) - 1, Math.min(z1, z2) - 1, Math.max(x1, x2) + 1, Math.max(z1, z2) + 1});
    }

    public boolean isOccupied(int x, int z) {
        for (int[] o : occupied) {
            if (x >= o[0] && x <= o[2] && z >= o[1] && z <= o[3]) {
                return true;
            }
        }
        return false;
    }

    // ---- props ---------------------------------------------------------------------

    public void lamp(int x, int z) {
        if (isOccupied(x, z)) {
            return;
        }
        fill(x, 0, z, x, 3, z, Blocks.POLISHED_ANDESITE_WALL.defaultBlockState());
        set(x, 4, z, t.light());
    }

    public void tank(int cx, int cz) {
        BlockState hull = Blocks.GREEN_TERRACOTTA.defaultBlockState();
        BlockState camo = Blocks.BROWN_TERRACOTTA.defaultBlockState();
        BlockState track = Blocks.BLACK_CONCRETE.defaultBlockState();
        fill(cx - 3, 0, cz - 2, cx + 3, 0, cz - 2, track);
        fill(cx - 3, 0, cz + 2, cx + 3, 0, cz + 2, track);
        fill(cx - 3, 1, cz - 2, cx + 3, 1, cz - 2, track);
        fill(cx - 3, 1, cz + 2, cx + 3, 1, cz + 2, track);
        fill(cx - 3, 0, cz - 1, cx + 3, 1, cz + 1, hull);
        fill(cx - 2, 2, cz - 1, cx + 2, 2, cz + 1, camo);
        fill(cx - 1, 3, cz - 1, cx + 1, 3, cz + 1, hull);
        fill(cx + 2, 3, cz, cx + 6, 3, cz, Blocks.BLACK_CONCRETE.defaultBlockState());
        set(cx - 1, 4, cz, Blocks.IRON_BLOCK.defaultBlockState());
        occupy(cx - 3, cz - 2, cx + 6, cz + 2);
        post(cx - 4, 0, cz, Kind.YARD);
        post(cx, 0, cz + 4, Kind.YARD);
    }

    public void container(int x, int z, BlockState color) {
        box(x, 0, z - 1, x + 5, 2, z + 1, color);
        air(x, 0, z, x + 5, 1, z);
        set(x + 2, 2, z, t.light());
        occupy(x, z - 1, x + 5, z + 1);
        post(x - 1, 0, z + 2, Kind.YARD);
    }

    public void fuelTank(int cx, int cz) {
        for (int y = 0; y <= 5; y++) {
            BlockState s = (y == 1 || y == 3) ? Blocks.RED_CONCRETE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState();
            if (y == 5) {
                s = Blocks.IRON_BLOCK.defaultBlockState();
            }
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.sqrt(x * x + z * z) <= 2.3) {
                        set(cx + x, y, cz + z, s);
                    }
                }
            }
        }
        occupy(cx - 2, cz - 2, cx + 2, cz + 2);
        post(cx, 0, cz + 4, Kind.YARD);
    }

    public void sandbags(int x1, int z1, int x2, int z2) {
        fill(x1, 0, z1, x2, 1, z2, Blocks.PACKED_MUD.defaultBlockState());
    }

    public void dish(int cx, int cz) {
        fill(cx, 0, cz, cx, 4, cz, Blocks.IRON_BLOCK.defaultBlockState());
        ring(cx, cz, 5, 0, 3.2, Blocks.WHITE_CONCRETE.defaultBlockState());
        set(cx, 5, cz, Blocks.IRON_BLOCK.defaultBlockState());
        ring(cx, cz, 6, 3.0, 3.4, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        fill(cx, 6, cz, cx, 8, cz, Blocks.LIGHTNING_ROD.defaultBlockState());
        occupy(cx - 3, cz - 3, cx + 3, cz + 3);
    }
}
