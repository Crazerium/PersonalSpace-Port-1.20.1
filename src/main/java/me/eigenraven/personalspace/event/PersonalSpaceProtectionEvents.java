package me.eigenraven.personalspace.event;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.config.PSConfig;
import me.eigenraven.personalspace.dimension.PersonalSpaceProtectionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = PersonalSpace.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PersonalSpaceProtectionEvents {
    private static final Map<UUID, Long> LAST_NOTICE = new ConcurrentHashMap<>();

    private PersonalSpaceProtectionEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && deny(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && deny(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && deny(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!PSConfig.SERVER.privacyProtectContainers.get()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && deny(player)) {
            event.setCanceled(true);
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && deny(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!PSConfig.SERVER.privacyProtectEntities.get()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            if (isOwnCorpse(player, event.getTarget())) {
                return;
            }
            if (deny(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!PSConfig.SERVER.privacyProtectEntities.get()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            if (isOwnCorpse(player, event.getTarget())) {
                return;
            }
            if (deny(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!PSConfig.SERVER.privacyProtectEntities.get()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && deny(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFillBucket(FillBucketEvent event) {
        if (!PSConfig.SERVER.privacyProtectContainers.get()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && deny(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!PSConfig.SERVER.privacyProtectFromPlayerExplosions.get()) {
            return;
        }

        if (event.getExplosion().getDirectSourceEntity() instanceof ServerPlayer player
                && denySilently(player)) {
            event.getAffectedBlocks().clear();
            event.getAffectedEntities().clear();
        }
    }
    private static boolean isOwnCorpse(ServerPlayer player, Entity target) {
        if (!"de.maxhenkel.corpse.entities.CorpseEntity".equals(target.getClass().getName())) {
            return false;
        }

        try {
            Method method = target.getClass().getMethod("getCorpseUUID");
            Object value = method.invoke(target);
            if (value instanceof Optional<?> optional) {
                return optional.filter(UUID.class::isInstance)
                        .map(UUID.class::cast)
                        .map(player.getUUID()::equals)
                        .orElse(false);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return false;
    }

    private static boolean deny(ServerPlayer player) {
        boolean denied = denySilently(player);
        if (denied) {
            long now = System.currentTimeMillis();
            long previous = LAST_NOTICE.getOrDefault(player.getUUID(), 0L);
            if (now - previous >= 2000L) {
                player.sendSystemMessage(Component.literal("Эта персоналка защищена. Изменять её могут только владелец и его FTB-команда."));
                LAST_NOTICE.put(player.getUUID(), now);
            }
        }
        return denied;
    }

    private static boolean denySilently(ServerPlayer player) {
        return !PersonalSpaceProtectionManager.canModify(player);
    }
}
