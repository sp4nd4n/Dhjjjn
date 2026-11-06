package com.spandan.taczsoldiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** World-saved list of bases and active missions. */
public class SoldiersData extends SavedData {
    private static final String NAME = "taczsoldiers_data";

    public final List<BaseInfo> bases = new ArrayList<>();
    public final Map<UUID, MissionData> missions = new HashMap<>();
    public int nextBaseId = 1;
    public boolean initialDone;

    public static SoldiersData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SoldiersData::load, SoldiersData::new, NAME);
    }

    @Nullable
    public BaseInfo base(int id) {
        for (BaseInfo b : bases) {
            if (b.id == id) {
                return b;
            }
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag baseList = new ListTag();
        for (BaseInfo b : bases) {
            baseList.add(b.save());
        }
        tag.put("Bases", baseList);
        ListTag missionList = new ListTag();
        for (MissionData m : missions.values()) {
            missionList.add(m.save());
        }
        tag.put("Missions", missionList);
        tag.putInt("NextBaseId", nextBaseId);
        tag.putBoolean("InitialDone", initialDone);
        return tag;
    }

    public static SoldiersData load(CompoundTag tag) {
        SoldiersData data = new SoldiersData();
        for (Tag t : tag.getList("Bases", Tag.TAG_COMPOUND)) {
            data.bases.add(BaseInfo.load((CompoundTag) t));
        }
        for (Tag t : tag.getList("Missions", Tag.TAG_COMPOUND)) {
            MissionData m = MissionData.load((CompoundTag) t);
            data.missions.put(m.player, m);
        }
        data.nextBaseId = Math.max(1, tag.getInt("NextBaseId"));
        data.initialDone = tag.getBoolean("InitialDone");
        return data;
    }
}
