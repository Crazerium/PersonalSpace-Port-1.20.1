package me.eigenraven.personalspace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.eigenraven.personalspace.personalspace.PersonalSpace;
import me.eigenraven.personalspace.block.PortalBlockEntity;
import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.item.PortalBlockItem;
import me.eigenraven.personalspace.registry.PSBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class PSCommands {
    private PSCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("pspace")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("ls")
                                .executes(context -> listPersonalDimensions(context.getSource())))

                        .then(Commands.literal("where")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> where(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player")
                                        ))))

                        .then(Commands.literal("give-portal")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("dimension", StringArgumentType.word())
                                                .executes(context -> givePortal(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "dimension"),
                                                        new BlockPos(0, 80, 0)
                                                ))
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(context -> givePortal(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                StringArgumentType.getString(context, "dimension"),
                                                                BlockPosArgument.getLoadedBlockPos(context, "pos")
                                                        ))))))

                        .then(Commands.literal("tpx")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("dimension", StringArgumentType.word())
                                                .executes(context -> teleport(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "dimension"),
                                                        new BlockPos(0, 80, 0)
                                                ))
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(context -> teleport(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                StringArgumentType.getString(context, "dimension"),
                                                                BlockPosArgument.getLoadedBlockPos(context, "pos")
                                                        ))))))
        );
    }

    private static int givePortal(
            CommandSourceStack source,
            ServerPlayer player,
            String dimension,
            BlockPos targetPos
    ) {
        ResourceKey<Level> key = PSDimensions.key(dimension);
        ServerLevel level = PSDimensions.getOrCreate(source.getServer(), key);

        PortalBlockEntity.prepareLanding(level, targetPos);

        ItemStack stack = PortalBlockItem.createLinkedPortal(
                PSBlocks.PERSONAL_PORTAL.get(),
                key,
                targetPos
        );

        player.getInventory().placeItemBackInInventory(stack);

        source.sendSuccess(
                () -> Component.literal("Gave Personal Space portal to " + player.getGameProfile().getName()
                        + " -> " + key.location()),
                true
        );

        return 1;
    }

    private static int teleport(
            CommandSourceStack source,
            ServerPlayer player,
            String dimension,
            BlockPos targetPos
    ) {
        ResourceKey<Level> key = PSDimensions.key(dimension);
        ServerLevel level = PSDimensions.getOrCreate(source.getServer(), key);

        PortalBlockEntity.prepareLanding(level, targetPos);

        player.teleportTo(
                level,
                targetPos.getX() + 0.5D,
                targetPos.getY() + 1.0D,
                targetPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );

        source.sendSuccess(
                () -> Component.literal("Teleported " + player.getGameProfile().getName()
                        + " to " + key.location()),
                true
        );

        return 1;
    }

    private static int where(CommandSourceStack source, ServerPlayer player) {
        source.sendSuccess(
                () -> Component.literal(player.getGameProfile().getName()
                        + " is in " + player.level().dimension().location()),
                false
        );

        return 1;
    }

    private static int listPersonalDimensions(CommandSourceStack source) {
        int[] count = {0};

        for (ServerLevel level : source.getServer().getAllLevels()) {
            if (PersonalSpace.MODID.equals(level.dimension().location().getNamespace())) {
                count[0]++;
                source.sendSuccess(
                        () -> Component.literal(level.dimension().location().toString()),
                        false
                );
            }
        }

        if (count[0] == 0) {
            source.sendSuccess(
                    () -> Component.literal("No loaded Personal Space dimensions."),
                    false
            );
        }
        return 0;
    }
}