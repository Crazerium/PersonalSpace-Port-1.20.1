package me.eigenraven.personalspace;

import com.mojang.logging.LogUtils;
import me.eigenraven.personalspace.command.PSCommands;
import me.eigenraven.personalspace.command.PersonalSpaceCommandBlocker;
import me.eigenraven.personalspace.compat.gtceu.PersonalSpaceBedrockFluidVeins;
import me.eigenraven.personalspace.compat.gtceu.PersonalSpaceGTCEuConfig;
import me.eigenraven.personalspace.compat.gtceu.PersonalSpaceGTCEuDebugCommands;
import me.eigenraven.personalspace.compat.gtceu.PersonalSpaceGTCEuMaintenanceCommands;
import me.eigenraven.personalspace.compat.gtocore.GTOCoreAirCompat;
import me.eigenraven.personalspace.config.PSConfig;
import me.eigenraven.personalspace.dimension.PersonalSpaceDeletionManager;
import me.eigenraven.personalspace.dimension.PersonalSpaceAutoUnloadManager;
import me.eigenraven.personalspace.dimension.PersonalSpaceLazyMigrationManager;
import me.eigenraven.personalspace.event.PSWorldRules;
import me.eigenraven.personalspace.network.CreateDimensionPacket;
import me.eigenraven.personalspace.network.DeletePersonalSpacePacket;
import me.eigenraven.personalspace.network.SyncPersonalSpaceSettingsPacket;
import me.eigenraven.personalspace.network.UpdatePersonalSpaceSettingsPacket;
import me.eigenraven.personalspace.network.UsePortalPacket;
import me.eigenraven.personalspace.registry.PSBlockEntities;
import me.eigenraven.personalspace.registry.PSBlocks;
import me.eigenraven.personalspace.registry.PSChunkGenerators;
import me.eigenraven.personalspace.registry.PSCreativeTabs;
import me.eigenraven.personalspace.registry.PSItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
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

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.SERVER,
                PSConfig.SERVER_SPEC,
                "personalspace-server.toml"
        );

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                PersonalSpaceGTCEuConfig.SPEC,
                "personalspace-gtceu.toml"
        );
        modBus.addListener(this::onModConfigLoaded);

        PSBlocks.BLOCKS.register(modBus);
        PSItems.ITEMS.register(modBus);
        PSBlockEntities.BLOCK_ENTITIES.register(modBus);
        PSChunkGenerators.CHUNK_GENERATORS.register(modBus);
        PSCreativeTabs.CREATIVE_MODE_TABS.register(modBus);

        CHANNEL.registerMessage(
                0,
                CreateDimensionPacket.class,
                CreateDimensionPacket::encode,
                CreateDimensionPacket::decode,
                CreateDimensionPacket::handle
        );

        CHANNEL.registerMessage(
                1,
                UsePortalPacket.class,
                UsePortalPacket::encode,
                UsePortalPacket::decode,
                UsePortalPacket::handle
        );

        CHANNEL.registerMessage(
                2,
                UpdatePersonalSpaceSettingsPacket.class,
                UpdatePersonalSpaceSettingsPacket::encode,
                UpdatePersonalSpaceSettingsPacket::decode,
                UpdatePersonalSpaceSettingsPacket::handle
        );

        CHANNEL.registerMessage(
                3,
                SyncPersonalSpaceSettingsPacket.class,
                SyncPersonalSpaceSettingsPacket::encode,
                SyncPersonalSpaceSettingsPacket::decode,
                SyncPersonalSpaceSettingsPacket::handle
        );

        CHANNEL.registerMessage(
                4,
                DeletePersonalSpacePacket.class,
                DeletePersonalSpacePacket::encode,
                DeletePersonalSpacePacket::decode,
                DeletePersonalSpacePacket::handle
        );

        MinecraftForge.EVENT_BUS.addListener(PSWorldRules::onPotentialSpawns);
        MinecraftForge.EVENT_BUS.addListener(PSWorldRules::onMobSpawnPositionCheck);
        MinecraftForge.EVENT_BUS.addListener(PSWorldRules::onEntityJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(PSWorldRules::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(PSWorldRules::onPlayerChangedDimension);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(PSWorldRules::onLevelLoad);
        MinecraftForge.EVENT_BUS.addListener(PSWorldRules::onLevelUnload);
        MinecraftForge.EVENT_BUS.addListener(PersonalSpaceDeletionManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(PersonalSpaceLazyMigrationManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(PersonalSpaceAutoUnloadManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(PersonalSpaceAutoUnloadManager::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(PersonalSpaceAutoUnloadManager::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.register(new GTOCoreAirCompat());
        MinecraftForge.EVENT_BUS.register(new PersonalSpaceCommandBlocker());
    }

    private void onModConfigLoaded(ModConfigEvent.Loading event) {
        if (event == null || event.getConfig() == null) {
            return;
        }

        if (event.getConfig().getSpec() != PersonalSpaceGTCEuConfig.SPEC) {
            return;
        }

        LOGGER.warn("PERSONALSPACE GTCEU CONFIG LOADED, INITIALIZING BEDROCK FLUID VEINS");

        try {
            PersonalSpaceBedrockFluidVeins.init();
            LOGGER.warn("PERSONALSPACE GTCEU BEDROCK FLUID VEINS INIT AFTER CONFIG LOAD FINISHED");
        } catch (Throwable throwable) {
            LOGGER.warn("PERSONALSPACE GTCEU BEDROCK FLUID VEINS INIT AFTER CONFIG LOAD FAILED", throwable);
        }
    }

    private void registerCommands(RegisterCommandsEvent event) {
        PSCommands.register(event.getDispatcher());
        PersonalSpaceGTCEuDebugCommands.register(event.getDispatcher());
        PersonalSpaceGTCEuMaintenanceCommands.register(event.getDispatcher());
    }
}