package me.eigenraven.personalspace.compat.gtceu;

import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PSDimensions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PersonalSpaceBedrockFluidVeins {
    private static final Set<BedrockFluidDefinition> PERSONAL_SPACE_WILDCARD_VEINS = new HashSet<>();

    private static boolean initialized = false;

    private PersonalSpaceBedrockFluidVeins() {
    }

    public static void init() {
        PersonalSpace.LOGGER.warn("PersonalSpaceBedrockFluidVeins.init() ENTERED.");

        if (initialized) {
            PersonalSpace.LOGGER.warn("GTCEu bedrock fluid veins init skipped: already initialized.");
            return;
        }

        initialized = true;

        boolean enabled;

        try {
            enabled = PersonalSpaceGTCEuConfig.ENABLED.get();
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.warn("GTCEu bedrock fluid veins init failed while reading ENABLED config.", throwable);
            return;
        }

        PersonalSpace.LOGGER.warn("GTCEu bedrock fluid veins config enabled={}.", enabled);

        if (!enabled) {
            PersonalSpace.LOGGER.warn("GTCEu Personal Space bedrock fluid veins are disabled by config.");
            return;
        }

        List<? extends String> veins;

        try {
            veins = PersonalSpaceGTCEuConfig.BEDROCK_FLUID_VEINS.get();
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.warn("GTCEu bedrock fluid veins init failed while reading BEDROCK_FLUID_VEINS config.", throwable);
            return;
        }

        int veinCount = veins == null ? -1 : veins.size();

        PersonalSpace.LOGGER.warn("GTCEu bedrock fluid veins config entries count={}.", veinCount);

        if (veins == null || veins.isEmpty()) {
            PersonalSpace.LOGGER.warn("GTCEu bedrock fluid veins config list is empty. Nothing will be registered.");
            return;
        }

        int registered = 0;
        int failed = 0;

        for (String rawEntry : veins) {
            PersonalSpace.LOGGER.warn("GTCEu bedrock fluid vein raw config entry: {}", rawEntry);

            try {
                boolean ok = registerFromConfigDebug(rawEntry);

                if (ok) {
                    registered++;
                } else {
                    failed++;
                }
            } catch (Throwable throwable) {
                failed++;

                PersonalSpace.LOGGER.warn(
                        "GTCEu bedrock fluid vein config entry crashed: {}",
                        rawEntry,
                        throwable
                );
            }
        }

        PersonalSpace.LOGGER.warn(
                "GTCEu Personal Space bedrock fluid veins init finished. registered={}, failed={}, wildcardDefinitions={}.",
                registered,
                failed,
                PERSONAL_SPACE_WILDCARD_VEINS.size()
        );
    }

    private static boolean registerFromConfigDebug(String rawEntry) {
        if (rawEntry == null || rawEntry.isBlank()) {
            PersonalSpace.LOGGER.warn("GTCEu bedrock fluid vein config entry skipped: blank entry.");
            return false;
        }

        String[] parts = rawEntry.split("\\|");

        PersonalSpace.LOGGER.warn(
                "GTCEu bedrock fluid vein parsed parts count={} for entry={}",
                parts.length,
                rawEntry
        );

        if (parts.length != 9) {
            PersonalSpace.LOGGER.warn(
                    "GTCEu bedrock fluid vein config entry skipped: expected 9 parts, got {}. entry={}",
                    parts.length,
                    rawEntry
            );

            return false;
        }

        PersonalSpace.LOGGER.warn(
                "GTCEu bedrock fluid vein parsed: id={}, fluid={}, dimensions={}, weight={}, minYield={}, maxYield={}, depletion={}, depletionChance={}, depletedYield={}",
                parts[0],
                parts[1],
                parts[2],
                parts[3],
                parts[4],
                parts[5],
                parts[6],
                parts[7],
                parts[8]
        );

        registerFromConfig(rawEntry);

        PersonalSpace.LOGGER.warn(
                "GTCEu bedrock fluid vein registerFromConfig returned normally for id={}",
                parts[0]
        );

        return true;
    }

    private static void registerFromConfig(String rawEntry) {
        if (rawEntry == null || rawEntry.isBlank()) {
            return;
        }

        String[] parts = rawEntry.split("\\|");

        if (parts.length != 9) {
            PersonalSpace.LOGGER.warn(
                    "GTCEu bedrock fluid vein config entry skipped: expected 9 parts, got {}. entry={}",
                    parts.length,
                    rawEntry
            );

            return;
        }

        String id = parts[0].trim();
        String fluidName = parts[1].trim();
        String dimensionsRaw = parts[2].trim();

        int weight = Integer.parseInt(parts[3].trim());
        int minYield = Integer.parseInt(parts[4].trim());
        int maxYield = Integer.parseInt(parts[5].trim());
        int depletionAmount = Integer.parseInt(parts[6].trim());
        int depletionChance = Integer.parseInt(parts[7].trim());
        int depletedYield = Integer.parseInt(parts[8].trim());

        ResourceLocation idLocation = new ResourceLocation(id);
        ResourceLocation fluidLocation = new ResourceLocation(fluidName);

        Set<ResourceKey<Level>> dimensions = parseDimensions(dimensionsRaw);

        boolean personalSpaceWildcard = hasPersonalSpaceWildcard(dimensionsRaw);

        /*
         * Пока оставляем hardcoded test dimension, потому что именно на ней ты тестируешь.
         * Потом, когда всё заработает стабильно, можно убрать этот блок и полагаться
         * на addExistingPersonalSpaceDimensionsToOwnVeins(server).
         */
        if (personalSpaceWildcard) {
            ResourceKey<Level> testDimension = ResourceKey.create(
                    Registries.DIMENSION,
                    new ResourceLocation(
                            PersonalSpace.MODID,
                            PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/team_9c619856388d4ae8ae032690939d5365"
                    )
            );

            dimensions.add(testDimension);

            PersonalSpace.LOGGER.warn(
                    "GTCEu bedrock fluid vein debug: added hardcoded test Personal Space dimension {} to {}",
                    testDimension.location(),
                    idLocation
            );
        }

        /*
         * ВАЖНО:
         * Не резолвим Fluid заранее через ForgeRegistries.FLUIDS.getValue(fluidLocation).
         *
         * Этот init сейчас вызывается очень рано из конструктора PersonalSpace.
         * На этом этапе gtceu:oil / gtceu:natural_gas могут ещё не быть готовы
         * как нормальные Fluid-объекты, и ранний getValue(...) может дать minecraft:empty.
         *
         * Поэтому supplier должен быть ленивым и искать Fluid только когда GTCEu реально
         * запросит жидкость у definition.
         */
        BedrockFluidDefinition definition = BedrockFluidDefinition.builder(idLocation)
                .fluid(() -> resolveFluidLazy(fluidLocation, idLocation))
                .weight(weight)
                .minimumYield(minYield)
                .maximumYield(maxYield)
                .depletionAmount(depletionAmount)
                .depletionChance(depletionChance)
                .depletedYield(depletedYield)
                .dimensions(dimensions)
                .register();

        /*
         * В твоей версии register() уже регистрирует definition.
         * registerOrOverride оставляем как страховку, потому что в логе он отработал нормально.
         */
        try {
            GTRegistries.BEDROCK_FLUID_DEFINITIONS.registerOrOverride(idLocation, definition);

            PersonalSpace.LOGGER.warn(
                    "GTCEu bedrock fluid vein registerOrOverride called for {}",
                    idLocation
            );
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.warn(
                    "GTCEu bedrock fluid vein registerOrOverride failed/skipped for {}",
                    idLocation,
                    throwable
            );
        }

        if (personalSpaceWildcard) {
            PERSONAL_SPACE_WILDCARD_VEINS.add(definition);
        }

        PersonalSpace.LOGGER.warn(
                "Registered GTCEu Personal Space bedrock fluid vein {} fluid={} dimensions={} weight={} yield={}-{}",
                idLocation,
                fluidLocation,
                dimensions,
                weight,
                minYield,
                maxYield
        );
    }

    private static Fluid resolveFluidLazy(ResourceLocation fluidLocation, ResourceLocation veinId) {
        Fluid resolvedFluid = ForgeRegistries.FLUIDS.getValue(fluidLocation);

        if (resolvedFluid == null) {
            PersonalSpace.LOGGER.warn(
                    "GTCEu bedrock fluid vein {} lazy fluid resolve failed: {} is null. Using minecraft:empty.",
                    veinId,
                    fluidLocation
            );

            return Fluids.EMPTY;
        }

        ResourceLocation resolvedId = ForgeRegistries.FLUIDS.getKey(resolvedFluid);

        if (resolvedId == null || resolvedId.equals(new ResourceLocation("minecraft", "empty"))) {
            PersonalSpace.LOGGER.warn(
                    "GTCEu bedrock fluid vein {} lazy fluid resolve got {} for requested {}. Using minecraft:empty.",
                    veinId,
                    resolvedId,
                    fluidLocation
            );

            return Fluids.EMPTY;
        }

        return resolvedFluid;
    }

    public static void addPersonalSpaceDimension(ResourceKey<Level> levelKey) {
        if (levelKey == null) {
            return;
        }

        int changed = 0;

        for (BedrockFluidDefinition definition : PERSONAL_SPACE_WILDCARD_VEINS) {
            if (definition == null) {
                continue;
            }

            Set<ResourceKey<Level>> oldFilter = definition.dimensionFilter;
            Set<ResourceKey<Level>> newFilter = new HashSet<>();

            if (oldFilter != null) {
                newFilter.addAll(oldFilter);
            }

            if (newFilter.add(levelKey)) {
                definition.setDimensionFilter(newFilter);
                changed++;
            }
        }

        PersonalSpace.LOGGER.info(
                "Added Personal Space dimension '{}' to {} own GTCEu bedrock fluid vein definition(s).",
                levelKey.location(),
                changed
        );
    }

    public static void addPersonalSpaceDimensionToExistingGTCEuVeins(ResourceKey<Level> levelKey) {
        /*
         * ВАЖНО:
         * Ничего не делаем.
         *
         * Старый код добавлял Personal Space dimensions в существующие GTCEu/GTO жилы.
         * Из-за этого в сканере могла появляться чужая служебная жила вроде "Воздух".
         *
         * Нам нужны только собственные personalspace:void_* жилы из конфига.
         */
        PersonalSpace.LOGGER.debug(
                "Skipping patch of existing GTCEu/GTO bedrock fluid veins for Personal Space dimension {}.",
                levelKey == null ? "null" : levelKey.location()
        );
    }

    public static int addExistingPersonalSpaceDimensionsToOwnVeins(MinecraftServer server) {
        if (server == null) {
            return 0;
        }

        Set<ResourceKey<Level>> dimensions = findExistingPersonalSpaceDimensions(server);

        for (ResourceKey<Level> dimension : dimensions) {
            addPersonalSpaceDimension(dimension);
        }

        PersonalSpace.LOGGER.info(
                "GTCEu Personal Space data-only patch added {} existing Personal Space dimension(s) to own fluid veins.",
                dimensions.size()
        );

        return dimensions.size();
    }

    private static Set<ResourceKey<Level>> findExistingPersonalSpaceDimensions(MinecraftServer server) {
        Set<ResourceKey<Level>> result = new HashSet<>();

        Path worldRoot = server.getWorldPath(LevelResource.ROOT);

        Path modernRoot = worldRoot
                .resolve("dimensions")
                .resolve(PersonalSpace.MODID)
                .resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER);

        Path legacyRoot = worldRoot.resolve(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER);

        scanPersonalSpaceRoot(modernRoot, result);
        scanPersonalSpaceRoot(legacyRoot, result);

        return result;
    }

    private static void scanPersonalSpaceRoot(Path root, Set<ResourceKey<Level>> output) {
        if (root == null || output == null || !Files.isDirectory(root)) {
            return;
        }

        try (var stream = Files.list(root)) {
            stream
                    .filter(Files::isDirectory)
                    .forEach(path -> {
                        String folderName = path.getFileName().toString();

                        if (!isPersonalSpaceFolder(folderName)) {
                            return;
                        }

                        ResourceLocation dimensionId = new ResourceLocation(
                                PersonalSpace.MODID,
                                PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/" + folderName
                        );

                        ResourceKey<Level> dimensionKey = ResourceKey.create(
                                Registries.DIMENSION,
                                dimensionId
                        );

                        output.add(dimensionKey);
                    });
        } catch (IOException exception) {
            PersonalSpace.LOGGER.warn(
                    "Failed to scan Personal Space root '{}'.",
                    root,
                    exception
            );
        }
    }

    private static boolean isPersonalSpaceFolder(String folderName) {
        return folderName != null
                && (folderName.startsWith("ps_") || folderName.startsWith("team_"));
    }

    private static Set<ResourceKey<Level>> parseDimensions(String raw) {
        Set<ResourceKey<Level>> result = new HashSet<>();

        if (raw == null || raw.isBlank()) {
            return result;
        }

        String[] entries = raw.split(",");

        for (String entry : entries) {
            String clean = entry.trim();

            if (clean.isBlank()) {
                continue;
            }

            if (hasPersonalSpaceWildcard(clean)) {
                continue;
            }

            ResourceLocation dimensionId = new ResourceLocation(clean);

            result.add(ResourceKey.create(
                    Registries.DIMENSION,
                    dimensionId
            ));
        }

        return result;
    }

    private static boolean hasPersonalSpaceWildcard(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }

        String clean = raw.trim();

        return clean.equals("personalspace:*")
                || clean.equals("personalspace:ps_*")
                || clean.equals("personalspace:team_*")
                || clean.contains("personalspace:*")
                || clean.contains("personalspace:ps_*")
                || clean.contains("personalspace:team_*");
    }
}