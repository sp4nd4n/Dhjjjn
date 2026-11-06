package com.spandan.taczsoldiers;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** A frag grenade: bounces, then explodes after ~3 seconds. Hurts entities but never breaks blocks. */
public class GrenadeEntity extends ThrowableItemProjectile {
    private int fuse = 60;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
    }

    public GrenadeEntity(Level level, LivingEntity owner) {
        super(TaczSoldiers.GRENADE_ENTITY.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return TaczSoldiers.GRENADE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            fuse--;
            if (fuse <= 0) {
                explode();
            } else if (fuse % 4 == 0 && this.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        Vec3 v = this.getDeltaMovement();
        Direction.Axis axis = hit.getDirection().getAxis();
        if (axis == Direction.Axis.X) {
            v = new Vec3(-v.x * 0.5, v.y * 0.8, v.z * 0.8);
        } else if (axis == Direction.Axis.Y) {
            v = new Vec3(v.x * 0.7, -v.y * 0.4, v.z * 0.7);
        } else {
            v = new Vec3(v.x * 0.8, v.y * 0.8, -v.z * 0.5);
        }
        this.setDeltaMovement(v);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!this.level().isClientSide) {
            explode();
        }
    }

    private void explode() {
        if (this.isRemoved()) {
            return;
        }
        this.level().explode(this, getX(), getY(), getZ(), 2.8F, Level.ExplosionInteraction.NONE);
        this.discard();
    }
}
