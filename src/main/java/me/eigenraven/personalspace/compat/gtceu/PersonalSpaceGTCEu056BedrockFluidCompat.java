package me.eigenraven.personalspace.compat.gtceu;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PSDimensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Runtime compatibility for PersonalSpace bedrock-fluid veins on GTO 0.5.6 / GTCEu 26.x.
 *
 * Important: this class deliberately avoids linking against GTRegistry#getKey().
 * PersonalSpace is normally compiled against the older GTCEu API, while GTO 0.5.6
 * ships a newer DataSyncLib-backed Registry whose erased getKey() return descriptor
 * changed from Object to Comparable. A direct call therefore produces a
 * NoSuchMethodError at runtime even though the Java source compiles.
 *
 * The runtime registry access below is reflection-only, and every event handler is
 * fail-safe: a compatibility failure is logged but is never allowed to abort a
 * PersonalSpace LevelEvent.Load / dimension creation.
 */
@Mod.EventBusSubscriber(modid = PersonalSpace.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PersonalSpaceGTCEu056BedrockFluidCompat {
    private static final String GT_REGISTRIES_CLASS =
            "com.gregtechceu.gtceu.api.registry.GTRegistries";

    private PersonalSpaceGTCEu056BedrockFluidCompat() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!ModList.get().isLoaded("gtceu")) {
            return;
        }

        try {
            PatchSummary summary = patchAllKnownDimensions(event.getServer());
            PersonalSpace.LOGGER.info(
                    "GTCEu 26 bedrock-fluid compat v2: patched {} existing PersonalSpace dimension(s); "
                            + "matched {} runtime definition link(s), added {} missing link(s).",
                    summary.dimensions(), summary.matchedDefinitions(), summary.addedLinks());
        } catch (Throwable t) {
            // Never let optional GT compatibility prevent the server from finishing startup.
            PersonalSpace.LOGGER.error(
                    "GTCEu 26 bedrock-fluid compat v2 failed during ServerStarted. "
                            + "PersonalSpace itself will continue loading.",
                    t);
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!isPersonalSpace(level.dimension())) {
            return;
        }

        try {
            PatchStats stats = patchDimensionRuntime(level.dimension());
            PersonalSpace.LOGGER.info(
                    "GTCEu 26 bedrock-fluid compat v2: dimension {} -> configuredFluids={}, "
                            + "matchedDefinitions={}, addedLinks={}.",
                    level.dimension().location(),
                    stats.configuredFluids(),
                    stats.matchedDefinitions(),
                    stats.addedLinks());
        } catch (Throwable t) {
            // Critical safety rule: do not propagate from LevelEvent.Load. An exception here
            // can interrupt Infiniverse dimension creation and leave a half-created empty level.
            PersonalSpace.LOGGER.error(
                    "GTCEu 26 bedrock-fluid compat v2 failed for {}. "
                            + "The error was suppressed so PersonalSpace dimension creation can continue.",
                    level.dimension().location(),
                    t);
        }
    }

    private static PatchSummary patchAllKnownDimensions(MinecraftServer server) {
        Set<ResourceKey<Level>> dimensions = findExistingPersonalSpaceDimensions(server);
        int matched = 0;
        int added = 0;

        for (ResourceKey<Level> dimension : dimensions) {
            try {
                PatchStats stats = patchDimensionRuntime(dimension);
                matched += stats.matchedDefinitions();
                added += stats.addedLinks();
            } catch (Throwable t) {
                PersonalSpace.LOGGER.warn(
                        "GTCEu 26 bedrock-fluid compat v2: failed to patch existing dimension {}.",
                        dimension.location(),
                        t);
            }
        }

        return new PatchSummary(dimensions.size(), matched, added);
    }

    /**
     * Patches one actual PersonalSpace dimension into all runtime bedrock-fluid
     * definitions whose fluid is enabled for that dimension by personalspace-gtceu.toml.
     *
     * This intentionally matches by fluid id instead of registry definition id. It avoids
     * GTRegistry#getKey binary incompatibility and also covers both the PersonalSpace
     * custom definitions and GTO's standard definitions for the same configured fluids.
     */
    private static PatchStats patchDimensionRuntime(ResourceKey<Level> levelKey) throws Throwable {
        // Keep the original PersonalSpace integration path active as well. It is reflection
        // based and knows about the mod's wildcard definition set.
        try {
            PersonalSpaceGTCEuHooks.onPersonalDimensionCreated(levelKey);
        } catch (Throwable t) {
            PersonalSpace.LOGGER.warn(
                    "Legacy GTCEu bedrock-fluid hook failed for {}; continuing with GTCEu 26 runtime patch.",
                    levelKey.location(),
                    t);
        }

        Set<ResourceLocation> configuredFluids = configuredFluidsForDimension(levelKey);
        if (configuredFluids.isEmpty()) {
            return new PatchStats(0, 0, 0);
        }

        Class<?> gtRegistries = Class.forName(GT_REGISTRIES_CLASS);
        Field registryField = gtRegistries.getField("BEDROCK_FLUID_DEFINITIONS");
        Object registry = registryField.get(null);
        if (registry == null) {
            throw new IllegalStateException("GTRegistries.BEDROCK_FLUID_DEFINITIONS is null");
        }

        Method valuesMethod = registry.getClass().getMethod("values");
        Object rawValues = valuesMethod.invoke(registry);
        if (!(rawValues instanceof Iterable<?> definitions)) {
            throw new IllegalStateException(
                    "BEDROCK_FLUID_DEFINITIONS.values() is not Iterable: "
                            + (rawValues == null ? "null" : rawValues.getClass().getName()));
        }

        int matched = 0;
        int added = 0;

        for (Object definition : definitions) {
            if (definition == null) {
                continue;
            }

            ResourceLocation fluidId = getDefinitionFluidId(definition);
            if (fluidId == null || !configuredFluids.contains(fluidId)) {
                continue;
            }
            matched++;

            Method getDimensionFilter = definition.getClass().getMethod("getDimensionFilter");
            Method setDimensionFilter = definition.getClass().getMethod("setDimensionFilter", Set.class);

            Object rawFilter = getDimensionFilter.invoke(definition);
            Set<Object> updated = new HashSet<>();
            if (rawFilter instanceof Set<?> existing) {
                updated.addAll(existing);
            }

            if (updated.add(levelKey)) {
                setDimensionFilter.invoke(definition, updated);
                added++;
            }
        }

        return new PatchStats(configuredFluids.size(), matched, added);
    }

    private static ResourceLocation getDefinitionFluidId(Object definition) {
        try {
            Method getStoredFluid = definition.getClass().getMethod("getStoredFluid");
            Object rawSupplier = getStoredFluid.invoke(definition);
            if (!(rawSupplier instanceof Supplier<?> supplier)) {
                return null;
            }
            Object rawFluid = supplier.get();
            if (!(rawFluid instanceof Fluid fluid)) {
                return null;
            }
            return BuiltInRegistries.FLUID.getKey(fluid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Set<ResourceLocation> configuredFluidsForDimension(ResourceKey<Level> levelKey) {
        Set<ResourceLocation> result = new HashSet<>();

        List<? extends String> entries;
        try {
            entries = PersonalSpaceGTCEuConfig.BEDROCK_FLUID_VEINS.get();
        } catch (Throwable t) {
            PersonalSpace.LOGGER.warn("Unable to read PersonalSpace GTCEu bedrock-fluid config.", t);
            return result;
        }

        if (entries == null) {
            return result;
        }

        for (String rawEntry : entries) {
            if (rawEntry == null || rawEntry.isBlank()) {
                continue;
            }

            String[] parts = rawEntry.split("\\|");
            if (parts.length != 9) {
                continue;
            }

            if (!dimensionSelectorMatches(parts[2], levelKey)) {
                continue;
            }

            ResourceLocation fluidId = ResourceLocation.tryParse(parts[1].trim());
            if (fluidId != null) {
                result.add(fluidId);
            }
        }

        return result;
    }

    private static boolean dimensionSelectorMatches(String selector, ResourceKey<Level> levelKey) {
        if (selector == null || selector.isBlank() || levelKey == null) {
            return false;
        }

        ResourceLocation actual = levelKey.location();
        String actualPath = actual.getPath();
        String folderPrefix = PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/";
        String shortName = actualPath.startsWith(folderPrefix)
                ? actualPath.substring(folderPrefix.length())
                : actualPath;

        for (String tokenRaw : selector.split(",")) {
            String token = tokenRaw.trim();
            if (token.isEmpty()) {
                continue;
            }

            if ("personalspace:*".equals(token)) {
                return true;
            }
            if ("personalspace:ps_*".equals(token) && shortName.startsWith("ps_")) {
                return true;
            }
            if ("personalspace:team_*".equals(token) && shortName.startsWith("team_")) {
                return true;
            }

            ResourceLocation configured = ResourceLocation.tryParse(token);
            if (configured == null) {
                continue;
            }
            if (configured.equals(actual)) {
                return true;
            }

            // Keep compatibility with the config's short exact form:
            // personalspace:ps_name / personalspace:team_name.
            if (PersonalSpace.MODID.equals(configured.getNamespace())
                    && configured.getPath().equals(shortName)) {
                return true;
            }
        }

        return false;
    }

    private static Set<ResourceKey<Level>> findExistingPersonalSpaceDimensions(MinecraftServer server) {
        Set<ResourceKey<Level>> result = new HashSet<>();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);

        scanRoot(
                worldRoot.resolve("dimensions")
                        .resolve(PersonalSpace.MODID)
                        .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER),
                result);

        // Legacy/fallback layout used by some older PersonalSpace builds.
        scanRoot(worldRoot.resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER), result);
        return result;
    }

    private static void scanRoot(Path root, Set<ResourceKey<Level>> out) {
        if (!Files.isDirectory(root)) {
            return;
        }

        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory).forEach(path -> {
                String name = path.getFileName().toString();
                if (!name.startsWith("ps_") && !name.startsWith("team_")) {
                    return;
                }

                ResourceLocation id = new ResourceLocation(
                        PersonalSpace.MODID,
                        PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/" + name);
                out.add(ResourceKey.create(Registries.DIMENSION, id));
            });
        } catch (IOException e) {
            PersonalSpace.LOGGER.warn(
                    "GTCEu 26 bedrock-fluid compat v2: failed to scan PersonalSpace root {}.",
                    root,
                    e);
        }
    }

    private static boolean isPersonalSpace(ResourceKey<Level> key) {
        return key != null && PSDimensions.isPersonalSpaceDimension(key.location());
    }

    private record PatchStats(int configuredFluids, int matchedDefinitions, int addedLinks) {}

    private record PatchSummary(int dimensions, int matchedDefinitions, int addedLinks) {}
}
