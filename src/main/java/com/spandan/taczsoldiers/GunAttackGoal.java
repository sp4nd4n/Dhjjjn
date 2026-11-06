package com.spandan.taczsoldiers;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/** Moves, takes cover, throws grenades, aims and fires the soldier's TaCZ gun at its target. */
public class GunAttackGoal extends Goal {
    private final SoldierEntity mob;

    private int seeTime;
    private int lostSight;
    private int burstLeft;
    private int pause;
    private int reload;
    private int repath;
    private int strafeTime;
    private int coverSearch;
    private boolean strafeRight = true;
    private boolean aiming;
    @Nullable
    private Vec3 cover;

    public GunAttackGoal(SoldierEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.hasGun();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        mob.setAggressive(true);
        seeTime = 0;
        lostSight = 0;
        burstLeft = 0;
        pause = 0;
        reload = 0;
        cover = null;
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        setAiming(false);
        mob.getNavigation().stop();
        seeTime = 0;
        cover = null;
    }

    private void setAiming(boolean value) {
        if (aiming != value) {
            aiming = value;
            TaczBridge.aim(mob, value);
        }
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        SoldierRole role = mob.getRole();
        double dist = mob.distanceTo(target);
        boolean sees = mob.getSensing().hasLineOfSight(target);
        if (sees) {
            seeTime++;
            lostSight = 0;
        } else {
            seeTime = 0;
            lostSight++;
        }

        mob.getLookControl().setLookAt(target, 40.0F, 40.0F);

        boolean leashed = false;
        if (mob.isGuard() && mob.getHome() != null) {
            BlockPos home = mob.getHome();
            double homeDist = Math.sqrt(target.distanceToSqr(home.getX(), home.getY(), home.getZ()));
            leashed = homeDist > mob.getGuardRadius() + 18;
        }

        // ---- grenades ----
        if (mob.grenadeCooldown > 0) {
            mob.grenadeCooldown--;
        }
        if (mob.getGrenades() > 0 && mob.grenadeCooldown <= 0 && dist > 8 && dist < 26) {
            boolean hiding = !sees && lostSight > 20 && lostSight < 140;
            boolean pressure = sees && dist > 10 && mob.getRandom().nextInt(60) == 0;
            if ((hiding || pressure) && mob.throwGrenade(target)) {
                mob.grenadeCooldown = 160 + mob.getRandom().nextInt(160);
                pause = Math.max(pause, 15);
            }
        }

        // ---- movement ----
        boolean wantsCover = reload > 0 || mob.getHealth() < mob.getMaxHealth() * 0.5F;
        boolean usingCover = false;
        if (wantsCover && dist > 5) {
            if (cover == null && --coverSearch <= 0) {
                cover = findCover(target);
                coverSearch = 40;
            }
            if (cover != null) {
                usingCover = true;
                if (mob.position().distanceToSqr(cover) > 2.5) {
                    if (--repath <= 0) {
                        repath = 10;
                        mob.getNavigation().moveTo(cover.x, cover.y, cover.z, 1.25);
                    }
                } else {
                    mob.getNavigation().stop();
                }
            }
        } else {
            cover = null;
        }

        if (!usingCover) {
            if (sees && dist < role.retreatRange && !leashed) {
                if (--repath <= 0) {
                    repath = 10;
                    Vec3 away = DefaultRandomPos.getPosAway(mob, 12, 6, target.position());
                    if (away != null) {
                        mob.getNavigation().moveTo(away.x, away.y, away.z, 1.1);
                    }
                }
            } else if ((!sees || dist > role.preferredRange) && !leashed) {
                if (--repath <= 0) {
                    repath = 10;
                    mob.getNavigation().moveTo(target, 1.0);
                }
            } else {
                mob.getNavigation().stop();
                if (role.strafes && sees) {
                    if (--strafeTime <= 0) {
                        strafeTime = 20 + mob.getRandom().nextInt(25);
                        strafeRight = !strafeRight;
                    }
                    mob.getMoveControl().strafe(0.0F, strafeRight ? 0.5F : -0.5F);
                    mob.lookAt(target, 30.0F, 30.0F);
                }
            }
        }

        // ---- shooting ----
        if (reload > 0) {
            reload--;
            if (reload == 0) {
                mob.refillMagazine();
            }
            return;
        }

        if (!sees || dist > role.engageRange || seeTime < role.reactionTicks) {
            setAiming(false);
            return;
        }
        setAiming(true);

        if (pause > 0) {
            pause--;
            return;
        }

        if (mob.ammoLeft() <= 0) {
            startReload(role);
            return;
        }

        if (burstLeft <= 0) {
            burstLeft = Mth.nextInt(mob.getRandom(), role.burstMin, role.burstMax);
        }

        Vec3 from = mob.getEyePosition();
        double dx = target.getX() - from.x;
        double dy = target.getY() + target.getBbHeight() * 0.6 - from.y;
        double dz = target.getZ() - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horizontal) * (180.0 / Math.PI)));
        yaw += (mob.getRandom().nextFloat() - 0.5F) * 2.0F * role.inaccuracy;
        pitch += (mob.getRandom().nextFloat() - 0.5F) * 2.0F * role.inaccuracy;

        String result = TaczBridge.shoot(mob, pitch, yaw);
        switch (result) {
            case "SUCCESS" -> {
                burstLeft--;
                if (burstLeft <= 0) {
                    pause = Mth.nextInt(mob.getRandom(), role.pauseMin, role.pauseMax);
                }
            }
            case "NEED_BOLT" -> TaczBridge.bolt(mob);
            case "NO_AMMO" -> startReload(role);
            default -> {
                // COOL_DOWN, IS_DRAWING, etc. - just try again next tick
            }
        }
    }

    private void startReload(SoldierRole role) {
        reload = role.reloadTicks;
        burstLeft = 0;
        setAiming(false);
    }

    /** Finds a nearby standing spot that the target cannot see (walls, crates, sandbags...). */
    @Nullable
    private Vec3 findCover(LivingEntity target) {
        Level level = mob.level();
        Vec3 eye = target.getEyePosition();
        BlockPos origin = mob.blockPosition();
        BlockPos home = mob.getHome();
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < 18; i++) {
            BlockPos column = origin.offset(mob.getRandom().nextInt(17) - 8, 0, mob.getRandom().nextInt(17) - 8);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos p = column.offset(0, dy, 0);
                if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()
                        || !level.getBlockState(p.below()).blocksMotion()) {
                    continue;
                }
                if (home != null && home.distSqr(p) > (double) (mob.getGuardRadius() * mob.getGuardRadius())) {
                    continue;
                }
                Vec3 head = new Vec3(p.getX() + 0.5, p.getY() + mob.getEyeHeight(), p.getZ() + 0.5);
                HitResult hit = level.clip(new ClipContext(eye, head, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, target));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    double d = origin.distSqr(p);
                    if (d < bestDist) {
                        bestDist = d;
                        best = new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                    }
                    break;
                }
            }
        }
        return best;
    }
}
