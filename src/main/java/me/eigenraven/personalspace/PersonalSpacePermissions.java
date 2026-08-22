package me.eigenraven.personalspace;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

@EventBusSubscriber(modid = PersonalSpace.MODID)
public final class PersonalSpacePermissions {
    public static final String PROTECTION_BYPASS_NAME = "personalspace.bypass.protection";

    public static final PermissionNode<Boolean> PROTECTION_BYPASS = new PermissionNode<>(
            PersonalSpace.MODID,
            "bypass.protection",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> false
    );

    private PersonalSpacePermissions() {
    }

    @SubscribeEvent
    public static void registerNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(PROTECTION_BYPASS);
    }

    public static boolean canBypassProtection(ServerPlayer player) {
        if (player == null || player instanceof FakePlayer) {
            return false;
        }
        return PermissionAPI.getPermission(player, PROTECTION_BYPASS);
    }
}
