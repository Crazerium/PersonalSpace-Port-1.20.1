package me.eigenraven.personalspace.registry;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.block.PortalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PSBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PersonalSpace.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortalBlockEntity>> PERSONAL_PORTAL =
            BLOCK_ENTITIES.register("personal_portal",
                    () -> BlockEntityType.Builder
                            .of(PortalBlockEntity::new, PSBlocks.PERSONAL_PORTAL.get())
                            .build(null));

    private PSBlockEntities() {
    }
}