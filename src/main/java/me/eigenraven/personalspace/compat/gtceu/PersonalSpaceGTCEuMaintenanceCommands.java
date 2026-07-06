package me.eigenraven.personalspace.compat.gtceu;

import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.FluidVeinWorldEntry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class PersonalSpaceGTCEuMaintenanceCommands {
    private PersonalSpaceGTCEuMaintenanceCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ps_gtceu_fluids")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("check_saved")
                                .executes(context -> checkSaved(context.getSource())))

                        .then(Commands.literal("repair_empty_saved")
                                .executes(context -> repairEmptySaved(context.getSource())))

                        .then(Commands.literal("repair_empty_here")
                                .then(Commands.argument("radius_chunks", IntegerArgumentType.integer(0, 128))
                                        .executes(context -> repairEmptyHere(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "radius_chunks")
                                        ))))
        );
    }

    private static int checkSaved(CommandSourceStack source) {
        say(source, "§e[PS/GTCEu] check_saved started.");

        try {
            ServerLevel level = source.getLevel();

            say(source, "§e[PS/GTCEu] current dimension=" + level.dimension().location());

            if (!isPersonalSpaceLevel(level)) {
                say(source, "§c[PS/GTCEu] Эта команда работает только в Personal Space измерениях. Сейчас: " + level.dimension().location());
                return 0;
            }

            say(source, "§e[PS/GTCEu] loading GTCEu BedrockFluidVeinSavedData...");

            BedrockFluidVeinSavedData data = BedrockFluidVeinSavedData.getOrCreate(level);

            if (data == null) {
                say(source, "§c[PS/GTCEu] BedrockFluidVeinSavedData is null.");
                return 0;
            }

            Map<?, FluidVeinWorldEntry> veinMap = getVeinFluidMap(data);

            if (veinMap == null) {
                say(source, "§c[PS/GTCEu] Could not find GTCEu vein fluid map by reflection.");
                return 0;
            }

            say(source, "§e[PS/GTCEu] saved data loaded. entries=" + veinMap.size());

            int total = 0;
            int empty = 0;
            int nullDefinition = 0;
            int normal = 0;

            for (Map.Entry<?, FluidVeinWorldEntry> entry : veinMap.entrySet()) {
                total++;

                FluidStateInfo info = getFluidStateInfo(entry.getValue());

                if (info.nullDefinition()) {
                    nullDefinition++;
                } else if (info.empty()) {
                    empty++;
                } else {
                    normal++;
                }
            }

            say(source,
                    "§e[PS/GTCEu] Saved entries: total="
                            + total
                            + ", normal=" + normal
                            + ", empty=" + empty
                            + ", nullDefinition=" + nullDefinition
            );

            PersonalSpace.LOGGER.warn(
                    "[PS/GTCEu] check_saved in {}: total={}, normal={}, empty={}, nullDefinition={}",
                    level.dimension().location(),
                    total,
                    normal,
                    empty,
                    nullDefinition
            );

            return Math.max(total, 1);
        } catch (Throwable throwable) {
            say(source, "§c[PS/GTCEu] check_saved crashed: " + throwable.getClass().getName() + ": " + throwable.getMessage());

            PersonalSpace.LOGGER.warn(
                    "[PS/GTCEu] check_saved crashed.",
                    throwable
            );

            return 0;
        }
    }

    private static int repairEmptySaved(CommandSourceStack source) {
        say(source, "§e[PS/GTCEu] repair_empty_saved started.");

        try {
            ServerLevel level = source.getLevel();

            if (!isPersonalSpaceLevel(level)) {
                say(source, "§c[PS/GTCEu] Эта команда работает только в Personal Space измерениях. Сейчас: " + level.dimension().location());
                return 0;
            }

            BedrockFluidVeinSavedData data = BedrockFluidVeinSavedData.getOrCreate(level);
            Map<?, FluidVeinWorldEntry> veinMap = getVeinFluidMap(data);

            if (veinMap == null) {
                say(source, "§c[PS/GTCEu] Could not find GTCEu vein fluid map by reflection.");
                return 0;
            }

            int before = veinMap.size();
            int removed = removeEmptyEntries(veinMap);
            int after = veinMap.size();

            if (removed > 0) {
                data.setDirty();
            }

            say(source,
                    "§a[PS/GTCEu] repair_empty_saved done. before="
                            + before
                            + ", removed=" + removed
                            + ", after=" + after
                            + ". Removed entries will regenerate on next scan/use."
            );

            PersonalSpace.LOGGER.warn(
                    "[PS/GTCEu] repair_empty_saved in {}: before={}, removed={}, after={}",
                    level.dimension().location(),
                    before,
                    removed,
                    after
            );

            return Math.max(removed, 1);
        } catch (Throwable throwable) {
            say(source, "§c[PS/GTCEu] repair_empty_saved crashed: " + throwable.getClass().getName() + ": " + throwable.getMessage());

            PersonalSpace.LOGGER.warn(
                    "[PS/GTCEu] repair_empty_saved crashed.",
                    throwable
            );

            return 0;
        }
    }

    private static int repairEmptyHere(CommandSourceStack source, int radiusChunks) {
        say(source, "§e[PS/GTCEu] repair_empty_here started. radius=" + radiusChunks);

        try {
            ServerLevel level = source.getLevel();

            if (!isPersonalSpaceLevel(level)) {
                say(source, "§c[PS/GTCEu] Эта команда работает только в Personal Space измерениях. Сейчас: " + level.dimension().location());
                return 0;
            }

            BedrockFluidVeinSavedData data = BedrockFluidVeinSavedData.getOrCreate(level);
            Map<?, FluidVeinWorldEntry> veinMap = getVeinFluidMap(data);

            if (veinMap == null) {
                say(source, "§c[PS/GTCEu] Could not find GTCEu vein fluid map by reflection.");
                return 0;
            }

            int centerChunkX = ((int) Math.floor(source.getPosition().x)) >> 4;
            int centerChunkZ = ((int) Math.floor(source.getPosition().z)) >> 4;

            int minX = centerChunkX - radiusChunks;
            int maxX = centerChunkX + radiusChunks;
            int minZ = centerChunkZ - radiusChunks;
            int maxZ = centerChunkZ + radiusChunks;

            List<Object> toRemove = new ArrayList<>();

            for (Map.Entry<?, FluidVeinWorldEntry> entry : veinMap.entrySet()) {
                Object rawKey = entry.getKey();

                ChunkPos pos = keyToChunkPos(rawKey);

                if (pos == null) {
                    continue;
                }

                if (pos.x < minX || pos.x > maxX || pos.z < minZ || pos.z > maxZ) {
                    continue;
                }

                FluidStateInfo info = getFluidStateInfo(entry.getValue());

                if (info.empty() || info.nullDefinition()) {
                    toRemove.add(rawKey);
                }
            }

            for (Object key : toRemove) {
                veinMap.remove(key);
            }

            if (!toRemove.isEmpty()) {
                data.setDirty();
            }

            int removed = toRemove.size();

            say(source,
                    "§a[PS/GTCEu] repair_empty_here done. centerChunk="
                            + centerChunkX + "," + centerChunkZ
                            + ", radius=" + radiusChunks
                            + ", removed=" + removed
                            + ". Removed entries will regenerate on next scan/use."
            );

            PersonalSpace.LOGGER.warn(
                    "[PS/GTCEu] repair_empty_here in {}: center={},{} radius={} removed={}",
                    level.dimension().location(),
                    centerChunkX,
                    centerChunkZ,
                    radiusChunks,
                    removed
            );

            return Math.max(removed, 1);
        } catch (Throwable throwable) {
            say(source, "§c[PS/GTCEu] repair_empty_here crashed: " + throwable.getClass().getName() + ": " + throwable.getMessage());

            PersonalSpace.LOGGER.warn(
                    "[PS/GTCEu] repair_empty_here crashed.",
                    throwable
            );

            return 0;
        }
    }

    private static ChunkPos keyToChunkPos(Object key) {
        if (key instanceof ChunkPos chunkPos) {
            return chunkPos;
        }

        if (key instanceof Long longKey) {
            return new ChunkPos(longKey);
        }

        return null;
    }

    private static int removeEmptyEntries(Map<?, FluidVeinWorldEntry> veinMap) {
        int removed = 0;

        Iterator<? extends Map.Entry<?, FluidVeinWorldEntry>> iterator = veinMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<?, FluidVeinWorldEntry> entry = iterator.next();

            FluidStateInfo info = getFluidStateInfo(entry.getValue());

            if (info.empty() || info.nullDefinition()) {
                iterator.remove();
                removed++;
            }
        }

        return removed;
    }

    private static FluidStateInfo getFluidStateInfo(FluidVeinWorldEntry entry) {
        if (entry == null || entry.getDefinition() == null) {
            return new FluidStateInfo(null, true, true);
        }

        Fluid fluid;

        try {
            fluid = entry.getDefinition().getStoredFluid().get();
        } catch (Throwable throwable) {
            return new FluidStateInfo(null, true, true);
        }

        if (fluid == null) {
            return new FluidStateInfo(null, true, true);
        }

        ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluid);

        boolean empty = fluid == Fluids.EMPTY
                || fluidId == null
                || fluidId.equals(new ResourceLocation("minecraft", "empty"));

        return new FluidStateInfo(fluidId, empty, false);
    }

    @SuppressWarnings("unchecked")
    private static Map<?, FluidVeinWorldEntry> getVeinFluidMap(BedrockFluidVeinSavedData data) {
        if (data == null) {
            return null;
        }

        String[] preferredNames = {
                "veinFluids",
                "fluidVeins",
                "veins",
                "veinInfo"
        };

        Class<?> clazz = data.getClass();

        for (String fieldName : preferredNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);

                Object value = field.get(data);

                if (value instanceof Map<?, ?> map) {
                    return (Map<?, FluidVeinWorldEntry>) map;
                }
            } catch (Throwable ignored) {
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);

                Object value = field.get(data);

                if (!(value instanceof Map<?, ?> map)) {
                    continue;
                }

                boolean looksLikeVeinMap = true;

                for (Object key : map.keySet()) {
                    if (!(key instanceof ChunkPos) && !(key instanceof Long)) {
                        looksLikeVeinMap = false;
                        break;
                    }
                }

                if (looksLikeVeinMap) {
                    return (Map<?, FluidVeinWorldEntry>) map;
                }
            } catch (Throwable ignored) {
            }
        }

        PersonalSpace.LOGGER.warn(
                "[PS/GTCEu] Failed to locate vein map in {}.",
                clazz.getName()
        );

        for (Field field : clazz.getDeclaredFields()) {
            PersonalSpace.LOGGER.warn(
                    "[PS/GTCEu] BedrockFluidVeinSavedData field: {} {}",
                    field.getType().getName(),
                    field.getName()
            );
        }

        return null;
    }

    private static boolean isPersonalSpaceLevel(ServerLevel level) {
        if (level == null) {
            return false;
        }

        ResourceLocation dimensionId = level.dimension().location();

        return dimensionId.getNamespace().equals(PersonalSpace.MODID)
                && dimensionId.getPath().startsWith("personal_space_dimensions/");
    }

    private static void say(CommandSourceStack source, String message) {
        PersonalSpace.LOGGER.warn(message.replace("§", "&"));

        try {
            ServerPlayer player = source.getPlayerOrException();
            player.sendSystemMessage(Component.literal(message));
        } catch (Throwable ignored) {
            source.sendSystemMessage(Component.literal(message));
        }
    }

    private record FluidStateInfo(ResourceLocation fluidId, boolean empty, boolean nullDefinition) {
    }
}