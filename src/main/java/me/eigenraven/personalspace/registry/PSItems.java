package me.eigenraven.personalspace.registry;

import me.eigenraven.personalspace.personalspace.PersonalSpace;
import me.eigenraven.personalspace.item.PortalBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PersonalSpace.MODID);

    public static final RegistryObject<Item> PERSONAL_PORTAL =
            ITEMS.register("personal_portal",
                    () -> new PortalBlockItem(PSBlocks.PERSONAL_PORTAL.get(), new Item.Properties()));

    private PSItems() {
    }
}