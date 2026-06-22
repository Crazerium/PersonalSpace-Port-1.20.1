package me.eigenraven.personalspace.registry;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class PSCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PersonalSpace.MODID);

    public static final RegistryObject<CreativeModeTab> PERSONAL_SPACE_TAB =
            CREATIVE_MODE_TABS.register("personalspace", () -> CreativeModeTab.builder()
                    .title(Component.literal("PersonalSpace"))
                    .icon(() -> new ItemStack(PSItems.PERSONAL_PORTAL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(PSItems.PERSONAL_PORTAL.get());
                    })
                    .build());

    private PSCreativeTabs() {
    }
}