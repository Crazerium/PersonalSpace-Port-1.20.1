package me.eigenraven.personalspace.personalspace;

import com.mojang.logging.LogUtils;
import me.eigenraven.personalspace.command.PSCommands;
import me.eigenraven.personalspace.registry.PSBlockEntities;
import me.eigenraven.personalspace.registry.PSBlocks;
import me.eigenraven.personalspace.registry.PSItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(PersonalSpace.MODID)
public final class PersonalSpace {
    public static final String MODID = "personalspace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PersonalSpace() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        PSBlocks.BLOCKS.register(modBus);
        PSItems.ITEMS.register(modBus);
        PSBlockEntities.BLOCK_ENTITIES.register(modBus);

        MinecraftForge.EVENT_BUS.addListener(PSCommands::register);
    }
}