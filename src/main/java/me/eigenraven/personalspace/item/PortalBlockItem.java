package me.eigenraven.personalspace.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class PortalBlockItem extends BlockItem {
    public PortalBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack createLinkedPortal(Block block, ResourceKey<Level> targetLevel, BlockPos targetPos) {
        ItemStack stack = new ItemStack(block);

        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.putBoolean("Active", true);
        blockEntityTag.putString("TargetLevel", targetLevel.location().toString());
        blockEntityTag.putLong("TargetPos", targetPos.asLong());

        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));

        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);

        if (blockEntityData != null) {
            CompoundTag blockEntityTag = blockEntityData.copyTag();

            if (blockEntityTag.contains("TargetLevel")) {
                tooltip.add(Component.translatable(
                        "tooltip.personalspace.target",
                        blockEntityTag.getString("TargetLevel")
                ));

                super.appendHoverText(stack, context, tooltip, flag);
                return;
            }
        }

        tooltip.add(Component.translatable("tooltip.personalspace.unlinked"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}