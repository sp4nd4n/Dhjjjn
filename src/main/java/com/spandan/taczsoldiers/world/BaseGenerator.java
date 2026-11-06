package com.spandan.taczsoldiers.world;

import com.spandan.taczsoldiers.BaseInfo;
import com.spandan.taczsoldiers.BaseType;
import com.spandan.taczsoldiers.ConfidentialType;
import com.spandan.taczsoldiers.SoldiersData;
import com.spandan.taczsoldiers.TaczSoldiers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;

/** Finds a site, levels the ground, builds a base, fills it with guards and records it. */
public final class BaseGenerator {
    private static final int FLAGS = 2 | 16;

    private BaseGenerator() {
    }

    private static int weightedValue(RandomSource rnd) {
        int roll = rnd.nextInt(100);
        if (roll < 25) {
            return 1;
        }
        if (roll < 50) {
            return 2;
        }
        if (roll < 75) {
            return 3;
        }
        return roll < 90 ? 4 : 5;
    }

    private static BaseType pickType(int value, RandomSource rnd) {
        return switch (value) {
            case 1 -> BaseType.OUTPOST;
            case 2 -> rnd.nextInt(10) < 6 ? BaseType.OUTPOST : BaseType.COMPOUND;
            case 3 -> BaseType.COMPOUND;
            case 4 -> rnd.nextInt(10) < 7 ? BaseType.COMPOUND : BaseType.FACILITY;
            default -> BaseType.FACILITY;
        };
    }

    @Nullable
    private static BlockPos findSite(ServerLevel level, BlockPos near, int minD, int maxD, int r) {
        RandomSource rnd = level.getRandom();
        int[][] samples = {{0, 0}, {r, r}, {-r, r}, {r, -r}, {-r, -r}, {r, 0}, {-r, 0}, {0, r}, {0, -r}};
        for (int attempt = 0; attempt < 18; attempt++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            int dist = minD + rnd.nextInt(Math.max(1, maxD - minD + 1));
            int x = near.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = near.getZ() + (int) Math.round(Math.sin(angle) * dist);

            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            long sum = 0;
            boolean ok = true;
            for (int[] s : samples) {
                int sx = x + s[0];
                int sz = z + s[1];
                level.getChunk(sx >> 4, sz >> 4);
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
                BlockState top = level.getBlockState(new BlockPos(sx, h - 1, sz));
                if (!top.getFluidState().isEmpty()) {
                    ok = false;
                    break;
                }
                min = Math.min(min, h);
                max = Math.max(max, h);
                sum += h;
            }
            if (!ok || max - min > 16) {
                continue;
            }
            int avg = (int) Math.round(sum / (double) samples.length);
            if (avg <= level.getSeaLevel() + 1) {
                continue;
            }
            BlockPos center = new BlockPos(x, avg, z);
            if (level.getBiome(center).is(BiomeTags.IS_OCEAN) || level.getBiome(center).is(BiomeTags.IS_RIVER)) {
                continue;
            }
            return center;
        }
        return null;
    }

    private static void flatten(ServerLevel level, BlockPos c, int radius, int baseY, int clearHeight, BlockState apron) {
        int minCx = (c.getX() - radius) >> 4;
        int maxCx = (c.getX() + radius) >> 4;
        int minCz = (c.getZ() - radius) >> 4;
        int maxCz = (c.getZ() + radius) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                level.getChunk(cx, cz);
            }
        }
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int wx = c.getX() + x;
                int wz = c.getZ() + z;
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                for (int y = ground; y < baseY; y++) {
                    level.setBlock(new BlockPos(wx, y, wz), dirt, FLAGS);
                }
                int top = Math.max(ground + 12, baseY + clearHeight);
                for (int y = baseY; y <= top; y++) {
                    BlockPos p = new BlockPos(wx, y, wz);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, air, FLAGS);
                    }
                }
                level.setBlock(new BlockPos(wx, baseY - 1, wz), apron, FLAGS);
            }
        }
    }

    @Nullable
    public static BaseInfo generate(ServerLevel level, SoldiersData data, BlockPos near, int minD, int maxD,
                                    @Nullable Integer forceValue) {
        RandomSource rnd = level.getRandom();
        int value = forceValue != null ? Math.max(1, Math.min(5, forceValue)) : weightedValue(rnd);
        BaseType type = pickType(value, rnd);
        int r = type.minRadius + rnd.nextInt(type.maxRadius - type.minRadius + 1);

        BlockPos site = findSite(level, near, minD, maxD, r);
        if (site == null) {
            TaczSoldiers.LOGGER.warn("[TaCZ Soldiers] Could not find a flat dry site for a base near {}", near);
            return null;
        }

        ConfidentialType item = ConfidentialType.byValue(value);
        int id = data.nextBaseId++;
        Theme theme = Theme.pick(rnd, level.getBiome(site).value().getBaseTemperature());
        int clearHeight = type == BaseType.OUTPOST ? 16 : type == BaseType.COMPOUND ? 26 : 28;

        flatten(level, site, r + 6, site.getY(), clearHeight, theme.ground());

        BaseBuilder builder = new BaseBuilder(level, site, Rotation.getRandom(rnd), theme);
        switch (type) {
            case OUTPOST -> Structures.outpost(builder, r, item, id);
            case COMPOUND -> Structures.compound(builder, r, false, item, id);
            default -> Structures.compound(builder, r, true, item, id);
        }

        BaseInfo info = new BaseInfo();
        info.id = id;
        info.center = site;
        info.radius = r;
        info.type = type;
        info.item = item;
        info.state = BaseInfo.State.AVAILABLE;
        info.soldiers = Garrison.spawn(level, info, builder.posts);
        data.bases.add(info);
        data.setDirty();

        TaczSoldiers.LOGGER.info("[TaCZ Soldiers] Generated {} #{} at {} with {} soldiers (secret: {})",
                type.displayName, id, site, info.soldiers, item.displayName);
        return info;
    }
}
