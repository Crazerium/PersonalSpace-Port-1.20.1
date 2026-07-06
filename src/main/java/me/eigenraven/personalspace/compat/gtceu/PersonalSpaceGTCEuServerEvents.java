package me.eigenraven.personalspace.compat.gtceu;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = PersonalSpace.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PersonalSpaceGTCEuServerEvents {
    private PersonalSpaceGTCEuServerEvents() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (event == null || event.getServer() == null) {
            return;
        }

        if (!ModList.get().isLoaded("gtceu")) {
            return;
        }

        if (!PersonalSpaceGTCEuConfig.ENABLED.get()) {
            PersonalSpace.LOGGER.info(
                    "GTCEu Personal Space startup patch skipped because compatibility is disabled."
            );
            return;
        }

        if (!PersonalSpaceGTCEuConfig.PATCH_EXISTING_PERSONAL_SPACES_ON_STARTUP.get()) {
            PersonalSpace.LOGGER.info(
                    "GTCEu Personal Space startup patch skipped by config."
            );
            return;
        }
        int patched = PersonalSpaceBedrockFluidVeins.addExistingPersonalSpaceDimensionsToOwnVeins(
                event.getServer()
        );

        PersonalSpace.LOGGER.info(
                "GTCEu Personal Space early data-only patch added {} existing Personal Space dimension(s) to own fluid veins.",
                patched
        );
    }
}