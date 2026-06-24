package me.eigenraven.personalspace.registry;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.block.PortalBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PSBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PersonalSpace.MODID);

    public static final DeferredBlock<Block> PERSONAL_PORTAL =
            BLOCKS.register("personal_portal", PortalBlock::new);

    private PSBlocks() {
    }
}