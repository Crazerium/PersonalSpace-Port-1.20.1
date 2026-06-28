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
import java.util.stream.Stream;

public final class PersonalSpaceGTCEuMaintenance {
    private static final String GTCEU_BEDROCK_FLUID_FILE = "gtceu_bedrock_fluid.dat";

    private PersonalSpaceGTCEuMaintenance() {
    }

    public static int rebuildAllPersonalSpaceFluids(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        Path root = getPersonalSpaceDimensionsRoot(server);

        if (!Files.exists(root)) {
            source.sendFailure(Component.literal("§cПапка персоналок не найдена: §f" + root));
            return 0;
        }

        int found = 0;
        int deleted = 0;
        int patched = 0;

        try (Stream<Path> stream = Files.list(root)) {
            for (Path dimensionPath : stream.filter(Files::isDirectory).toList()) {
                String folderName = dimensionPath.getFileName().toString();

                if (!folderName.startsWith("ps_") && !folderName.startsWith("team_")) {
                    continue;
                }

                found++;

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
                    patched++;
                } catch (Throwable throwable) {
                    PersonalSpace.LOGGER.warn(
                            "Failed to patch GTCEu bedrock fluids for {}",
                            levelKey.location(),
                            throwable
                    );
                }

                Path fluidDataFile = dimensionPath
                        .resolve("data")
                        .resolve(GTCEU_BEDROCK_FLUID_FILE);

                if (Files.exists(fluidDataFile)) {
                    try {
                        Files.deleteIfExists(fluidDataFile);
                        deleted++;
                    } catch (IOException exception) {
                        PersonalSpace.LOGGER.warn(
                                "Failed to delete GTCEu bedrock fluid data file {}",
                                fluidDataFile,
                                exception
                        );
                    }
                }
            }
        } catch (IOException exception) {
            source.sendFailure(Component.literal("§cОшибка при сканировании персоналок. Смотри лог сервера."));
            PersonalSpace.LOGGER.warn("Failed to scan Personal Space dimensions for GTCEu fluid rebuild.", exception);
            return 0;
        }

        int finalFound = found;
        int finalDeleted = deleted;
        int finalPatched = patched;

        source.sendSuccess(
                () -> Component.literal(
                        "§aGTCEu fluids rebuild подготовлен. "
                                + "Персоналок найдено: §e" + finalFound
                                + "§a, файлов удалено: §e" + finalDeleted
                                + "§a, измерений пропатчено: §e" + finalPatched
                                + "§a. Теперь сделай полный рестарт сервера."
                ),
                false
        );

        return 1;
    }

    public static int rebuildOnePersonalSpaceFluids(CommandSourceStack source, String name) {
        MinecraftServer server = source.getServer();

        String folderName = normalizeDimensionFolderName(name);
        Path dimensionPath = getPersonalSpaceDimensionsRoot(server).resolve(folderName);

        if (!Files.exists(dimensionPath)) {
            source.sendFailure(Component.literal("§cПерсоналка не найдена: §f" + folderName));
            return 0;
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

        Path fluidDataFile = dimensionPath
                .resolve("data")
                .resolve(GTCEU_BEDROCK_FLUID_FILE);

        boolean deleted = false;

        if (Files.exists(fluidDataFile)) {
            try {
                Files.deleteIfExists(fluidDataFile);
                deleted = true;
            } catch (IOException exception) {
                source.sendFailure(Component.literal("§cНе удалось удалить файл GTCEu fluids. Смотри лог сервера."));
                PersonalSpace.LOGGER.warn("Failed to delete {}", fluidDataFile, exception);
                return 0;
            }
        }

        boolean finalDeleted = deleted;

        source.sendSuccess(
                () -> Component.literal(
                        "§aGTCEu fluids rebuild подготовлен для §e" + folderName
                                + "§a. Файл был удалён: §e" + finalDeleted
                                + "§a. Теперь сделай полный рестарт сервера."
                ),
                false
        );

        return 1;
    }

    private static Path getPersonalSpaceDimensionsRoot(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions")
                .resolve(PersonalSpace.MODID)
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER);
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
}