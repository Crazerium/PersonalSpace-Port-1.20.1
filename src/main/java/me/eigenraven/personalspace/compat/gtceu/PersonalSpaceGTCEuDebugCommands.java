package me.eigenraven.personalspace.compat.gtceu;

import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.mojang.brigadier.CommandDispatcher;
import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public final class PersonalSpaceGTCEuDebugCommands {
    private PersonalSpaceGTCEuDebugCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ps_debug_fluids")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            ResourceKey<Level> currentDimension = source.getLevel().dimension();
                            ResourceLocation currentDimensionId = currentDimension.location();

                            source.sendSuccess(
                                    () -> Component.literal("§e[PS/GTCEu] Current dimension: §f" + currentDimensionId),
                                    false
                            );

                            int total = 0;
                            int matched = 0;

                            for (BedrockFluidDefinition definition : GTRegistries.BEDROCK_FLUID_DEFINITIONS.values()) {
                                total++;

                                ResourceLocation definitionId = getDefinitionIdSafe(definition);
                                String fluidInfo = getFluidInfoSafe(definition);
                                Set<ResourceKey<Level>> dimensionFilter = getDimensionFilterSafe(definition);

                                boolean matches = dimensionFilter != null && dimensionFilter.contains(currentDimension);

                                if (matches) {
                                    matched++;

                                    source.sendSuccess(
                                            () -> Component.literal(
                                                    "§a[PS/GTCEu] MATCH "
                                                            + "id=§f" + definitionId
                                                            + " §bfluid=" + fluidInfo
                                                            + " §7filterSize=" + (dimensionFilter == null ? -1 : dimensionFilter.size())
                                            ),
                                            false
                                    );

                                    source.sendSuccess(
                                            () -> Component.literal(
                                                    "§7[PS/GTCEu] MATCH class=" + definition.getClass().getName()
                                            ),
                                            false
                                    );
                                }

                                if (definitionId != null && definitionId.getNamespace().equals(PersonalSpace.MODID)) {
                                    source.sendSuccess(
                                            () -> Component.literal(
                                                    "§b[PS/GTCEu] PersonalSpace definition "
                                                            + "id=§f" + definitionId
                                                            + " §bfluid=" + fluidInfo
                                                            + " §7matchesCurrent=" + matches
                                                            + " filterSize=" + (dimensionFilter == null ? -1 : dimensionFilter.size())
                                            ),
                                            false
                                    );
                                }
                            }

                            int finalTotal = total;
                            int finalMatched = matched;

                            source.sendSuccess(
                                    () -> Component.literal(
                                            "§e[PS/GTCEu] Total bedrock fluid definitions: §f" + finalTotal
                                                    + "§e, matched current dimension: §f" + finalMatched
                                    ),
                                    false
                            );

                            return 1;
                        })
        );
    }

    private static ResourceLocation getDefinitionIdSafe(BedrockFluidDefinition definition) {
        if (definition == null) {
            return null;
        }

        String[] methodNames = {
                "id",
                "getId",
                "name",
                "getName",
                "getRegistryName"
        };

        for (String methodName : methodNames) {
            try {
                Method method = definition.getClass().getMethod(methodName);
                Object result = method.invoke(definition);

                if (result instanceof ResourceLocation resourceLocation) {
                    return resourceLocation;
                }
            } catch (Throwable ignored) {
            }
        }

        String[] fieldNames = {
                "id",
                "name",
                "registryName",
                "location"
        };

        for (String fieldName : fieldNames) {
            try {
                Field field = definition.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);

                Object result = field.get(definition);

                if (result instanceof ResourceLocation resourceLocation) {
                    return resourceLocation;
                }
            } catch (Throwable ignored) {
            }
        }

        for (Field field : definition.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);

                Object result = field.get(definition);

                if (result instanceof ResourceLocation resourceLocation) {
                    return resourceLocation;
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static String getFluidInfoSafe(BedrockFluidDefinition definition) {
        if (definition == null) {
            return "null";
        }

        /*
         * Сначала пробуем методы.
         */
        String[] methodNames = {
                "fluid",
                "getFluid",
                "getFluidSupplier",
                "fluidSupplier"
        };

        for (String methodName : methodNames) {
            try {
                Method method = definition.getClass().getMethod(methodName);
                Object result = method.invoke(definition);

                String converted = convertFluidResultToString(result);

                if (!converted.equals("unknown")) {
                    return converted;
                }
            } catch (Throwable ignored) {
            }
        }

        /*
         * Потом пробуем ожидаемые поля.
         */
        String[] fieldNames = {
                "fluid",
                "fluidSupplier",
                "fluidGetter",
                "fluidStack"
        };

        for (String fieldName : fieldNames) {
            try {
                Field field = definition.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);

                Object result = field.get(definition);
                String converted = convertFluidResultToString(result);

                if (!converted.equals("unknown")) {
                    return converted;
                }
            } catch (Throwable ignored) {
            }
        }

        /*
         * Потом перебираем все поля и ищем Fluid / Supplier<Fluid>.
         */
        for (Field field : definition.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);

                Object result = field.get(definition);
                String converted = convertFluidResultToString(result);

                if (!converted.equals("unknown")) {
                    return field.getName() + "=" + converted;
                }
            } catch (Throwable ignored) {
            }
        }

        return "unknown";
    }

    private static String convertFluidResultToString(Object result) {
        if (result == null) {
            return "null";
        }

        if (result instanceof Fluid fluid) {
            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
            return String.valueOf(id);
        }

        if (result instanceof Supplier<?> supplier) {
            Object supplied;

            try {
                supplied = supplier.get();
            } catch (Throwable throwable) {
                return "supplier_error=" + throwable.getClass().getSimpleName();
            }

            if (supplied instanceof Fluid fluid) {
                ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
                return String.valueOf(id);
            }

            return "supplier=" + supplied;
        }

        return "unknown";
    }

    @SuppressWarnings("unchecked")
    private static Set<ResourceKey<Level>> getDimensionFilterSafe(BedrockFluidDefinition definition) {
        if (definition == null) {
            return null;
        }

        String[] methodNames = {
                "dimensionFilter",
                "getDimensionFilter",
                "dimensions",
                "getDimensions"
        };

        for (String methodName : methodNames) {
            try {
                Method method = definition.getClass().getMethod(methodName);
                Object result = method.invoke(definition);

                if (result instanceof Set<?> set) {
                    return (Set<ResourceKey<Level>>) set;
                }

                if (result instanceof Collection<?> collection) {
                    return Set.copyOf((Collection<ResourceKey<Level>>) collection);
                }
            } catch (Throwable ignored) {
            }
        }

        String[] fieldNames = {
                "dimensionFilter",
                "dimensions"
        };

        for (String fieldName : fieldNames) {
            try {
                Field field = definition.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);

                Object result = field.get(definition);

                if (result instanceof Set<?> set) {
                    return (Set<ResourceKey<Level>>) set;
                }

                if (result instanceof Collection<?> collection) {
                    return Set.copyOf((Collection<ResourceKey<Level>>) collection);
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }
}