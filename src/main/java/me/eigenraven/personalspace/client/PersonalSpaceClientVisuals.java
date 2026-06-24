package me.eigenraven.personalspace.client;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(
        modid = PersonalSpace.MODID,
        value = net.neoforged.api.distmarker.Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME
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