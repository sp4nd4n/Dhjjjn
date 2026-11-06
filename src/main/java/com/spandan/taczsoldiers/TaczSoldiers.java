package com.spandan.taczsoldiers;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;

@Mod(TaczSoldiers.MODID)
public class TaczSoldiers {
    public static final String MODID = "taczsoldiers";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<EntityType<SoldierEntity>> SOLDIER = ENTITIES.register("soldier",
            () -> EntityType.Builder.of(SoldierEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(MODID + ":soldier"));

    public static final RegistryObject<EntityType<GrenadeEntity>> GRENADE_ENTITY = ENTITIES.register("frag_grenade",
            () -> EntityType.Builder.<GrenadeEntity>of(GrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(MODID + ":frag_grenade"));

    public static final RegistryObject<Item> GRENADE = ITEMS.register("frag_grenade",
            () -> new FragGrenadeItem(new Item.Properties().stacksTo(16)));

    public static final Map<ConfidentialType, RegistryObject<Item>> CONFIDENTIAL = new EnumMap<>(ConfidentialType.class);

    public static final Map<SoldierRole, RegistryObject<Item>> EGGS = new EnumMap<>(SoldierRole.class);

    static {
        for (ConfidentialType type : ConfidentialType.values()) {
            CONFIDENTIAL.put(type, ITEMS.register(type.id, () -> new ConfidentialItem(type)));
        }
        for (SoldierRole role : SoldierRole.values()) {
            EGGS.put(role, ITEMS.register("spawn_" + role.id,
                    () -> new SoldierEggItem(role, new Item.Properties())));
        }
    }

    public TaczSoldiers() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(this::onAttributes);
        modBus.addListener(this::onCreativeTab);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SoldierConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(SOLDIER.get(), SoldierEntity.createAttributes().build());
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            EGGS.values().forEach(egg -> event.accept(egg.get()));
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(GRENADE.get());
            CONFIDENTIAL.values().forEach(item -> event.accept(item.get()));
        }
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        SoldierCommand.register(event.getDispatcher());
    }
}
