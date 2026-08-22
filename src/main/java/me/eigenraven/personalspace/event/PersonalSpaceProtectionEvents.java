package me.eigenraven.personalspace.event;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.config.PSConfig;
import me.eigenraven.personalspace.config.PersonalSpacePublicInteractionConfig;
import me.eigenraven.personalspace.dimension.PersonalSpaceProtectionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BucketItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = PersonalSpace.MODID)
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

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (PersonalSpacePublicInteractionConfig.allows(event.getLevel().getBlockState(event.getPos()).getBlock())) {
            // Public blocks may still be opened/used, but a held item must not modify the protected space.
            if (denySilently(player)) {
                event.setUseItem(TriState.FALSE);
            }
            return;
        }

        if (deny(player)) {
            // NeoForge's RightClickBlock#setCanceled(true) already denies both block and item use.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!PSConfig.SERVER.privacyProtectContainers.get()) {
            return;
        }

        // FillBucketEvent from the Forge 1.20.1 implementation is not used here.
        // Buckets execute through the item-use path, so protect them at RightClickItem instead.
        if (event.getItemStack().getItem() instanceof BucketItem
                && event.getEntity() instanceof ServerPlayer player
                && deny(player)) {
            event.setCanceled(true);
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
                player.sendSystemMessage(Component.translatable("message.personalspace.protected"));
                LAST_NOTICE.put(player.getUUID(), now);
            }
        }
        return denied;
    }

    private static boolean denySilently(ServerPlayer player) {
        return !PersonalSpaceProtectionManager.canModify(player);
    }
}
