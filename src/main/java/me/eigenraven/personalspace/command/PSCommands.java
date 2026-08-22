package me.eigenraven.personalspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.compat.ftbteams.FTBTeamsCompat;
import me.eigenraven.personalspace.config.PersonalSpacePublicInteractionConfig;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.dimension.PersonalSpaceDeletionManager;
import me.eigenraven.personalspace.registry.PSItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class PSCommands {
    private static final BlockPos FALLBACK_PORTAL_TARGET_POS = new BlockPos(7, 66, 7);

    private PSCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("personalspace")
                .then(Commands.literal("delete")
                        .then(Commands.literal("approve")
                                .executes(context -> PersonalSpaceDeletionManager.approveDelete(context.getSource())))
                        .then(Commands.literal("cancel")
                                .executes(context -> PersonalSpaceDeletionManager.cancelDelete(context.getSource()))))
                .then(Commands.literal("dimension")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        getPersonalSpaceDimensionSuggestions(context.getSource()), builder))
                                .then(Commands.literal("tp")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> teleportToDimension(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "name"))))
                                .then(Commands.literal("giveportal")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> givePortal(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "name"),
                                                context.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> givePortal(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name"),
                                                        EntityArgument.getPlayer(context, "player")))))
                                .then(Commands.literal("respawn")
                                        .then(Commands.literal("get")
                                                .requires(source -> source.hasPermission(2))
                                                .executes(context -> getRespawn(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name"))))
                                        .then(Commands.literal("set")
                                                .executes(context -> setRespawn(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name"))))
                                        .then(Commands.literal("reset")
                                                .requires(source -> source.hasPermission(2))
                                                .executes(context -> resetRespawn(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name")))))
                                .then(Commands.literal("delete")
                                        .requires(source -> source.hasPermission(3))
                                        .then(Commands.literal("confirm")
                                                .executes(context -> deleteDimension(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name")))))))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reloadConfig(context.getSource())))
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(3))
                        .then(Commands.literal("chunks")
                                .executes(context -> debugChunks(context.getSource())))));
    }

    private static int reloadConfig(CommandSourceStack source) {
        if (!PersonalSpacePublicInteractionConfig.reload()) {
            source.sendFailure(Component.literal(
                    "Failed to reload PersonalSpace public interaction config. Check the server log."));
            return 0;
        }

        int count = PersonalSpacePublicInteractionConfig.allowedBlockCount();
        source.sendSuccess(
                () -> Component.literal("PersonalSpace config reloaded. Public interaction blocks: " + count),
                true);
        return 1;
    }

    private static int teleportToDimension(CommandSourceStack source, String dimensionName) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(translatable("player_only"));
            return 0;
        }

        MinecraftServer server = source.getServer();
        ResourceKey<Level> dimensionKey = getPersonalSpaceDimensionKey(dimensionName);
        ServerLevel destination = getOrCreatePersonalSpaceLevel(server, dimensionKey);

        if (destination == null) {
            source.sendFailure(translatable("dimension_not_found", dimensionKey.location()));
            return 0;
        }

        BlockPos targetPos = getRespawnPosForDimension(server, dimensionKey);
        destination.getChunkAt(targetPos);
        player.teleportTo(
                destination,
                targetPos.getX() + 0.5D,
                targetPos.getY(),
                targetPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot());

        source.sendSuccess(
                () -> translatable("teleported", dimensionKey.location(), formatPos(targetPos)),
                true);
        return 1;
    }

    private static int givePortal(CommandSourceStack source, String dimensionName, ServerPlayer targetPlayer) {
        MinecraftServer server = source.getServer();
        ResourceKey<Level> dimensionKey = getPersonalSpaceDimensionKey(dimensionName);
        BlockPos targetPos = getRespawnPosForDimension(server, dimensionKey);

        ItemStack stack = new ItemStack(PSItems.PERSONAL_PORTAL.get());
        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.putString(
                "id",
                ResourceLocation.fromNamespaceAndPath(PersonalSpace.MODID, "personal_portal").toString());
        blockEntityTag.putBoolean("Active", true);
        blockEntityTag.putBoolean("ReturnPortal", false);
        blockEntityTag.putString("TargetLevel", dimensionKey.location().toString());
        blockEntityTag.putLong("TargetPos", targetPos.asLong());

        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("Personal Portal: " + formatDimensionName(dimensionKey.location())));

        if (!targetPlayer.getInventory().add(stack)) {
            targetPlayer.drop(stack, false);
        }

        source.sendSuccess(
                () -> translatable(
                        "gave_portal",
                        dimensionKey.location(),
                        targetPlayer.getGameProfile().getName(),
                        formatPos(targetPos)),
                true);
        return 1;
    }

    private static int getRespawn(CommandSourceStack source, String dimensionName) {
        MinecraftServer server = source.getServer();
        ResourceKey<Level> dimensionKey = getPersonalSpaceDimensionKey(dimensionName);
        ServerLevel level = getOrCreatePersonalSpaceLevel(server, dimensionKey);

        if (level == null) {
            source.sendFailure(translatable("dimension_not_found", dimensionKey.location()));
            return 0;
        }

        PersonalSpaceData data = PersonalSpaceData.load(level);
        BlockPos respawnPos = data.getRespawnPos();
        source.sendSuccess(
                () -> translatable("respawn_get", dimensionKey.location(), formatPos(respawnPos)),
                false);
        return 1;
    }

    private static int setRespawn(CommandSourceStack source, String dimensionName) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(translatable("player_only"));
            return 0;
        }

        MinecraftServer server = source.getServer();
        ResourceKey<Level> dimensionKey = getPersonalSpaceDimensionKey(dimensionName);

        if (!source.hasPermission(2) && !canPlayerUsePersonalSpace(player, dimensionKey)) {
            source.sendFailure(translatable("no_personal_space_access"));
            return 0;
        }

        if (!player.level().dimension().equals(dimensionKey)) {
            source.sendFailure(translatable("respawn_set_wrong_dimension", dimensionKey.location()));
            return 0;
        }

        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            source.sendFailure(translatable("dimension_not_loaded", dimensionKey.location()));
            return 0;
        }

        BlockPos respawnPos = player.blockPosition();
        PersonalSpaceData data = PersonalSpaceData.load(level);
        data.setRespawnPos(respawnPos);
        PersonalSpaceData.save(level, data);

        source.sendSuccess(
                () -> translatable("respawn_set", dimensionKey.location(), formatPos(respawnPos)),
                true);
        return 1;
    }

    private static int resetRespawn(CommandSourceStack source, String dimensionName) {
        MinecraftServer server = source.getServer();
        ResourceKey<Level> dimensionKey = getPersonalSpaceDimensionKey(dimensionName);
        ServerLevel level = getOrCreatePersonalSpaceLevel(server, dimensionKey);

        if (level == null) {
            source.sendFailure(translatable("dimension_not_found", dimensionKey.location()));
            return 0;
        }

        PersonalSpaceData data = PersonalSpaceData.load(level);
        data.resetRespawnPos();
        PersonalSpaceData.save(level, data);
        BlockPos respawnPos = data.getRespawnPos();

        source.sendSuccess(
                () -> translatable("respawn_reset", dimensionKey.location(), formatPos(respawnPos)),
                true);
        return 1;
    }

    private static int deleteDimension(CommandSourceStack source, String dimensionName) {
        return PersonalSpaceDeletionManager.adminDelete(source, getPersonalSpaceDimensionKey(dimensionName));
    }

    private static ServerLevel getOrCreatePersonalSpaceLevel(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        ServerLevel level = server.getLevel(dimensionKey);
        return level != null ? level : PSDimensions.getOrCreate(server, dimensionKey);
    }

    private static BlockPos getRespawnPosForDimension(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            level = PSDimensions.getOrCreate(server, dimensionKey);
        }
        if (level == null) {
            return FALLBACK_PORTAL_TARGET_POS;
        }
        return PersonalSpaceData.load(level).getRespawnPos();
    }

    private static ResourceKey<Level> getPersonalSpaceDimensionKey(String rawName) {
        return ResourceKey.create(Registries.DIMENSION, parsePersonalSpaceDimensionId(rawName));
    }

    private static ResourceLocation parsePersonalSpaceDimensionId(String rawName) {
        String value = rawName.trim().toLowerCase(Locale.ROOT);
        if (value.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(value);
            if (parsed != null) {
                return parsed;
            }
        }

        String path = value;
        if (path.startsWith(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/")) {
            return ResourceLocation.fromNamespaceAndPath(PersonalSpace.MODID, path);
        }
        if (!path.startsWith("ps_") && !path.startsWith("team_")) {
            path = "ps_" + path;
        }
        return ResourceLocation.fromNamespaceAndPath(
                PersonalSpace.MODID,
                PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/" + path);
    }

    private static String formatDimensionName(ResourceLocation location) {
        if (location.getNamespace().equals(PersonalSpace.MODID)) {
            String path = location.getPath();
            if (path.startsWith(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/")) {
                path = path.substring((PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/").length());
            }
            if (path.startsWith("ps_")) {
                return path.substring(3);
            }
            if (path.startsWith("team_")) {
                return path.substring(5);
            }
            return path;
        }
        return location.toString();
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static List<String> getPersonalSpaceDimensionSuggestions(CommandSourceStack source) {
        List<String> suggestions = new ArrayList<>();
        MinecraftServer server = source.getServer();
        ServerPlayer player = null;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
        }
        boolean isOp = source.hasPermission(2);

        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dimensionKey = level.dimension();
            ResourceLocation location = dimensionKey.location();
            if (!location.getNamespace().equals(PersonalSpace.MODID)) {
                continue;
            }
            if (!isOp && !canPlayerUsePersonalSpace(player, dimensionKey)) {
                continue;
            }

            String path = location.getPath();
            if (path.startsWith(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/")) {
                path = path.substring((PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/").length());
            }
            if (path.startsWith("ps_") && path.length() > 3) {
                addSuggestionIfMissing(suggestions, path.substring(3));
            } else {
                addSuggestionIfMissing(suggestions, path);
            }
        }
        return suggestions;
    }

    private static boolean canPlayerUsePersonalSpace(ServerPlayer player, ResourceKey<Level> dimensionKey) {
        if (player == null || dimensionKey == null) {
            return false;
        }
        ResourceLocation location = dimensionKey.location();
        if (!location.getNamespace().equals(PersonalSpace.MODID)) {
            return false;
        }

        String path = location.getPath();
        if (path.startsWith(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/")) {
            path = path.substring((PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/").length());
        }

        String playerName = player.getGameProfile().getName().toLowerCase(Locale.ROOT);
        if (path.startsWith("ps_") && path.length() > 3) {
            return path.substring(3).toLowerCase(Locale.ROOT).equals(playerName);
        }
        if (path.startsWith("team_") && path.length() > 5) {
            return FTBTeamsCompat.isPlayerInTeam(player, path.substring(5));
        }
        return false;
    }

    private static void addSuggestionIfMissing(List<String> suggestions, String value) {
        if (value != null && !value.isBlank() && !suggestions.contains(value)) {
            suggestions.add(value);
        }
    }

    private static int debugChunks(CommandSourceStack source) {
        List<DimensionChunkInfo> infos = new ArrayList<>();
        int totalChunks = 0;
        int personalSpaceChunks = 0;
        int personalSpaceDimensions = 0;

        for (ServerLevel level : source.getServer().getAllLevels()) {
            ResourceLocation id = level.dimension().location();
            int loadedChunks = level.getChunkSource().getLoadedChunksCount();
            int players = level.players().size();
            boolean personalSpace = id.getNamespace().equals(PersonalSpace.MODID);

            totalChunks += Math.max(loadedChunks, 0);
            if (personalSpace) {
                personalSpaceDimensions++;
                personalSpaceChunks += Math.max(loadedChunks, 0);
            }
            infos.add(new DimensionChunkInfo(id.toString(), loadedChunks, players));
        }

        infos.sort(Comparator.comparingInt(DimensionChunkInfo::loadedChunks).reversed());
        int finalTotalChunks = totalChunks;
        int finalPersonalSpaceChunks = personalSpaceChunks;
        int finalPersonalSpaceDimensions = personalSpaceDimensions;
        source.sendSuccess(
                () -> Component.literal(
                        "Loaded chunks: all=" + finalTotalChunks
                                + ", personalspace=" + finalPersonalSpaceChunks
                                + ", personalspace_dims=" + finalPersonalSpaceDimensions),
                false);
        source.sendSuccess(() -> Component.literal("Top dimensions by loaded chunks:"), false);

        for (int index = 0; index < Math.min(40, infos.size()); index++) {
            DimensionChunkInfo info = infos.get(index);
            source.sendSuccess(
                    () -> Component.literal(
                            info.loadedChunks + " chunks | " + info.players + " players | " + info.name),
                    false);
        }
        return 1;
    }

    private record DimensionChunkInfo(String name, int loadedChunks, int players) {
    }

    private static Component translatable(String key, Object... args) {
        return Component.translatable("command.personalspace." + key, normalizeTranslationArgs(args));
    }

    private static Object[] normalizeTranslationArgs(Object[] args) {
        Object[] normalized = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Component || arg instanceof Number || arg instanceof Boolean || arg instanceof String) {
                normalized[i] = arg;
            } else if (arg != null) {
                normalized[i] = arg.toString();
            } else {
                normalized[i] = "";
            }
        }
        return normalized;
    }
}
