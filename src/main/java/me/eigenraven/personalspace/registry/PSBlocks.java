package me.eigenraven.personalspace.registry;


import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.block.PortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, PersonalSpace.MODID);

    public static final RegistryObject<Block> PERSONAL_PORTAL =
            BLOCKS.register("personal_portal", PortalBlock::new);

    private PSBlocks() {
    }
}