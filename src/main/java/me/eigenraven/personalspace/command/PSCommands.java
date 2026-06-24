package me.eigenraven.personalspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.registry.PSItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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

public final class PSCommands {
    private static final BlockPos DEFAULT_PORTAL_TARGET_POS = new BlockPos(7, 65, 7);

    private PSCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("personalspace")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("dimension")
                        .then(Commands.argument("name", StringArgumentType.word())
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
                        )
                )
        );
    }

    private static int teleportToDimension(CommandSourceStack source, String dimensionName) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        ResourceKey<Level> dimensionKey = getPersonalSpaceDimensionKey(dimensionName);

        ServerLevel destination = server.getLevel(dimensionKey);

        if (destination == null) {
            destination = PSDimensions.getOrCreate(server, dimensionKey);
        }

        if (destination == null) {
            source.sendFailure(Component.literal("Personal Space dimension not found: " + dimensionKey.location()));
            return 0;
        }

        BlockPos targetPos = DEFAULT_PORTAL_TARGET_POS;

        destination.getChunkAt(targetPos);

        player.teleportTo(
                destination,
                targetPos.getX() + 0.5D,
                targetPos.getY() + 1.0D,
                targetPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );

        source.sendSuccess(
                () -> Component.literal("Teleported to Personal Space dimension: " + dimensionKey.location()),
                true
        );

        return 1;
    }

    private static int givePortal(
            CommandSourceStack source,
            String dimensionName,
            ServerPlayer targetPlayer
    ) {
        ResourceKey<Level> dimensionKey = getPersonalSpaceDimensionKey(dimensionName);

        ItemStack stack = new ItemStack(PSItems.PERSONAL_PORTAL.get());

        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.putBoolean("Active", true);
        blockEntityTag.putBoolean("ReturnPortal", false);
        blockEntityTag.putString("TargetLevel", dimensionKey.location().toString());
        blockEntityTag.putLong("TargetPos", DEFAULT_PORTAL_TARGET_POS.asLong());

        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                "Personal Portal: " + formatDimensionName(dimensionKey.location())
        ));

        boolean inserted = targetPlayer.getInventory().add(stack);

        if (!inserted) {
            targetPlayer.drop(stack, false);
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Gave Personal Portal for " + dimensionKey.location()
                                + " to " + targetPlayer.getGameProfile().getName()
                ),
                true
        );

        return 1;
    }

    private static ResourceKey<Level> getPersonalSpaceDimensionKey(String rawName) {
        ResourceLocation location = parsePersonalSpaceDimensionId(rawName);

        return ResourceKey.create(
                Registries.DIMENSION,
                location
        );
    }

    private static ResourceLocation parsePersonalSpaceDimensionId(String rawName) {
        String value = rawName.trim();

        if (value.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(value);

            if (parsed != null) {
                return parsed;
            }
        }

        String path = value;

        if (!path.startsWith("ps_")) {
            path = "ps_" + path;
        }

        return ResourceLocation.fromNamespaceAndPath(PersonalSpace.MODID, path);
    }

    private static String formatDimensionName(ResourceLocation location) {
        if (location.getNamespace().equals(PersonalSpace.MODID)) {
            String path = location.getPath();

            if (path.startsWith("ps_")) {
                return path.substring(3);
            }

            return path;
        }

        return location.toString();
    }
}