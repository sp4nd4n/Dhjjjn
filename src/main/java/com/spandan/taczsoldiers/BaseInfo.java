package com.spandan.taczsoldiers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

public class BaseInfo {
    public enum State { AVAILABLE, ASSIGNED, DONE }

    public int id;
    public BlockPos center = BlockPos.ZERO;
    public int radius;
    public BaseType type = BaseType.OUTPOST;
    public ConfidentialType item = ConfidentialType.DOCUMENTS;
    public State state = State.AVAILABLE;
    @Nullable
    public UUID assignee;
    public boolean garrisonCleared;
    public int soldiers;

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putLong("Center", center.asLong());
        tag.putInt("Radius", radius);
        tag.putInt("Type", type.ordinal());
        tag.putInt("Item", item.ordinal());
        tag.putInt("State", state.ordinal());
        if (assignee != null) {
            tag.putUUID("Assignee", assignee);
        }
        tag.putBoolean("Cleared", garrisonCleared);
        tag.putInt("Soldiers", soldiers);
        return tag;
    }

    public static BaseInfo load(CompoundTag tag) {
        BaseInfo b = new BaseInfo();
        b.id = tag.getInt("Id");
        b.center = BlockPos.of(tag.getLong("Center"));
        b.radius = tag.getInt("Radius");
        b.type = BaseType.values()[Math.min(tag.getInt("Type"), BaseType.values().length - 1)];
        b.item = ConfidentialType.values()[Math.min(tag.getInt("Item"), ConfidentialType.values().length - 1)];
        b.state = State.values()[Math.min(tag.getInt("State"), State.values().length - 1)];
        if (tag.hasUUID("Assignee")) {
            b.assignee = tag.getUUID("Assignee");
        }
        b.garrisonCleared = tag.getBoolean("Cleared");
        b.soldiers = tag.getInt("Soldiers");
        return b;
    }
}
