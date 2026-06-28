package me.eigenraven.personalspace.compat.gtceu;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PSDimensions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class PersonalSpaceGTCEuStartupPatcher {
    private PersonalSpaceGTCEuStartupPatcher() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        if (!ModList.get().isLoaded("gtceu")) {
            return;
        }

        patchAllExistingPersonalSpaces(event.getServer());
    }

    public static int patchAllExistingPersonalSpaces(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions")
                .resolve(PersonalSpace.MODID)
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER);

        if (!Files.exists(root)) {
            PersonalSpace.LOGGER.info("GTCEu startup patch skipped: Personal Space root does not exist: {}", root);
            return 0;
        }

        int patched = 0;

        try (Stream<Path> stream = Files.list(root)) {
            for (Path dimensionPath : stream.filter(Files::isDirectory).toList()) {
                String folderName = dimensionPath.getFileName().toString();

                if (!folderName.startsWith("ps_") && !folderName.startsWith("team_")) {
                    continue;
                }

                ResourceLocation dimensionId = new ResourceLocation(
                        PersonalSpace.MODID,
                        PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/" + folderName
                );

                ResourceKey<Level> levelKey = ResourceKey.create(
                        Registries.DIMENSION,
                        dimensionId
                );

                PersonalSpaceGTCEuHooks.onPersonalDimensionCreated(levelKey);
                patched++;
            }
        } catch (IOException exception) {
            PersonalSpace.LOGGER.warn("Failed to patch existing Personal Space dimensions into GTCEu bedrock fluid veins.", exception);
            return patched;
        }

        PersonalSpace.LOGGER.info(
                "GTCEu startup patch added {} existing Personal Space dimension(s) to bedrock fluid vein filters.",
                patched
        );

        return patched;
    }
}