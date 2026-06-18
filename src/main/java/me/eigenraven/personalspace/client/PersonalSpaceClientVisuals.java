package me.eigenraven.personalspace.client;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = PersonalSpace.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PersonalSpaceClientVisuals {
    private PersonalSpaceClientVisuals() {
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        ResourceLocation levelId = minecraft.level.dimension().location();

        if (!levelId.getNamespace().equals(PersonalSpace.MODID)) {
            return;
        }

        ClientPersonalSpaceSettings.Settings settings = ClientPersonalSpaceSettings.get(levelId);

        event.setRed(settings.skyRed() / 255.0F);
        event.setGreen(settings.skyGreen() / 255.0F);
        event.setBlue(settings.skyBlue() / 255.0F);
    }
}