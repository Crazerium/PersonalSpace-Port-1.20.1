package me.eigenraven.personalspace.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public final class PersonalAirIntakeBlockItem extends BlockItem {
    public PersonalAirIntakeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return literal(
                "Персональный воздухозаборный люк",
                "Personal Air Intake"
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(literal(
                "Генерирует воздух GTCEu внутри PersonalSpace.",
                "Generates GTCEu Air inside PersonalSpace."
        ).withStyle(ChatFormatting.GRAY));

        tooltip.add(literal(
                "Для получения нажмите Shift + ПКМ Бесконечным воздухозаборным люком.",
                "For crafting press Shift + RMB by Infinite Intake Hatch."
        ).withStyle(ChatFormatting.AQUA));

        tooltip.add(literal(
                "Можно сконвертировать обратно нажав Shift + ПКМ.",
                "Can be converted back by press Shift + RMB."
        ).withStyle(ChatFormatting.BLUE));

        super.appendHoverText(stack, level, tooltip, flag);
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