package me.eigenraven.personalspace;

import com.mojang.logging.LogUtils;
import me.eigenraven.personalspace.command.PSCommands;
import me.eigenraven.personalspace.command.PersonalSpaceCommandBlocker;
import me.eigenraven.personalspace.config.PSConfig;
import me.eigenraven.personalspace.dimension.PersonalSpaceAutoUnloadManager;
import me.eigenraven.personalspace.dimension.PersonalSpaceDeletionManager;
import me.eigenraven.personalspace.dimension.PersonalSpaceLazyMigrationManager;
import me.eigenraven.personalspace.dimension.PersonalSpaceProtectionManager;
import me.eigenraven.personalspace.event.PSWorldRules;
import me.eigenraven.personalspace.network.PSNetwork;
import me.eigenraven.personalspace.registry.PSBlockEntities;
import me.eigenraven.personalspace.registry.PSBlocks;
import me.eigenraven.personalspace.registry.PSChunkGenerators;
import me.eigenraven.personalspace.registry.PSCreativeTabs;
import me.eigenraven.personalspace.registry.PSItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(PersonalSpace.MODID)
public class PersonalSpace {
    public static final String MODID = "personalspace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PersonalSpace(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                PSConfig.SERVER_SPEC,
                "personalspace-server.toml"
        );

        PSBlocks.BLOCKS.register(modBus);
        PSItems.ITEMS.register(modBus);
        PSBlockEntities.BLOCK_ENTITIES.register(modBus);
        PSChunkGenerators.CHUNK_GENERATORS.register(modBus);
        PSCreativeTabs.CREATIVE_MODE_TABS.register(modBus);

        modBus.addListener(PSNetwork::register);

        NeoForge.EVENT_BUS.addListener(PSWorldRules::onPotentialSpawns);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onMobSpawnPositionCheck);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        NeoForge.EVENT_BUS.addListener(PersonalSpaceDeletionManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(PersonalSpaceLazyMigrationManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(PersonalSpaceAutoUnloadManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(PersonalSpaceAutoUnloadManager::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(PersonalSpaceAutoUnloadManager::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(PersonalSpaceProtectionManager::onServerStarted);
        NeoForge.EVENT_BUS.register(new PersonalSpaceCommandBlocker());
    }

    private void registerCommands(RegisterCommandsEvent event) {
        PSCommands.register(event.getDispatcher());
    }
}
