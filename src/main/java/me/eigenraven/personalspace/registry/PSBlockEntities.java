package me.eigenraven.personalspace.registry;


import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.block.PortalBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PersonalSpace.MODID);

    public static final RegistryObject<BlockEntityType<PortalBlockEntity>> PERSONAL_PORTAL =
            BLOCK_ENTITIES.register("personal_portal",
                    () -> BlockEntityType.Builder
                            .of(PortalBlockEntity::new, PSBlocks.PERSONAL_PORTAL.get())
                            .build(null));

    private PSBlockEntities() {
    }
}