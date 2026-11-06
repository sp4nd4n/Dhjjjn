package com.spandan.taczsoldiers.world;

import com.spandan.taczsoldiers.BaseInfo;
import com.spandan.taczsoldiers.SoldierConfig;
import com.spandan.taczsoldiers.SoldierEntity;
import com.spandan.taczsoldiers.SoldierRole;
import com.spandan.taczsoldiers.TaczSoldiers;
import com.spandan.taczsoldiers.world.BaseBuilder.Kind;
import com.spandan.taczsoldiers.world.BaseBuilder.Post;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.List;

/** Decides how many soldiers guard a base and where each one stands. */
public final class Garrison {
    private Garrison() {
    }

    private static SoldierRole[] rolesFor(Kind kind) {
        return switch (kind) {
            case TOWER -> new SoldierRole[]{SoldierRole.SNIPER, SoldierRole.SNIPER, SoldierRole.SNIPER,
                    SoldierRole.ROCKETGUNNER, SoldierRole.RIFLEMAN};
            case WALL -> new SoldierRole[]{SoldierRole.RIFLEMAN, SoldierRole.RIFLEMAN, SoldierRole.RIFLEMAN,
                    SoldierRole.GUNNER, SoldierRole.SNIPER};
            case GATE -> new SoldierRole[]{SoldierRole.GUNNER, SoldierRole.GUNNER, SoldierRole.RIFLEMAN,
                    SoldierRole.RIFLEMAN, SoldierRole.BREACHER};
            case ROOF -> new SoldierRole[]{SoldierRole.SNIPER, SoldierRole.ROCKETGUNNER, SoldierRole.GUNNER};
            case VAULT -> new SoldierRole[]{SoldierRole.OFFICER, SoldierRole.OPERATOR, SoldierRole.OPERATOR,
                    SoldierRole.GUNNER};
            case INSIDE -> new SoldierRole[]{SoldierRole.OPERATOR, SoldierRole.OPERATOR, SoldierRole.RIFLEMAN,
                    SoldierRole.BREACHER, SoldierRole.MEDIC};
            default -> new SoldierRole[]{SoldierRole.RIFLEMAN, SoldierRole.RIFLEMAN, SoldierRole.OPERATOR,
                    SoldierRole.BREACHER, SoldierRole.ROCKETGUNNER, SoldierRole.MEDIC};
        };
    }

    private static int radiusFor(Kind kind) {
        return switch (kind) {
            case TOWER -> 3;
            case ROOF -> 4;
            case WALL -> 6;
            case VAULT, INSIDE, GATE -> 8;
            default -> 14;
        };
    }

    /** More soldiers for bigger bases and more valuable secrets. */
    public static int count(BaseInfo info, RandomSource rnd) {
        int n = 4 + info.type.sizeClass * 6 + info.item.value * 3 + rnd.nextInt(5);
        return Math.min(n, SoldierConfig.MAX_GARRISON.get());
    }

    public static int spawn(ServerLevel level, BaseInfo info, List<Post> posts) {
        if (posts.isEmpty()) {
            return 0;
        }
        RandomSource rnd = level.getRandom();
        int wanted = count(info, rnd);
        int spawned = 0;
        for (int i = 0; i < wanted; i++) {
            Post post = posts.get(rnd.nextInt(posts.size()));
            SoldierRole[] pool = rolesFor(post.kind());
            SoldierRole role = pool[rnd.nextInt(pool.length)];
            BlockPos p = post.pos();
            double x = p.getX() + 0.5;
            double z = p.getZ() + 0.5;
            if (!level.noCollision(TaczSoldiers.SOLDIER.get().getAABB(x, p.getY(), z))) {
                continue;
            }
            SoldierEntity soldier = SoldierEntity.spawnGuard(level, role, x, p.getY(), z,
                    rnd.nextFloat() * 360.0F, p, radiusFor(post.kind()), info.id);
            if (soldier != null) {
                spawned++;
            }
        }
        return spawned;
    }
}
