package me.eigenraven.personalspace.registry;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.item.PortalBlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PSItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PersonalSpace.MODID);

    public static final DeferredItem<Item> PERSONAL_PORTAL =
            ITEMS.register("personal_portal",
                    () -> new PortalBlockItem(PSBlocks.PERSONAL_PORTAL.get(), new Item.Properties()));

    private PSItems() {
    }
}