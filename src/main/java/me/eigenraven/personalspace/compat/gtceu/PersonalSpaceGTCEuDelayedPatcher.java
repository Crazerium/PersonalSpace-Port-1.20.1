package me.eigenraven.personalspace.compat.gtceu;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PSDimensions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class PersonalSpaceGTCEuDelayedPatcher {
    private static int ticksUntilPatch = 200;
    private static boolean patched = false;

    private PersonalSpaceGTCEuDelayedPatcher() {
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (patched) {
            return;
        }

        if (!ModList.get().isLoaded("gtceu")) {
            patched = true;
            return;
        }

        if (!PersonalSpaceGTCEuConfig.ENABLED.get()) {
            patched = true;
            PersonalSpace.LOGGER.info("GTCEu delayed patch skipped because GTCEu Personal Space compatibility is disabled in config.");
            return;
        }

        if (!PersonalSpaceGTCEuConfig.PATCH_EXISTING_PERSONAL_SPACES_ON_STARTUP.get()) {
            patched = true;
            PersonalSpace.LOGGER.info("GTCEu delayed patch for existing Personal Space dimensions skipped by config. Set patchExistingPersonalSpacesOnStartup=true to run it once.");
            return;
        }

        ticksUntilPatch--;

        if (ticksUntilPatch > 0) {
            return;
        }

        patched = true;

        int count = patchAllExistingPersonalSpaces(event.getServer());

        PersonalSpace.LOGGER.info(
                "GTCEu delayed patch finished for {} existing Personal Space dimension(s).",
                count
        );
    }

    public static int patchAllExistingPersonalSpaces(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);

        Path modernRoot = worldRoot
                .resolve("dimensions")
                .resolve(PersonalSpace.MODID)
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER);

        Path legacyRoot = worldRoot
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER);

        Set<String> patchedNames = new HashSet<>();

        int modernCount = patchRoot(modernRoot, patchedNames);
        int legacyCount = patchRoot(legacyRoot, patchedNames);

        PersonalSpace.LOGGER.info(
                "GTCEu delayed patch scanned Personal Space roots. modern={}, legacy={}, unique={}",
                modernCount,
                legacyCount,
                patchedNames.size()
        );

        return patchedNames.size();
    }

    private static int patchRoot(Path root, Set<String> patchedNames) {
        if (!Files.exists(root)) {
            PersonalSpace.LOGGER.info("GTCEu delayed patch skipped missing root: {}", root);
            return 0;
        }

        int count = 0;

        try (Stream<Path> stream = Files.list(root)) {
            for (Path dimensionPath : stream.filter(Files::isDirectory).toList()) {
                String folderName = dimensionPath.getFileName().toString();

                if (!folderName.startsWith("ps_") && !folderName.startsWith("team_")) {
                    continue;
                }

                if (!patchedNames.add(folderName)) {
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
                count++;
            }
        } catch (IOException exception) {
            PersonalSpace.LOGGER.warn(
                    "Failed to delayed patch existing Personal Space dimensions from root {} into GTCEu bedrock fluid veins.",
                    root,
                    exception
            );
        }

        return count;
    }

    public static void resetForNewServerStart() {
        ticksUntilPatch = 200;
        patched = false;
    }
}
