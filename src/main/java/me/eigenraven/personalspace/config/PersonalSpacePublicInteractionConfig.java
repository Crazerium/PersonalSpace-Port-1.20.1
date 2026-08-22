package me.eigenraven.personalspace.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;

@EventBusSubscriber(modid = PersonalSpace.MODID)
public final class PersonalSpacePublicInteractionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ResourceLocation DEFAULT_BLOCK = ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table");

    private static volatile Set<ResourceLocation> allowedBlocks = Set.of(DEFAULT_BLOCK);
    private static volatile boolean loaded;

    private PersonalSpacePublicInteractionConfig() {
    }

    public static boolean allows(Block block) {
        ensureLoaded();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && allowedBlocks.contains(id);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        reload();
    }

    public static boolean reload() {
        synchronized (PersonalSpacePublicInteractionConfig.class) {
            Path path = configPath();
            try {
                if (Files.notExists(path)) {
                    writeDefault(path);
                }
                load(path);
                loaded = true;
                return true;
            } catch (Exception exception) {
                PersonalSpace.LOGGER.warn("Failed to reload Personal Space public interaction config from {}. Keeping previous values.", path, exception);
                return false;
            }
        }
    }

    public static int allowedBlockCount() {
        ensureLoaded();
        return allowedBlocks.size();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        reload();
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("personalspace-public-interactions.json");
    }

    private static void writeDefault(Path path) throws IOException {
        Files.createDirectories(path.getParent());

        JsonObject root = new JsonObject();
        JsonArray blocks = new JsonArray();
        blocks.add(DEFAULT_BLOCK.toString());
        root.add("allowed_blocks", blocks);

        Files.writeString(
                path,
                GSON.toJson(root),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    private static void load(Path path) throws IOException {
        Set<ResourceLocation> next = new LinkedHashSet<>();

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Config root must be a JSON object");
            }

            JsonObject root = parsed.getAsJsonObject();
            JsonElement allowed = root.get("allowed_blocks");
            if (allowed != null && !allowed.isJsonNull()) {
                if (!allowed.isJsonArray()) {
                    throw new IllegalArgumentException("allowed_blocks must be a JSON array");
                }

                for (JsonElement element : allowed.getAsJsonArray()) {
                    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                        continue;
                    }

                    String raw = element.getAsString().trim();
                    ResourceLocation id = ResourceLocation.tryParse(raw);
                    if (id == null) {
                        PersonalSpace.LOGGER.warn("Ignoring invalid block id '{}' in {}", raw, path);
                        continue;
                    }
                    next.add(id);
                }
            }
        }

        allowedBlocks = Set.copyOf(next);
        PersonalSpace.LOGGER.info("Loaded {} public Personal Space block interaction(s) from {}", allowedBlocks.size(), path);
    }
}
