package com.spandan.taczsoldiers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SoldierEggItem extends Item {
    private final SoldierRole role;

    public SoldierEggItem(SoldierRole role, Properties properties) {
        super(properties);
        this.role = role;
    }

    public SoldierRole getRole() {
        return role;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked : clicked.relative(face);

        Vec3 at = Vec3.atBottomCenterOf(spawnPos);
        SoldierEntity soldier = SoldierEntity.spawn(server, role, at.x, at.y, at.z, context.getRotation() + 180.0F);
        if (soldier != null && (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
