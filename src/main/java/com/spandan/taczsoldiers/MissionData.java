package com.spandan.taczsoldiers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

public class MissionData {
    public enum Phase { ACTIVE, SECURED, EXTRACTING }

    public UUID player;
    public String name = "";
    public Difficulty difficulty = Difficulty.EASY;
    public MissionType type = MissionType.ASSAULT;
    public int baseId;
    public long expireTick;
    public Phase phase = Phase.ACTIVE;
    public boolean engaged;
    public boolean standDownSent;
    @Nullable
    public BlockPos extractPos;

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Player", player);
        tag.putString("Name", name);
        tag.putInt("Difficulty", difficulty.ordinal());
        tag.putInt("Type", type.ordinal());
        tag.putInt("Base", baseId);
        tag.putLong("Expire", expireTick);
        tag.putInt("Phase", phase.ordinal());
        tag.putBoolean("Engaged", engaged);
        tag.putBoolean("StandDown", standDownSent);
        if (extractPos != null) {
            tag.putLong("Extract", extractPos.asLong());
        }
        return tag;
    }

    public static MissionData load(CompoundTag tag) {
        MissionData m = new MissionData();
        m.player = tag.getUUID("Player");
        m.name = tag.getString("Name");
        m.difficulty = Difficulty.values()[Math.min(tag.getInt("Difficulty"), Difficulty.values().length - 1)];
        m.type = MissionType.values()[Math.min(tag.getInt("Type"), MissionType.values().length - 1)];
        m.baseId = tag.getInt("Base");
        m.expireTick = tag.getLong("Expire");
        m.phase = Phase.values()[Math.min(tag.getInt("Phase"), Phase.values().length - 1)];
        m.engaged = tag.getBoolean("Engaged");
        m.standDownSent = tag.getBoolean("StandDown");
        if (tag.contains("Extract")) {
            m.extractPos = BlockPos.of(tag.getLong("Extract"));
        }
        return m;
    }
}
