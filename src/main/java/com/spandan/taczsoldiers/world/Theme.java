package com.spandan.taczsoldiers.world;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Block palette of a base. */
public record Theme(BlockState wall, BlockState wall2, BlockState floor, BlockState floor2, BlockState roof,
                    BlockState trim, BlockState glass, BlockState ground, BlockState ground2,
                    BlockState accent, BlockState light) {

    private static BlockState s(net.minecraft.world.level.block.Block b) {
        return b.defaultBlockState();
    }

    public static Theme pick(RandomSource rnd, float temperature) {
        int k;
        if (temperature > 1.0F) {
            k = 1;
        } else if (temperature < 0.15F) {
            k = 2;
        } else {
            k = rnd.nextInt(4) == 0 ? 3 : 0;
        }
        return switch (k) {
            case 1 -> new Theme(s(Blocks.SMOOTH_SANDSTONE), s(Blocks.CUT_SANDSTONE), s(Blocks.SMOOTH_SANDSTONE),
                    s(Blocks.LIGHT_GRAY_CONCRETE), s(Blocks.TERRACOTTA), s(Blocks.WHITE_TERRACOTTA),
                    s(Blocks.GRAY_STAINED_GLASS), s(Blocks.COARSE_DIRT), s(Blocks.GRAVEL),
                    s(Blocks.ORANGE_TERRACOTTA), s(Blocks.SEA_LANTERN));
            case 2 -> new Theme(s(Blocks.WHITE_CONCRETE), s(Blocks.LIGHT_GRAY_CONCRETE), s(Blocks.SMOOTH_QUARTZ),
                    s(Blocks.LIGHT_GRAY_CONCRETE), s(Blocks.GRAY_CONCRETE), s(Blocks.IRON_BLOCK),
                    s(Blocks.LIGHT_BLUE_STAINED_GLASS), s(Blocks.SNOW_BLOCK), s(Blocks.GRAVEL),
                    s(Blocks.RED_CONCRETE), s(Blocks.SEA_LANTERN));
            case 3 -> new Theme(s(Blocks.DEEPSLATE_BRICKS), s(Blocks.POLISHED_BLACKSTONE), s(Blocks.POLISHED_DEEPSLATE),
                    s(Blocks.BLACK_CONCRETE), s(Blocks.BLACKSTONE), s(Blocks.IRON_BLOCK),
                    s(Blocks.BLACK_STAINED_GLASS), s(Blocks.GRAVEL), s(Blocks.COBBLED_DEEPSLATE),
                    s(Blocks.YELLOW_CONCRETE), s(Blocks.SEA_LANTERN));
            default -> new Theme(s(Blocks.STONE_BRICKS), s(Blocks.POLISHED_ANDESITE), s(Blocks.SMOOTH_STONE),
                    s(Blocks.GRAY_CONCRETE), s(Blocks.GREEN_TERRACOTTA), s(Blocks.IRON_BLOCK),
                    s(Blocks.GRAY_STAINED_GLASS), s(Blocks.COARSE_DIRT), s(Blocks.GRAVEL),
                    s(Blocks.YELLOW_CONCRETE), s(Blocks.SEA_LANTERN));
        };
    }
}
