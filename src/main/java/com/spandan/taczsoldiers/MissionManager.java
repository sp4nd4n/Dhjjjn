package com.spandan.taczsoldiers;

import com.spandan.taczsoldiers.world.BaseGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Everything about the mission flow: /mission, /call, /done, drops, time limit, failure. */
public final class MissionManager {
    private static final String[] ADJECTIVES = {"Silent", "Crimson", "Iron", "Midnight", "Broken", "Shadow", "Golden",
            "Frozen", "Burning", "Hollow", "Scarlet", "Phantom"};
    private static final String[] NOUNS = {"Falcon", "Viper", "Thunder", "Anvil", "Dagger", "Horizon", "Serpent",
            "Lantern", "Hammer", "Raven", "Eclipse", "Tempest"};

    private static int ticks;
    private static int waited;
    private static int failedGenerations;

    private MissionManager() {
    }

    // ---- helpers -----------------------------------------------------------------------

    private static void say(ServerPlayer p, String text, ChatFormatting color) {
        p.sendSystemMessage(Component.literal(text).withStyle(color));
    }

    private static void title(ServerPlayer p, String title, ChatFormatting titleColor, String subtitle) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(titleColor, ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle).withStyle(ChatFormatting.WHITE)));
    }

    private static double horizontal(ServerPlayer p, BlockPos pos) {
        return Math.hypot(p.getX() - pos.getX(), p.getZ() - pos.getZ());
    }

    private static boolean hasItem(ServerPlayer p, int baseId) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (ConfidentialItem.isFor(p.getInventory().getItem(i), baseId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGarrisonCleared(int baseId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        BaseInfo base = SoldiersData.get(server).base(baseId);
        return base != null && base.garrisonCleared;
    }

    // ---- welcome -----------------------------------------------------------------------

    public static void welcome(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        CompoundTag persisted = root.getCompound("PlayerPersisted");
        if (persisted.getBoolean("taczsoldiers_welcomed")) {
            return;
        }
        persisted.putBoolean("taczsoldiers_welcomed", true);
        root.put("PlayerPersisted", persisted);

        say(p, "=== TaCZ Soldiers ===", ChatFormatting.GOLD);
        say(p, "Military bases are hidden across this world, guarded by armed soldiers.", ChatFormatting.YELLOW);
        say(p, "Type /mission to receive a random mission. A supply drop will fall from the sky next to you.", ChatFormatting.YELLOW);
        say(p, "Steal the secret, type /call for extraction, put it in the crate and type /done.", ChatFormatting.GRAY);
        say(p, "Tip: a full leather armor set dyed green fools the guards.", ChatFormatting.GRAY);
    }

    // ---- /mission ----------------------------------------------------------------------

    public static int start(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ServerLevel level = server.overworld();
        if (p.level() != level) {
            say(p, "Missions are only available in the Overworld.", ChatFormatting.RED);
            return 0;
        }
        SoldiersData data = SoldiersData.get(server);
        MissionData existing = data.missions.get(p.getUUID());
        if (existing != null) {
            status(p, existing, data, level);
            return 1;
        }

        RandomSource rnd = level.getRandom();
        List<BaseInfo> available = new ArrayList<>();
        for (BaseInfo b : data.bases) {
            if (b.state == BaseInfo.State.AVAILABLE && horizontal(p, b.center) <= 2500) {
                available.add(b);
            }
        }
        BaseInfo base;
        if (!available.isEmpty()) {
            base = available.get(rnd.nextInt(available.size()));
        } else {
            say(p, "Scouting a new target...", ChatFormatting.GRAY);
            base = BaseGenerator.generate(level, data, p.blockPosition(), 300, 700, null);
        }
        if (base == null) {
            say(p, "Command could not find a suitable target. Try again in a moment.", ChatFormatting.RED);
            return 0;
        }

        MissionData m = new MissionData();
        m.player = p.getUUID();
        m.name = "Operation " + ADJECTIVES[rnd.nextInt(ADJECTIVES.length)] + " " + NOUNS[rnd.nextInt(NOUNS.length)];
        m.difficulty = Difficulty.forValue(base.item.value);
        m.type = MissionType.values()[rnd.nextInt(MissionType.values().length)];
        m.baseId = base.id;
        m.expireTick = level.getGameTime() + SoldierConfig.MISSION_TIME_TICKS.get();
        base.state = BaseInfo.State.ASSIGNED;
        base.assignee = p.getUUID();
        base.garrisonCleared = false;
        data.missions.put(p.getUUID(), m);
        data.setDirty();

        title(p, m.name.toUpperCase(), m.difficulty.color, "Difficulty: " + m.difficulty.displayName
                + "  |  " + m.type.displayName);
        say(p, "=== " + m.name + " ===", ChatFormatting.GOLD);
        say(p, "Type: " + m.type.displayName + " - " + m.type.blurb, ChatFormatting.YELLOW);
        say(p, "Difficulty: " + m.difficulty.displayName, m.difficulty.color);
        say(p, "Objective: steal the " + base.item.displayName + " from a " + base.type.displayName + ".", ChatFormatting.WHITE);
        say(p, "Coordinates are written in your book and quill. You have one Minecraft day.", ChatFormatting.GRAY);
        say(p, "A supply drop is falling next to you!", ChatFormatting.AQUA);

        giveBook(p, m, base);
        if (!TaczBridge.hasGuns()) {
            say(p, "Warning: no TaCZ guns were found, the crate will only hold armor and grenades.", ChatFormatting.RED);
        }
        SupplyDrop.spawn(level, p, false, Loadout.build(m.type, m.difficulty, rnd));
        return 1;
    }

    private static void giveBook(ServerPlayer p, MissionData m, BaseInfo base) {
        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(m.name + "\nDifficulty: " + m.difficulty.displayName
                + "\nTarget: " + base.type.displayName
                + "\nX: " + base.center.getX() + "\nY: " + base.center.getY() + "\nZ: " + base.center.getZ()
                + "\nObjective: steal the " + base.item.displayName + "."));
        pages.add(StringTag.valueOf("Reach the base and take the secret from the vault. Kill the guards, or "
                + "sneak out and run. Then type /call for extraction, put the secret in the crate and type /done. "
                + "You have one day. Dying fails the mission. A full green leather armor set fools the guards."));
        book.addTagElement("pages", pages);
        book.setHoverName(Component.literal("Mission Notes").withStyle(ChatFormatting.GOLD));
        if (!p.getInventory().add(book)) {
            p.drop(book, false);
        }
    }

    private static void status(ServerPlayer p, MissionData m, SoldiersData data, ServerLevel level) {
        BaseInfo base = data.base(m.baseId);
        long left = Math.max(0, m.expireTick - level.getGameTime()) / 20;
        say(p, "Active mission: " + m.name + " (" + m.difficulty.displayName + ")", ChatFormatting.GOLD);
        if (base != null) {
            say(p, "Objective: steal the " + base.item.displayName + " at X " + base.center.getX()
                    + " Z " + base.center.getZ(), ChatFormatting.YELLOW);
        }
        String hint = switch (m.phase) {
            case ACTIVE -> "Reach the base and take the secret.";
            case SECURED -> "Objective secured. Type /call when you are somewhere safe.";
            default -> "Put the secret in the extraction crate, then type /done.";
        };
        say(p, hint, ChatFormatting.GRAY);
        say(p, "Time left: " + (left / 60) + "m " + (left % 60) + "s", ChatFormatting.GRAY);
        say(p, "You can only run one mission at a time. Finish or fail this one first.", ChatFormatting.DARK_GRAY);
    }

    // ---- /call -------------------------------------------------------------------------

    public static int call(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ServerLevel level = server.overworld();
        SoldiersData data = SoldiersData.get(server);
        MissionData m = data.missions.get(p.getUUID());
        if (m == null) {
            say(p, "You have no active mission. Type /mission.", ChatFormatting.RED);
            return 0;
        }
        if (p.level() != level) {
            say(p, "Extraction can only be called in the Overworld.", ChatFormatting.RED);
            return 0;
        }
        if (m.phase == MissionData.Phase.EXTRACTING) {
            say(p, "Extraction is already on the way. Put the secret in the crate and type /done.", ChatFormatting.YELLOW);
            return 0;
        }
        if (m.phase == MissionData.Phase.ACTIVE || !hasItem(p, m.baseId)) {
            say(p, "You have not secured the objective yet.", ChatFormatting.RED);
            return 0;
        }
        m.extractPos = SupplyDrop.spawn(level, p, true, new ArrayList<>());
        m.phase = MissionData.Phase.EXTRACTING;
        data.setDirty();
        say(p, "Extraction crate inbound! Place the secret inside it, then type /done.", ChatFormatting.AQUA);
        return 1;
    }

    // ---- /done -------------------------------------------------------------------------

    public static int done(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ServerLevel level = server.overworld();
        SoldiersData data = SoldiersData.get(server);
        MissionData m = data.missions.get(p.getUUID());
        if (m == null) {
            say(p, "You have no active mission.", ChatFormatting.RED);
            return 0;
        }
        BaseInfo base = data.base(m.baseId);
        if (m.phase != MissionData.Phase.EXTRACTING || m.extractPos == null || base == null) {
            say(p, "Call extraction first with /call.", ChatFormatting.RED);
            return 0;
        }
        BlockPos pos = locateCrate(level, m.extractPos);
        if (pos == null) {
            say(p, "The crate has not landed yet.", ChatFormatting.YELLOW);
            return 0;
        }
        if (p.distanceToSqr(Vec3.atCenterOf(pos)) > 24 * 24) {
            say(p, "Stay within 24 blocks of the crate.", ChatFormatting.YELLOW);
            return 0;
        }
        BlockEntity be = level.getBlockEntity(pos);
        boolean found = false;
        if (be instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (ConfidentialItem.isFor(container.getItem(i), m.baseId)) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            say(p, "Place the " + base.item.displayName + " inside the extraction crate first.", ChatFormatting.RED);
            return 0;
        }

        ((Container) be).clearContent();
        level.removeBlock(pos, false);
        blast(level, pos);

        data.missions.remove(p.getUUID());
        base.state = BaseInfo.State.DONE;
        base.garrisonCleared = true;
        data.setDirty();
        title(p, "MISSION COMPLETE", ChatFormatting.GREEN, m.name);
        say(p, "Mission complete: " + m.name + ". The crate has been extracted. Well done, soldier.", ChatFormatting.GREEN);
        say(p, "Type /mission whenever you are ready for the next one.", ChatFormatting.GRAY);
        return 1;
    }

    /** A loud, bright explosion effect that cannot hurt anyone or break anything. */
    private static void blast(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, 0.9F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 80, 1.0, 1.0, 1.0, 0.12);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1, z, 40, 1.0, 1.0, 1.0, 0.05);
        level.sendParticles(ParticleTypes.FIREWORK, x, y + 1, z, 60, 1.5, 1.5, 1.5, 0.2);
    }

    // ---- failure -----------------------------------------------------------------------

    public static void onPlayerDeath(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) {
            return;
        }
        SoldiersData data = SoldiersData.get(server);
        MissionData m = data.missions.get(p.getUUID());
        if (m != null) {
            fail(data, m, p, "You died. The mission is lost.");
        }
    }

    private static void fail(SoldiersData data, MissionData m, ServerPlayer p, String reason) {
        data.missions.remove(m.player);
        BaseInfo base = data.base(m.baseId);
        if (base != null) {
            base.state = BaseInfo.State.DONE;
            base.garrisonCleared = true;
        }
        data.setDirty();
        if (p != null) {
            title(p, "MISSION FAILED", ChatFormatting.DARK_RED, m.name);
            say(p, "Mission failed: " + m.name + ". " + reason, ChatFormatting.RED);
            say(p, "Type /mission to try another.", ChatFormatting.GRAY);
        }
    }

    // ---- server tick -------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (level == null) {
            return;
        }
        ticks++;
        tickDrops(level);
        if (ticks % 20 == 0) {
            tickMissions(server, level);
        }
        if (ticks % 40 == 0) {
            tickInitialBases(server, level);
        }
    }

    private static void tickInitialBases(MinecraftServer server, ServerLevel level) {
        SoldiersData data = SoldiersData.get(server);
        if (data.initialDone || server.getPlayerCount() == 0) {
            return;
        }
        waited += 40;
        if (waited < 200) {
            return;
        }
        if (data.bases.size() >= SoldierConfig.INITIAL_BASES.get() || failedGenerations > 6) {
            data.initialDone = true;
            data.setDirty();
            return;
        }
        BaseInfo b = BaseGenerator.generate(level, data, level.getSharedSpawnPos(), 220, 800, null);
        if (b == null) {
            failedGenerations++;
        }
    }

    private static void tickMissions(MinecraftServer server, ServerLevel level) {
        SoldiersData data = SoldiersData.get(server);
        if (data.missions.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (MissionData m : new ArrayList<>(data.missions.values())) {
            BaseInfo base = data.base(m.baseId);
            ServerPlayer p = server.getPlayerList().getPlayer(m.player);
            if (base == null) {
                data.missions.remove(m.player);
                data.setDirty();
                continue;
            }
            if (now > m.expireTick) {
                fail(data, m, p, "Time ran out - the day is over.");
                continue;
            }
            if (p == null || p.level() != level) {
                continue;
            }
            double dist = horizontal(p, base.center);

            if (m.phase == MissionData.Phase.ACTIVE) {
                if (!m.engaged && dist < base.radius + 95) {
                    m.engaged = true;
                    data.setDirty();
                    title(p, "TARGET IN RANGE", ChatFormatting.GOLD, "Get ready - start the mission");
                    say(p, "You are close to the target. Guards patrol inside and outside.", ChatFormatting.YELLOW);
                    say(p, "Kill them all and take the secret, or slip away with it. Your call.", ChatFormatting.GRAY);
                }
                if (hasItem(p, base.id)) {
                    m.phase = MissionData.Phase.SECURED;
                    data.setDirty();
                    title(p, "OBJECTIVE SECURED", ChatFormatting.GREEN, "Get out, then type /call");
                    say(p, "You have the " + base.item.displayName + ". Escape and type /call for extraction.", ChatFormatting.GREEN);
                }
            } else if (m.phase == MissionData.Phase.SECURED) {
                if (!hasItem(p, base.id)) {
                    m.phase = MissionData.Phase.ACTIVE;
                    data.setDirty();
                } else if (!m.standDownSent && dist > base.radius + 85) {
                    m.standDownSent = true;
                    base.garrisonCleared = true;
                    data.setDirty();
                    say(p, "You broke contact. The garrison has stood down. Type /call when you are ready.", ChatFormatting.AQUA);
                }
            }
        }
    }

    // ---- drops -------------------------------------------------------------------------

    private static BlockPos locateCrate(ServerLevel level, BlockPos near) {
        for (int dy = 3; dy >= -4; dy--) {
            BlockPos p = near.offset(0, dy, 0);
            if (level.getBlockState(p).getBlock() instanceof ShulkerBoxBlock) {
                return p;
            }
        }
        return null;
    }

    private static void tickDrops(ServerLevel level) {
        Iterator<SupplyDrop.Tracked> it = SupplyDrop.TRACKED.iterator();
        while (it.hasNext()) {
            SupplyDrop.Tracked t = it.next();
            if (!t.landed) {
                Entity e = level.getEntity(t.entity);
                if (e != null && e.isAlive()) {
                    Vec3 motion = e.getDeltaMovement();
                    if (motion.y < -0.35) {
                        e.setDeltaMovement(motion.x, -0.35, motion.z); // parachute-slow descent
                    }
                    if (e.tickCount % 2 == 0) {
                        level.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 1.4, e.getZ(), 2, 0.3, 0.2, 0.3, 0.01);
                    }
                } else if (level.hasChunkAt(t.landing)) {
                    t.landed = true;
                    BlockPos crate = locateCrate(level, t.landing);
                    if (crate == null) {
                        it.remove();
                        continue;
                    }
                    t.landing = crate;
                    level.playSound(null, crate, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5F, 0.7F);
                    level.sendParticles(ParticleTypes.POOF, crate.getX() + 0.5, crate.getY() + 0.5, crate.getZ() + 0.5,
                            30, 0.6, 0.3, 0.6, 0.05);
                    if (t.extraction) {
                        MinecraftServer server = level.getServer();
                        MissionData m = SoldiersData.get(server).missions.get(t.player);
                        if (m != null) {
                            m.extractPos = crate;
                            SoldiersData.get(server).setDirty();
                        }
                    }
                }
            } else {
                if (!(level.getBlockState(t.landing).getBlock() instanceof ShulkerBoxBlock) || --t.signal <= 0) {
                    it.remove();
                } else if (t.signal % 10 == 0) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, t.landing.getX() + 0.5,
                            t.landing.getY() + 1.0, t.landing.getZ() + 0.5, 1, 0.1, 0, 0.1, 0.02);
                }
            }
        }
    }
}
