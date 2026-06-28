package me.eigenraven.personalspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.dimension.PersonalSpaceDeletionManager;
import me.eigenraven.personalspace.registry.PSItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class PSCommands {
    private static final BlockPos FALLBACK_PORTAL_TARGET_POS = new BlockPos(7, 66, 7);

    private PSCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("personalspace")

                // Игроки подтверждают / отменяют удаление командной персоналки.
                // Тут НЕ нужен OP, иначе обычные участники команды не смогут подтвердить.
                .then(Commands.literal("delete")
                        .then(Commands.literal("approve")
                                .executes(context -> PersonalSpaceDeletionManager.approveDelete(context.getSource()))
                        )
                        .then(Commands.literal("cancel")
                                .executes(context -> PersonalSpaceDeletionManager.cancelDelete(context.getSource()))
                        )
                )

                // Админские команды.
                .then(Commands.literal("dimension")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        getPersonalSpaceDimensionSuggestions(context.getSource().getServer()),
                                        builder
                                ))
                                .then(Commands.literal("tp")
                                        .executes(context -> teleportToDimension(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "name")
                                        ))
                                )
                                .then(Commands.literal("giveportal")
                                        .executes(context -> givePortal(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "name"),
                                                context.getSource().getPlayerOrException()
                                        ))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> givePortal(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name"),
                                                        EntityArgument.getPlayer(context, "player")
                                                ))
                                        )
                                )
                                .then(Commands.literal("respawn")
                                        .then(Commands.literal("get")
                                                .executes(context -> getRespawn(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name")
                                                ))
                                        )
                                        .then(Commands.literal("set")
                                                .executes(context -> setRespawn(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name")
                                                ))
                                        )
                                        .then(Commands.literal("reset")
                                                .executes(context -> resetRespawn(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name")
                                                ))
                                        )
                                )

                                // Админское принудительное удаление персоналки.
                                // Команда:
                                // /personalspace dimension <name> delete confirm
                                .then(Commands.literal("delete")
                                        .requires(source -> source.hasPermission(3))
                                        .then(Commands.literal("confirm")
                                                .executes(context -> deleteDimension(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name")
                                                ))
                                        )
                                )
                        )
                )
        );
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

        ServerLevel destination = server.getLevel(dimensionKey);

        if (destination == null) {
            destination = PSDimensions.getOrCreate(server, dimensionKey);
        }

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
                player.getXRot()
        );

        source.sendSuccess(
                () -> translatable("teleported", dimensionKey.location(), formatPos(targetPos)),
                true
        );

        return 1;
    }

    private static int givePortal(
            CommandSourceStack source,
            String dimensionName,
            ServerPlayer targetPlayer
    ) {
        MinecraftServer server = source.getServer();
        ResourceKey<Level> dimensionKey = getPersonalSpaceDimensionKey(dimensionName);
        BlockPos targetPos = getRespawnPosForDimension(server, dimensionKey);

        ItemStack stack = new ItemStack(PSItems.PERSONAL_PORTAL.get());

        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.putBoolean("Active", true);
        blockEntityTag.putBoolean("ReturnPortal", false);
        blockEntityTag.putString("TargetLevel", dimensionKey.location().toString());
        blockEntityTag.putLong("TargetPos", targetPos.asLong());

        stack.getOrCreateTag().put("BlockEntityTag", blockEntityTag);
        stack.setHoverName(Component.literal("Personal Portal: " + formatDimensionName(dimensionKey.location())));

        boolean inserted = targetPlayer.getInventory().add(stack);

        if (!inserted) {
            targetPlayer.drop(stack, false);
        }

        source.sendSuccess(
                () -> translatable(
                        "gave_portal",
                        dimensionKey.location(),
                        targetPlayer.getGameProfile().getName(),
                        formatPos(targetPos)
                ),
                true
        );

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
                false
        );

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
                true
        );

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
                true
        );

        return 1;
    }

    private static int deleteDimension(CommandSourceStack source, String dimensionName) {
        ResourceKey<Level> dimensionKey = getPersonalSpaceDimensionKey(dimensionName);
        return PersonalSpaceDeletionManager.adminDelete(source, dimensionKey);
    }

    private static ServerLevel getOrCreatePersonalSpaceLevel(
            MinecraftServer server,
            ResourceKey<Level> dimensionKey
    ) {
        ServerLevel level = server.getLevel(dimensionKey);

        if (level != null) {
            return level;
        }

        return PSDimensions.getOrCreate(server, dimensionKey);
    }

    private static BlockPos getRespawnPosForDimension(
            MinecraftServer server,
            ResourceKey<Level> dimensionKey
    ) {
        ServerLevel level = server.getLevel(dimensionKey);

        if (level == null) {
            level = PSDimensions.getOrCreate(server, dimensionKey);
        }

        if (level == null) {
            return FALLBACK_PORTAL_TARGET_POS;
        }

        PersonalSpaceData data = PersonalSpaceData.load(level);
        return data.getRespawnPos();
    }

    private static ResourceKey<Level> getPersonalSpaceDimensionKey(String rawName) {
        ResourceLocation location = parsePersonalSpaceDimensionId(rawName);

        return ResourceKey.create(
                Registries.DIMENSION,
                location
        );
    }

    private static ResourceLocation parsePersonalSpaceDimensionId(String rawName) {
        String value = rawName.trim().toLowerCase();

        if (value.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(value);

            if (parsed != null) {
                return parsed;
            }
        }

        String path = value;

        if (path.startsWith(PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/")) {
            return new ResourceLocation(PersonalSpace.MODID, path);
        }

        if (!path.startsWith("ps_") && !path.startsWith("team_")) {
            path = "ps_" + path;
        }

        return new ResourceLocation(
                PersonalSpace.MODID,
                PSDimensions.PERSONAL_SPACE_DIMENSION_FOLDER + "/" + path
        );
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

            return path;
        }

        return location.toString();
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static List<String> getPersonalSpaceDimensionSuggestions(MinecraftServer server) {
        List<String> suggestions = new ArrayList<>();

        for (ServerLevel level : server.getAllLevels()) {
            ResourceLocation location = level.dimension().location();

            if (!location.getNamespace().equals(PersonalSpace.MODID)) {
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

    private static void addSuggestionIfMissing(List<String> suggestions, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (!suggestions.contains(value)) {
            suggestions.add(value);
        }
    }

    private static Component translatable(String key, Object... args) {
        return Component.translatable("command.personalspace." + key, args);
    }
}