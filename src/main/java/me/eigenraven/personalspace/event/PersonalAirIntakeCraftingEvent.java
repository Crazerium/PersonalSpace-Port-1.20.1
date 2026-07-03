package me.eigenraven.personalspace.event;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.registry.PSItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.Locale;

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
                    literal(
                            "Преобразовано в персональный воздухозаборный люк: " + count + " шт.",
                            "Converted to Personal Air Intake: " + count
                    )
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
                        literal(
                                "Преобразовано обратно в Infinite Intake Hatch: " + count + " шт.",
                                "Converted back to Infinite Intake Hatch: " + count
                        )
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
            event.getToolTip().add(literal(
                    "Можно превратить в персональный воздухозаборный люк.",
                    "Can be converted into a Personal Air Intake."
            ).withStyle(ChatFormatting.GRAY));

            event.getToolTip().add(literal(
                    "Shift + ПКМ в воздухе для конвертации.",
                    "Shift + Right Click in air to convert."
            ).withStyle(ChatFormatting.AQUA));
        }
    }

    private static MutableComponent literal(String ru, String en) {
        if (isRussian()) {
            return Component.literal(ru);
        }

        return Component.literal(en);
    }

    private static boolean isRussian() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            String selected = Minecraft.getInstance().getLanguageManager().getSelected();
            return selected != null && selected.toLowerCase(Locale.ROOT).startsWith("ru");
        }

        return Locale.getDefault().getLanguage().equalsIgnoreCase("ru");
    }
}