package me.eigenraven.personalspace;

import com.mojang.logging.LogUtils;
import me.eigenraven.personalspace.command.PSCommands;
import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.network.CreateDimensionPacket;
import me.eigenraven.personalspace.registry.PSBlockEntities;
import me.eigenraven.personalspace.registry.PSBlocks;
import me.eigenraven.personalspace.registry.PSItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

@Mod(PersonalSpace.MODID)
public final class PersonalSpace {
    public static final String MODID = "personalspace";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public PersonalSpace() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        PSBlocks.BLOCKS.register(modBus);
        PSItems.ITEMS.register(modBus);
        PSBlockEntities.BLOCK_ENTITIES.register(modBus);

        // Регистрируем обработчик событий (для генерации чанков)
        MinecraftForge.EVENT_BUS.register(PSDimensions.class);

        // Регистрируем сетевой пакет
        CHANNEL.registerMessage(0, CreateDimensionPacket.class,
                CreateDimensionPacket::encode,
                CreateDimensionPacket::decode,
                CreateDimensionPacket::handle
        );

        MinecraftForge.EVENT_BUS.addListener(PSCommands::register);
    }
}