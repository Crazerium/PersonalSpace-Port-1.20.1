package me.eigenraven.personalspace.registry;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.block.PersonalAirIntakeBlock;
import me.eigenraven.personalspace.block.PortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, PersonalSpace.MODID);

    public static final RegistryObject<PortalBlock> PERSONAL_PORTAL =
            BLOCKS.register("personal_portal", PortalBlock::new);

    public static final RegistryObject<PersonalAirIntakeBlock> PERSONAL_AIR_INTAKE =
            BLOCKS.register("personal_air_intake", () -> new PersonalAirIntakeBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            ));

    private PSBlocks() {
    }
}