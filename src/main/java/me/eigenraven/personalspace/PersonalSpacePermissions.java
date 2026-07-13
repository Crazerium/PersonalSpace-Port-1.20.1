package me.eigenraven.personalspace;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContext;

@Mod.EventBusSubscriber(modid = PersonalSpace.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
