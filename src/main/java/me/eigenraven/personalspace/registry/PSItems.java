package me.eigenraven.personalspace.registry;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.item.PortalBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PersonalSpace.MODID);

    public static final RegistryObject<PortalBlockItem> PERSONAL_PORTAL =
            ITEMS.register("personal_portal", () -> new PortalBlockItem(
                    PSBlocks.PERSONAL_PORTAL.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<BlockItem> PERSONAL_AIR_INTAKE =
            ITEMS.register("personal_air_intake", () -> new BlockItem(
                    PSBlocks.PERSONAL_AIR_INTAKE.get(),
                    new Item.Properties()
            ));

    private PSItems() {
    }
}