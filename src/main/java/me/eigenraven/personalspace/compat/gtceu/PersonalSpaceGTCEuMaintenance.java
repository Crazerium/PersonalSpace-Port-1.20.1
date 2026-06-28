package me.eigenraven.personalspace.compat.gtceu;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PSDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class PersonalSpaceGTCEuMaintenance {
    private static final String GTCEU_BEDROCK_FLUID_FILE = "gtceu_bedrock_fluid.dat";

    private PersonalSpaceGTCEuMaintenance() {
    }

    public static int rebuildAllPersonalSpaceFluids(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);

        Path modernRoot = worldRoot
                .resolve("dimensions")
                .resolve(PersonalSpace.MODID)
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER);

        Path legacyRoot = worldRoot
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER);

        RebuildResult result = new RebuildResult();

        scanAndRebuildRoot(modernRoot, result);
        scanAndRebuildRoot(legacyRoot, result);

        source.sendSuccess(
                () -> Component.literal(
                        "§aGTCEu fluids rebuild подготовлен. "
                                + "Персоналок найдено: §e" + result.found
                                + "§a, уникальных: §e" + result.uniqueNames.size()
                                + "§a, файлов удалено: §e" + result.deleted
                                + "§a, измерений пропатчено: §e" + result.patched
                                + "§a. Теперь сразу сделай §c/stop§a и запусти сервер заново."
                ),
                false
        );

        return 1;
    }

    public static int rebuildOnePersonalSpaceFluids(CommandSourceStack source, String name) {
        MinecraftServer server = source.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);

        String folderName = normalizeDimensionFolderName(name);

        Path modernPath = worldRoot
                .resolve("dimensions")
                .resolve(PersonalSpace.MODID)
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER)
                .resolve(folderName);

        Path legacyPath = worldRoot
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER)
                .resolve(folderName);

        RebuildResult result = new RebuildResult();

        rebuildDimensionPath(modernPath, folderName, result);
        rebuildDimensionPath(legacyPath, folderName, result);

        if (result.found <= 0) {
            source.sendFailure(Component.literal("§cПерсоналка не найдена: §f" + folderName));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§aGTCEu fluids rebuild подготовлен для §e" + folderName
                                + "§a. Файлов удалено: §e" + result.deleted
                                + "§a, измерений пропатчено: §e" + result.patched
                                + "§a. Теперь сразу сделай §c/stop§a и запусти сервер заново."
                ),
                false
        );

        return 1;
    }

    private static void scanAndRebuildRoot(Path root, RebuildResult result) {
        if (!Files.exists(root)) {
            PersonalSpace.LOGGER.info("GTCEu rebuild skipped missing root: {}", root);
            return;
        }

        try (Stream<Path> stream = Files.list(root)) {
            for (Path dimensionPath : stream.filter(Files::isDirectory).toList()) {
                String folderName = dimensionPath.getFileName().toString();

                if (!folderName.startsWith("ps_") && !folderName.startsWith("team_")) {
                    continue;
                }

                rebuildDimensionPath(dimensionPath, folderName, result);
            }
        } catch (IOException exception) {
            PersonalSpace.LOGGER.warn("Failed to scan Personal Space root {} for GTCEu fluid rebuild.", root, exception);
        }
    }

    private static void rebuildDimensionPath(Path dimensionPath, String folderName, RebuildResult result) {
        if (!Files.exists(dimensionPath)) {
            return;
        }

        result.found++;

        if (result.uniqueNames.add(folderName)) {
            ResourceLocation dimensionId = new ResourceLocation(
                    PersonalSpace.MODID,
                    PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/" + folderName
            );

            ResourceKey<Level> levelKey = ResourceKey.create(
                    Registries.DIMENSION,
                    dimensionId
            );

            try {
                PersonalSpaceGTCEuHooks.onPersonalDimensionCreated(levelKey);
                result.patched++;
            } catch (Throwable throwable) {
                PersonalSpace.LOGGER.warn(
                        "Failed to patch GTCEu bedrock fluids for {}",
                        levelKey.location(),
                        throwable
                );
            }
        }

        Path fluidDataFile = dimensionPath
                .resolve("data")
                .resolve(GTCEU_BEDROCK_FLUID_FILE);

        if (!Files.exists(fluidDataFile)) {
            PersonalSpace.LOGGER.info("GTCEu bedrock fluid file not found for {} at {}", folderName, fluidDataFile);
            return;
        }

        try {
            Files.deleteIfExists(fluidDataFile);
            result.deleted++;

            PersonalSpace.LOGGER.info(
                    "Deleted GTCEu bedrock fluid data for {} at {}",
                    folderName,
                    fluidDataFile
            );
        } catch (IOException exception) {
            PersonalSpace.LOGGER.warn(
                    "Failed to delete GTCEu bedrock fluid data for {} at {}",
                    folderName,
                    fluidDataFile,
                    exception
            );
        }
    }

    private static String normalizeDimensionFolderName(String name) {
        String value = name.trim().toLowerCase();

        if (value.startsWith(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/")) {
            value = value.substring((PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/").length());
        }

        if (!value.startsWith("ps_") && !value.startsWith("team_")) {
            value = "ps_" + value;
        }

        return value;
    }

    private static final class RebuildResult {
        private int found = 0;
        private int deleted = 0;
        private int patched = 0;
        private final Set<String> uniqueNames = new HashSet<>();
    }
}