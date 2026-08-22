package me.eigenraven.personalspace.dimension;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class PersonalSpaceDimensionCatalog {
    private PersonalSpaceDimensionCatalog() {
    }

    public static List<ResourceKey<Level>> listDimensionKeys(MinecraftServer server) {
        Set<ResourceKey<Level>> keys = new LinkedHashSet<>();

        server.getAllLevels().forEach(level -> {
            ResourceLocation id = level.dimension().location();
            if (PSDimensions.isPersonalSpaceDimension(id)) {
                keys.add(level.dimension());
            }
        });

        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path modernRoot = worldRoot
                .resolve("dimensions")
                .resolve(PersonalSpace.MODID)
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER);

        scanDirectory(modernRoot, keys, true);

        Path namespacedLegacyRoot = worldRoot
                .resolve("dimensions")
                .resolve(PersonalSpace.MODID);
        scanDirectory(namespacedLegacyRoot, keys, false);

        return keys.stream()
                .sorted(Comparator.comparing(key -> key.location().toString()))
                .toList();
    }

    public static List<String> listCommandNames(MinecraftServer server) {
        List<String> result = new ArrayList<>();

        for (ResourceKey<Level> key : listDimensionKeys(server)) {
            ResourceLocation id = key.location();
            String path = id.getPath();
            String prefix = PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/";

            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
            }

            if (!path.isBlank() && !result.contains(path)) {
                result.add(path);
            }
        }

        return result;
    }

    public static boolean existsOnDisk(MinecraftServer server, ResourceKey<Level> key) {
        Path path = dimensionPath(server, key);
        return Files.isDirectory(path);
    }

    public static Path dimensionPath(MinecraftServer server, ResourceKey<Level> key) {
        ResourceLocation id = key.location();
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions")
                .resolve(id.getNamespace())
                .resolve(id.getPath());
    }

    private static void scanDirectory(
            Path root,
            Set<ResourceKey<Level>> keys,
            boolean childNamesArePersonalSpaceIds
    ) {
        if (!Files.isDirectory(root)) {
            return;
        }

        try (Stream<Path> children = Files.list(root)) {
            children.filter(Files::isDirectory).forEach(path -> {
                String name = path.getFileName().toString();

                if (childNamesArePersonalSpaceIds) {
                    keys.add(PSDimensions.key(name));
                    return;
                }

                if (name.startsWith("ps_") || name.startsWith("team_")) {
                    keys.add(PSDimensions.key(name));
                }
            });
        } catch (IOException exception) {
            PersonalSpace.LOGGER.warn("Failed to scan Personal Space catalog at {}", root, exception);
        }
    }
}
