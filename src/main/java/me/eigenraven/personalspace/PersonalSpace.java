package me.eigenraven.personalspace;

import com.mojang.logging.LogUtils;
import me.eigenraven.personalspace.command.PSCommands;
import me.eigenraven.personalspace.config.PSConfig;
import me.eigenraven.personalspace.event.PSWorldRules;
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
public final class PersonalSpace {
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

        NeoForge.EVENT_BUS.addListener(PSWorldRules::onPotentialSpawns);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onMobSpawnPositionCheck);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(PSWorldRules::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        PSCommands.register(event.getDispatcher());
    }
}