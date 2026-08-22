package me.eigenraven.personalspace.command;

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.tree.CommandNode;
import me.eigenraven.personalspace.dimension.PSDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Locale;

public final class PersonalSpaceCommandBlocker {

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();

        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            return;
        }

        if (!PSDimensions.isPersonalSpaceDimension(player.level().dimension().location())) {
            return;
        }

        String rootCommand = getRootCommand(event);

        if (rootCommand == null) {
            return;
        }

        rootCommand = rootCommand.toLowerCase(Locale.ROOT);

        if (!isRtpCommand(rootCommand)) {
            return;
        }

        event.setCanceled(true);

        MinecraftServer server = player.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);

        if (overworld == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.personalspace.rtp_overworld_failed"
            ));
            return;
        }

        String command = event.getParseResults().getReader().getString();

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        String finalCommand = command;

        player.sendSystemMessage(Component.translatable(
                "message.personalspace.rtp_redirect_to_overworld"
        ));

        BlockPos spawn = overworld.getSharedSpawnPos();

        player.teleportTo(
                overworld,
                spawn.getX() + 0.5D,
                spawn.getY(),
                spawn.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );

        server.execute(() -> {
            if (player.isRemoved()) {
                return;
            }

            if (PSDimensions.isPersonalSpaceDimension(player.level().dimension().location())) {
                return;
            }

            server.getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(),
                    "/" + finalCommand
            );
        });
    }

    private static String getRootCommand(CommandEvent event) {
        CommandContextBuilder<CommandSourceStack> context = event.getParseResults().getContext();

        if (context.getNodes().isEmpty()) {
            return null;
        }

        CommandNode<CommandSourceStack> node = context.getNodes().get(0).getNode();

        if (node == null) {
            return null;
        }

        return node.getName();
    }

    private static boolean isRtpCommand(String command) {
        return command.equals("rtp")
                || command.equals("randomtp")
                || command.equals("randomteleport")
                || command.equals("wild")
                || command.equals("wilderness");
    }
}