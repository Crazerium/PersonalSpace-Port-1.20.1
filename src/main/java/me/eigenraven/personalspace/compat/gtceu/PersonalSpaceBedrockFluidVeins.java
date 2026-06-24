package me.eigenraven.personalspace.compat.gtceu;

import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.worldgen.BiomeWeightModifier;
import com.gregtechceu.gtceu.api.worldgen.bedrockfluid.BedrockFluidDefinition;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PSDimensions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PersonalSpaceBedrockFluidVeins {
    private static final Set<ResourceKey<Level>> PERSONAL_SPACE_DIMENSIONS = new HashSet<>();
    private static final Set<BedrockFluidDefinition> PERSONAL_SPACE_WILDCARD_VEINS = new HashSet<>();

    private static boolean initialized = false;

    private static final List<String> DEFAULT_BEDROCK_FLUID_VEINS = List.of(
            "personalspace:void_oil_deposit|gtceu:oil|personalspace:ps_*|99|120|720|2|1|50",
            "personalspace:void_natural_gas_deposit|gtceu:natural_gas|personalspace:ps_*|30|80|400|2|1|30"
    );

    private PersonalSpaceBedrockFluidVeins() {
    }

    public static void init() {
        if (initialized) {
            PersonalSpace.LOGGER.debug("GTCEu bedrock fluid veins were already initialized, skipping duplicate init.");
            return;
        }

        initialized = true;

        List<String> veins = getConfiguredVeinsSafely();

        if (veins.isEmpty()) {
            PersonalSpace.LOGGER.info("No Personal Space GTCEu bedrock fluid veins are configured.");
            return;
        }

        Registry<BedrockFluidDefinition> registry;

        try {
            registry = GTRegistries.builtinRegistry().registryOrThrow(GTRegistries.BEDROCK_FLUID_REGISTRY);
        } catch (Exception exception) {
            PersonalSpace.LOGGER.warn("Failed to access GTCEu bedrock fluid registry.", exception);
            return;
        }

        int registered = 0;

        for (String rawEntry : veins) {
            if (registerFromConfig(registry, rawEntry)) {
                registered++;
            }
        }

        PersonalSpace.LOGGER.info(
                "Registered {} Personal Space GTCEu bedrock fluid vein(s).",
                registered
        );
    }

    public static void addPersonalSpaceDimension(ResourceKey<Level> levelKey) {
        if (levelKey == null || !PSDimensions.isPersonalSpaceDimension(levelKey.location())) {
            return;
        }

        if (PERSONAL_SPACE_DIMENSIONS.add(levelKey)) {
            PersonalSpace.LOGGER.info(
                    "Tracked Personal Space dimension '{}' for GTCEu bedrock fluid veins.",
                    levelKey.location()
            );
        }

        int patched = 0;

        for (BedrockFluidDefinition definition : PERSONAL_SPACE_WILDCARD_VEINS) {
            Set<ResourceKey<Level>> dimensionFilter = definition.getDimensionFilter();

            if (dimensionFilter == null) {
                dimensionFilter = new HashSet<>();
            } else {
                dimensionFilter = new HashSet<>(dimensionFilter);
            }

            if (dimensionFilter.add(levelKey)) {
                patched++;
            }

            definition.setDimensionFilter(dimensionFilter);
        }

        if (patched > 0) {
            PersonalSpace.LOGGER.info(
                    "Added Personal Space dimension '{}' to {} Personal Space GTCEu bedrock fluid vein(s).",
                    levelKey.location(),
                    patched
            );
        }
    }

    public static void addPersonalSpaceDimensionToExistingGTCEuVeins(ResourceKey<Level> levelKey) {
        addPersonalSpaceDimension(levelKey);
    }

    private static boolean registerFromConfig(
            Registry<BedrockFluidDefinition> registry,
            String rawEntry
    ) {
        if (rawEntry == null || rawEntry.isBlank()) {
            return false;
        }

        String[] parts = rawEntry.split("\\|");

        if (parts.length != 9) {
            PersonalSpace.LOGGER.warn(
                    "Invalid GTCEu bedrock fluid vein config entry '{}'. Expected 9 parts.",
                    rawEntry
            );
            return false;
        }

        ResourceLocation veinId = ResourceLocation.tryParse(parts[0].trim());
        ResourceLocation fluidId = ResourceLocation.tryParse(parts[1].trim());

        if (veinId == null || fluidId == null) {
            PersonalSpace.LOGGER.warn(
                    "Invalid GTCEu bedrock fluid vein ids in entry '{}'.",
                    rawEntry
            );
            return false;
        }

        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);

        if (fluid == null || BuiltInRegistries.FLUID.getKey(fluid).equals(ResourceLocation.fromNamespaceAndPath("minecraft", "empty"))) {
            PersonalSpace.LOGGER.warn(
                    "Unknown fluid '{}' for GTCEu bedrock fluid vein '{}'.",
                    fluidId,
                    veinId
            );
            return false;
        }

        boolean personalSpaceWildcard = hasPersonalSpaceWildcard(parts[2]);
        Set<ResourceKey<Level>> dimensions = parseDimensions(parts[2]);

        if (personalSpaceWildcard) {
            dimensions.addAll(PERSONAL_SPACE_DIMENSIONS);
        }

        if (dimensions.isEmpty()) {
            PersonalSpace.LOGGER.warn(
                    "GTCEu bedrock fluid vein '{}' has no valid dimensions yet. It will be registered and patched when Personal Space dimensions are created.",
                    veinId
            );
        }

        int weight = parseInt(parts[3], 20);
        int minYield = parseInt(parts[4], 120);
        int maxYield = parseInt(parts[5], 720);
        int depletionAmount = parseInt(parts[6], 2);
        int depletionChance = parseInt(parts[7], 1);
        int depletedYield = parseInt(parts[8], 50);

        if (minYield > maxYield) {
            int temp = minYield;
            minYield = maxYield;
            maxYield = temp;
        }

        BedrockFluidDefinition definition = new BedrockFluidDefinition(
                weight,
                minYield,
                maxYield,
                depletionAmount,
                depletionChance,
                depletedYield,
                fluid,
                BiomeWeightModifier.EMPTY,
                new HashSet<>(dimensions)
        );

        try {
            GTRegistries.register(
                    registry,
                    veinId,
                    definition
            );
        } catch (Exception exception) {
            PersonalSpace.LOGGER.warn(
                    "Failed to register GTCEu bedrock fluid vein '{}'.",
                    veinId,
                    exception
            );
            return false;
        }

        if (personalSpaceWildcard) {
            PERSONAL_SPACE_WILDCARD_VEINS.add(definition);
        }

        PersonalSpace.LOGGER.info(
                "Registered GTCEu bedrock fluid vein '{}' with fluid '{}' in {} dimension(s){}.",
                veinId,
                fluidId,
                dimensions.size(),
                personalSpaceWildcard ? " with Personal Space wildcard support" : ""
        );

        return true;
    }

    private static List<String> getConfiguredVeinsSafely() {
        try {
            if (!PersonalSpaceGTCEuConfig.ENABLED.get()) {
                return List.of();
            }

            List<? extends String> configured = PersonalSpaceGTCEuConfig.BEDROCK_FLUID_VEINS.get();

            if (configured == null || configured.isEmpty()) {
                return DEFAULT_BEDROCK_FLUID_VEINS;
            }

            List<String> result = new ArrayList<>();

            for (String entry : configured) {
                if (entry != null && !entry.isBlank()) {
                    result.add(entry);
                }
            }

            return result.isEmpty() ? DEFAULT_BEDROCK_FLUID_VEINS : result;
        } catch (IllegalStateException exception) {
            PersonalSpace.LOGGER.info(
                    "Personal Space GTCEu config is not loaded yet, using built-in default bedrock fluid veins."
            );
            return DEFAULT_BEDROCK_FLUID_VEINS;
        } catch (Exception exception) {
            PersonalSpace.LOGGER.warn(
                    "Failed to read Personal Space GTCEu config, using built-in default bedrock fluid veins.",
                    exception
            );
            return DEFAULT_BEDROCK_FLUID_VEINS;
        }
    }

    private static boolean hasPersonalSpaceWildcard(String rawDimensions) {
        if (rawDimensions == null || rawDimensions.isBlank()) {
            return false;
        }

        String[] dimensionIds = rawDimensions.split(",");

        for (String rawDimensionId : dimensionIds) {
            String value = rawDimensionId.trim();

            if (value.equals("personalspace:*")
                    || value.equals("personalspace:ps_*")) {
                return true;
            }
        }

        return false;
    }

    private static Set<ResourceKey<Level>> parseDimensions(String rawDimensions) {
        Set<ResourceKey<Level>> result = new HashSet<>();

        if (rawDimensions == null || rawDimensions.isBlank()) {
            return result;
        }

        String[] dimensionIds = rawDimensions.split(",");

        for (String rawDimensionId : dimensionIds) {
            String value = rawDimensionId.trim();

            if (value.equals("personalspace:*")
                    || value.equals("personalspace:ps_*")) {
                continue;
            }

            ResourceLocation dimensionId = ResourceLocation.tryParse(value);

            if (dimensionId == null) {
                continue;
            }

            result.add(ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    dimensionId
            ));
        }

        return result;
    }

    private static int parseInt(String rawValue, int fallback) {
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}