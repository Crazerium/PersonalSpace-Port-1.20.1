package me.eigenraven.personalspace.compat.gtocore;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class GTOCoreAirCompat {

    private static final String GTOCORE_MODID = "gtocore";
    private static final String GTCEU_AIR_ID = "gtceu:air";
    private static final String INFINITE_INTAKE_CLASS =
            "com.gtocore.common.machine.multiblock.part.InfiniteIntakeHatchPartMachine";

    private static final Set<ResourceLocation> REGISTERED_DIMENSIONS = new HashSet<>();

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }

        if (level.isClientSide()) {
            return;
        }

        ResourceKey<Level> dimensionKey = level.dimension();
        ResourceLocation dimensionId = dimensionKey.location();

        if (!isPersonalSpaceDimension(dimensionId)) {
            return;
        }

        registerAirForDimension(dimensionKey);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerAirForDimension(ResourceKey<Level> dimensionKey) {
        if (!ModList.get().isLoaded(GTOCORE_MODID)) {
            return;
        }

        ResourceLocation dimensionId = dimensionKey.location();

        if (REGISTERED_DIMENSIONS.contains(dimensionId)) {
            return;
        }

        ResourceLocation airId = ResourceLocation.tryParse(GTCEU_AIR_ID);

        if (airId == null) {
            return;
        }

        Fluid airFluid = ForgeRegistries.FLUIDS.getValue(airId);

        if (airFluid == null || airFluid == Fluids.EMPTY) {
            PersonalSpace.LOGGER.warn(
                    "Could not register GTOCore air for Personal Space dimension {}: fluid {} not found",
                    dimensionId,
                    GTCEU_AIR_ID
            );
            return;
        }

        try {
            Class<?> hatchClass = Class.forName(INFINITE_INTAKE_CLASS);

            Field airMapField = hatchClass.getDeclaredField("AIR_MAP");
            airMapField.setAccessible(true);

            Object airMapObject = airMapField.get(null);

            if (!(airMapObject instanceof Map airMap)) {
                PersonalSpace.LOGGER.warn(
                        "Could not register GTOCore air for Personal Space dimension {}: AIR_MAP is not a Map",
                        dimensionId
                );
                return;
            }

            airMap.put(dimensionKey, airFluid);
            REGISTERED_DIMENSIONS.add(dimensionId);
        } catch (Throwable throwable) {
            PersonalSpace.LOGGER.warn(
                    "Could not register GTOCore air for Personal Space dimension {}",
                    dimensionId,
                    throwable
            );
        }
    }

    private static boolean isPersonalSpaceDimension(ResourceLocation dimensionId) {
        if (dimensionId == null) {
            return false;
        }

        return "personalspace".equals(dimensionId.getNamespace())
                && dimensionId.getPath().startsWith("personal_space_dimensions/");
    }
}