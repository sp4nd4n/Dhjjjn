package com.spandan.taczsoldiers.client;

import com.spandan.taczsoldiers.SoldierRole;
import com.spandan.taczsoldiers.TaczSoldiers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TaczSoldiers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TaczSoldiers.SOLDIER.get(), SoldierRenderer::new);
        event.registerEntityRenderer(TaczSoldiers.GRENADE_ENTITY.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerEggColors(RegisterColorHandlersEvent.Item event) {
        for (SoldierRole role : SoldierRole.values()) {
            Item egg = TaczSoldiers.EGGS.get(role).get();
            event.register((stack, tintIndex) -> tintIndex == 0 ? role.eggPrimary : role.eggSecondary, egg);
        }
    }
}
