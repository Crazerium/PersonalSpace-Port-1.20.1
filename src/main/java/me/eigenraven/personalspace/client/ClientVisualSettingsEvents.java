package me.eigenraven.personalspace.client;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(
        modid = PersonalSpace.MODID,
        value = net.neoforged.api.distmarker.Dist.CLIENT
)
public final class ClientVisualSettingsEvents {
    private static CloudStatus previousCloudStatus = null;
    private static boolean cloudOverrideActive = false;

    private ClientVisualSettingsEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            restoreCloudsIfNeeded(minecraft);
            return;
        }

        ResourceLocation levelId = minecraft.level.dimension().location();

        if (!levelId.getNamespace().equals(PersonalSpace.MODID)) {
            restoreCloudsIfNeeded(minecraft);
            return;
        }

        ClientPersonalSpaceSettings.Settings settings =
                ClientPersonalSpaceSettings.get(levelId);

        if (!settings.cloudsEnabled()) {
            disableCloudsIfNeeded(minecraft);
        } else {
            restoreCloudsIfNeeded(minecraft);
        }
    }

    private static void disableCloudsIfNeeded(Minecraft minecraft) {
        CloudStatus current = minecraft.options.cloudStatus().get();

        if (!cloudOverrideActive) {
            previousCloudStatus = current;
            cloudOverrideActive = true;
        }

        if (current != CloudStatus.OFF) {
            minecraft.options.cloudStatus().set(CloudStatus.OFF);
        }
    }

    private static void restoreCloudsIfNeeded(Minecraft minecraft) {
        if (!cloudOverrideActive) {
            return;
        }

        if (previousCloudStatus != null) {
            minecraft.options.cloudStatus().set(previousCloudStatus);
        }

        previousCloudStatus = null;
        cloudOverrideActive = false;
    }
}