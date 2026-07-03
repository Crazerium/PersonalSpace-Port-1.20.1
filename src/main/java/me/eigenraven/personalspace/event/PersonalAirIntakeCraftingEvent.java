package me.eigenraven.personalspace.event;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.registry.PSItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PersonalSpace.MODID)
public final class PersonalAirIntakeCraftingEvent {
    private static final ResourceLocation INFINITE_INTAKE_HATCH_ID =
            new ResourceLocation("gtocore", "infinite_intake_hatch");

    private static final ResourceLocation PERSONAL_AIR_INTAKE_ID =
            new ResourceLocation(PersonalSpace.MODID, "personal_air_intake");

    private PersonalAirIntakeCraftingEvent() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        ItemStack heldStack = event.getItemStack();

        if (heldStack.isEmpty()) {
            return;
        }

        ResourceLocation heldItemId = BuiltInRegistries.ITEM.getKey(heldStack.getItem());

        if (INFINITE_INTAKE_HATCH_ID.equals(heldItemId)) {
            int count = heldStack.getCount();

            convertHeldStack(
                    player,
                    event,
                    new ItemStack(PSItems.PERSONAL_AIR_INTAKE.get(), count),
                    Component.translatable("message.personalspace.air_intake.converted_to_personal", count)
            );
            return;
        }

        if (PERSONAL_AIR_INTAKE_ID.equals(heldItemId)) {
            Item infiniteIntakeHatch = BuiltInRegistries.ITEM.get(INFINITE_INTAKE_HATCH_ID);

            if (BuiltInRegistries.ITEM.getKey(infiniteIntakeHatch).equals(INFINITE_INTAKE_HATCH_ID)) {
                int count = heldStack.getCount();

                convertHeldStack(
                        player,
                        event,
                        new ItemStack(infiniteIntakeHatch, count),
                        Component.translatable("message.personalspace.air_intake.converted_to_infinite", count)
                );
            }
        }
    }

    private static void convertHeldStack(
            Player player,
            PlayerInteractEvent.RightClickItem event,
            ItemStack result,
            Component message
    ) {
        ItemStack heldStack = event.getItemStack();

        if (!player.getAbilities().instabuild) {
            heldStack.setCount(0);
        }

        player.setItemInHand(event.getHand(), result);

        player.displayClientMessage(message, true);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.isEmpty()) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (INFINITE_INTAKE_HATCH_ID.equals(itemId)) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.personalspace.air_intake.infinite_hatch_1"
            ).withStyle(ChatFormatting.GRAY));

            event.getToolTip().add(Component.translatable(
                    "tooltip.personalspace.air_intake.infinite_hatch_2"
            ).withStyle(ChatFormatting.AQUA));
            return;
        }

        if (PERSONAL_AIR_INTAKE_ID.equals(itemId)) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.personalspace.air_intake.personal_1"
            ).withStyle(ChatFormatting.GRAY));

            event.getToolTip().add(Component.translatable(
                    "tooltip.personalspace.air_intake.personal_2"
            ).withStyle(ChatFormatting.AQUA));
        }
    }
}