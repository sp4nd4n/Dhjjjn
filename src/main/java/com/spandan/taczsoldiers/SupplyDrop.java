package com.spandan.taczsoldiers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Air drops: a shulker-box crate that really falls out of the sky and lands next to the player. */
public final class SupplyDrop {
    public static final class Tracked {
        public UUID entity;
        public UUID player;
        public boolean extraction;
        public BlockPos landing;
        public boolean landed;
        public int signal = 1200;
    }

    public static final List<Tracked> TRACKED = new ArrayList<>();

    private SupplyDrop() {
    }

    /** Spawns the falling crate. Items that do not fit (27 slots) go straight to the player. Returns the landing spot. */
    public static BlockPos spawn(ServerLevel level, ServerPlayer player, boolean extraction, List<ItemStack> items) {
        RandomSource rnd = level.getRandom();
        BlockPos landing = null;
        for (int i = 0; i < 14 && landing == null; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            int dist = 3 + rnd.nextInt(3);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, h, z);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).blocksMotion()) {
                landing = p;
            }
        }
        if (landing == null) {
            landing = new BlockPos(player.getBlockX() + 3, player.getBlockY(), player.getBlockZ());
        }

        int startY = Math.min(landing.getY() + 90, level.getMaxBuildHeight() - 3);
        BlockPos start = new BlockPos(landing.getX(), startY, landing.getZ());
        BlockState state = extraction ? Blocks.YELLOW_SHULKER_BOX.defaultBlockState() : Blocks.RED_SHULKER_BOX.defaultBlockState();
        FallingBlockEntity falling = FallingBlockEntity.fall(level, start, state);

        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            if (i < 27) {
                contents.set(i, items.get(i));
            } else if (!player.getInventory().add(items.get(i))) {
                player.drop(items.get(i), false);
            }
        }
        CompoundTag data = new CompoundTag();
        ContainerHelper.saveAllItems(data, contents);
        data.putString("CustomName", Component.Serializer.toJson(
                Component.literal(extraction ? "Extraction Crate" : "Supply Drop")));
        falling.blockData = data;

        Tracked tracked = new Tracked();
        tracked.entity = falling.getUUID();
        tracked.player = player.getUUID();
        tracked.extraction = extraction;
        tracked.landing = landing;
        TRACKED.add(tracked);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 2.0F, 0.6F);
        return landing;
    }
}
