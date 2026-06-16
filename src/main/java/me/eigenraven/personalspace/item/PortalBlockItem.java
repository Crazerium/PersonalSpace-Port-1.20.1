package me.eigenraven.personalspace.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

        stack.getOrCreateTag().put("BlockEntityTag", blockEntityTag);

        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");

        if (blockEntityTag != null && blockEntityTag.contains("TargetLevel")) {
            tooltip.add(Component.translatable(
                    "tooltip.personalspace.target",
                    blockEntityTag.getString("TargetLevel")
            ));
        } else {
            tooltip.add(Component.translatable("tooltip.personalspace.unlinked"));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }
}