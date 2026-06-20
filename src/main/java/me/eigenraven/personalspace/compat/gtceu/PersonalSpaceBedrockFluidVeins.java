package me.eigenraven.personalspace.compat.gtceu;

import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PSDimensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PersonalSpaceBedrockFluidVeins {
    private static final Set<BedrockFluidDefinition> PERSONAL_SPACE_WILDCARD_VEINS = new HashSet<>();
    private static boolean initialized = false;
    private static final ResourceKey<Level> PERSONAL_SPACE_WILDCARD_PLACEHOLDER =
            ResourceKey.create(
                    Registries.DIMENSION,
                    new ResourceLocation(PersonalSpace.MODID, "ps_wildcard_placeholder")
            );
    private PersonalSpaceBedrockFluidVeins() {
    }


    private static final Set<ResourceLocation> GTO_STANDARD_VOID_FLUIDS = Set.of(
            new ResourceLocation("gtceu", "oil_heavy"),
            new ResourceLocation("gtceu", "oil_medium"),
            new ResourceLocation("gtceu", "oil_light"),
            new ResourceLocation("gtceu", "oil"),
            new ResourceLocation("gtceu", "natural_gas"),
            new ResourceLocation("gtceu", "salt_water")
    );

    public static void addPersonalSpaceDimensionToExistingGTCEuVeins(ResourceKey<Level> levelKey) {
        if (levelKey == null || !PSDimensions.isPersonalSpaceDimension(levelKey.location())) {
            return;
        }

        int patched = 0;

        for (BedrockFluidDefinition definition : GTRegistries.BEDROCK_FLUID_DEFINITIONS.values()) {
            ResourceLocation fluidId;

            try {
                Fluid fluid = definition.getStoredFluid().get();
                fluidId = BuiltInRegistries.FLUID.getKey(fluid);
            } catch (Exception exception) {
                continue;
            }

            if (!GTO_STANDARD_VOID_FLUIDS.contains(fluidId)) {
                continue;
            }

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

        PersonalSpace.LOGGER.info(
                "Added Personal Space dimension '{}' to {} existing GTCEu/GTO matching bedrock fluid vein(s).",
                levelKey.location(),
                patched
        );
    }

    public static void init() {
        if (initialized) {
            PersonalSpace.LOGGER.debug("GTCEu bedrock fluid veins were already initialized, skipping duplicate init.");
            return;
        }

        initialized = true;

        if (!PersonalSpaceGTCEuConfig.ENABLED.get()) {
            return;
        }

        List<? extends String> veins = PersonalSpaceGTCEuConfig.BEDROCK_FLUID_VEINS.get();

        for (String rawEntry : veins) {
            registerFromConfig(rawEntry);
        }
    }

    public static void addPersonalSpaceDimension(ResourceKey<Level> levelKey) {
        if (levelKey == null || !PSDimensions.isPersonalSpaceDimension(levelKey.location())) {
            return;
        }

        for (BedrockFluidDefinition definition : PERSONAL_SPACE_WILDCARD_VEINS) {
            Set<ResourceKey<Level>> dimensionFilter = definition.getDimensionFilter();

            if (dimensionFilter == null) {
                dimensionFilter = new HashSet<>();
                definition.setDimensionFilter(dimensionFilter);
            }

            if (!(dimensionFilter instanceof HashSet)) {
                dimensionFilter = new HashSet<>(dimensionFilter);
                definition.setDimensionFilter(dimensionFilter);
            }

            dimensionFilter.add(levelKey);
        }

        if (!PERSONAL_SPACE_WILDCARD_VEINS.isEmpty()) {
            PersonalSpace.LOGGER.info(
                    "Added Personal Space dimension '{}' to {} GTCEu bedrock fluid vein(s).",
                    levelKey.location(),
                    PERSONAL_SPACE_WILDCARD_VEINS.size()
            );
        }
    }

    private static void registerFromConfig(String rawEntry) {
        if (rawEntry == null || rawEntry.isBlank()) {
            return;
        }

        String[] parts = rawEntry.split("\\|");

        if (parts.length != 9) {
            PersonalSpace.LOGGER.warn(
                    "Invalid GTCEu bedrock fluid vein config entry '{}'. Expected 9 parts.",
                    rawEntry
            );
            return;
        }

        ResourceLocation veinId = ResourceLocation.tryParse(parts[0].trim());
        ResourceLocation fluidId = ResourceLocation.tryParse(parts[1].trim());

        if (veinId == null || fluidId == null) {
            PersonalSpace.LOGGER.warn(
                    "Invalid GTCEu bedrock fluid vein ids in entry '{}'.",
                    rawEntry
            );
            return;
        }

        boolean personalSpaceWildcard = hasPersonalSpaceWildcard(parts[2]);
        Set<ResourceKey<Level>> dimensions = parseDimensions(parts[2]);

        if (personalSpaceWildcard) {
            dimensions.add(PERSONAL_SPACE_WILDCARD_PLACEHOLDER);
        }

        if (dimensions.isEmpty() && !personalSpaceWildcard) {
            PersonalSpace.LOGGER.warn(
                    "GTCEu bedrock fluid vein '{}' has no valid dimensions.",
                    veinId
            );
            return;
        }

        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);



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

        BedrockFluidDefinition definition = BedrockFluidDefinition.builder(veinId)
                .dimensions(new HashSet<>(dimensions))
                .fluid(() -> BuiltInRegistries.FLUID.get(fluidId))
                .weight(weight)
                .yield(minYield, maxYield)
                .depletionAmount(depletionAmount)
                .depletionChance(depletionChance)
                .depletedYield(depletedYield)
                .register();

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

            result.add(ResourceKey.create(Registries.DIMENSION, dimensionId));
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