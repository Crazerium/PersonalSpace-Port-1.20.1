package me.eigenraven.personalspace.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

public class PersonalSpaceData {
    public enum WorldType {
        VOID,
        FLAT
    }
    private WorldType type = WorldType.VOID;
    private int groundLevel = 64;
    private String returnLevel = null;
    private int returnPosX = 0;
    private int returnPosY = 64;
    private int returnPosZ = 0;
    private int respawnPosX = 7;
    private int respawnPosY = 66;
    private int respawnPosZ = 7;
    private long timeOfDay = 6000L;
    private int skyRed = 128;
    private int skyGreen = 192;
    private int skyBlue = 255;
    private float starBrightness = 1.0F;
    private String biomeName = "minecraft:plains";

    private boolean treesEnabled = false;
    private boolean foliageEnabled = false;
    private boolean weatherEnabled = false;
    private boolean cloudsEnabled = false;

    private String layersPreset = "minecraft:bedrock,1;minecraft:dirt,3;minecraft:grass_block,1";
    private int boundaryChunksX = 2;
    private int boundaryChunksZ = 2;
    private int gapChunks = 1;

    private String boundaryBlock = "minecraft:yellow_concrete";
    private String roadBlock = "minecraft:black_concrete";
    private String centerMarkerBlock = "minecraft:white_concrete";

    private boolean centerMarkerEnabled = true;
    private boolean repeatingGridEnabled = false;
    private int repeatingGridOriginX = 0;
    private int repeatingGridOriginZ = 0;

    // Whole-dimension privacy metadata. Empty values are migrated from the dimension id.
    private String protectionOwnerType = ""; // player or team
    private String protectionOwnerId = "";   // player UUID or compact team UUID
    private String protectionOwnerName = "";
    private String protectionTeamId = "";    // compact FTB team UUID for player-owned spaces
    public float getStarBrightness() {
        return starBrightness;
    }

    public void setStarBrightness(float starBrightness) {
        this.starBrightness = Math.max(0.0F, Math.min(1.0F, starBrightness));
    }

    public String getBiomeName() {
        return biomeName == null || biomeName.isBlank() ? "minecraft:plains" : biomeName;
    }

    public void setBiomeName(String biomeName) {
        if (biomeName == null || biomeName.isBlank()) {
            biomeName = "minecraft:plains";
        }
        this.biomeName = biomeName;
    }

    public boolean isTreesEnabled() {
        return treesEnabled;
    }

    public void setTreesEnabled(boolean treesEnabled) {
        this.treesEnabled = treesEnabled;
    }

    public boolean isFoliageEnabled() {
        return foliageEnabled;
    }

    public void setFoliageEnabled(boolean foliageEnabled) {
        this.foliageEnabled = foliageEnabled;
    }

    public boolean isWeatherEnabled() {
        return weatherEnabled;
    }

    public void setWeatherEnabled(boolean weatherEnabled) {
        this.weatherEnabled = weatherEnabled;
    }

    public boolean isCloudsEnabled() {
        return cloudsEnabled;
    }

    public void setCloudsEnabled(boolean cloudsEnabled) {
        this.cloudsEnabled = cloudsEnabled;
    }
    public int getBoundaryChunksX() {
        return Math.max(0, Math.min(16, boundaryChunksX));
    }

    public void setBoundaryChunksX(int boundaryChunksX) {
        this.boundaryChunksX = Math.max(0, Math.min(16, boundaryChunksX));
    }

    public int getBoundaryChunksZ() {
        return Math.max(0, Math.min(16, boundaryChunksZ));
    }

    public void setBoundaryChunksZ(int boundaryChunksZ) {
        this.boundaryChunksZ = Math.max(0, Math.min(16, boundaryChunksZ));
    }

    public int getGapChunks() {
        return Math.max(0, Math.min(16, gapChunks));
    }

    public void setGapChunks(int gapChunks) {
        this.gapChunks = Math.max(0, Math.min(16, gapChunks));
    }

    public String getBoundaryBlock() {
        return boundaryBlock == null || boundaryBlock.isBlank()
                ? "minecraft:yellow_concrete"
                : boundaryBlock;
    }

    public void setBoundaryBlock(String boundaryBlock) {
        if (boundaryBlock == null || boundaryBlock.isBlank()) {
            boundaryBlock = "minecraft:yellow_concrete";
        }

        this.boundaryBlock = boundaryBlock;
    }

    public String getRoadBlock() {
        return roadBlock == null || roadBlock.isBlank()
                ? "minecraft:black_concrete"
                : roadBlock;
    }

    public void setRoadBlock(String roadBlock) {
        if (roadBlock == null || roadBlock.isBlank()) {
            roadBlock = "minecraft:black_concrete";
        }

        this.roadBlock = roadBlock;
    }

    public String getCenterMarkerBlock() {
        return centerMarkerBlock == null || centerMarkerBlock.isBlank()
                ? "minecraft:beacon"
                : centerMarkerBlock;
    }

    public void setCenterMarkerBlock(String centerMarkerBlock) {
        if (centerMarkerBlock == null || centerMarkerBlock.isBlank()) {
            centerMarkerBlock = "minecraft:beacon";
        }

        this.centerMarkerBlock = centerMarkerBlock;
    }

    public boolean isCenterMarkerEnabled() {
        return centerMarkerEnabled;
    }

    public void setCenterMarkerEnabled(boolean centerMarkerEnabled) {
        this.centerMarkerEnabled = centerMarkerEnabled;
    }

    public boolean isRepeatingGridEnabled() {
        return repeatingGridEnabled;
    }

    public void setRepeatingGridEnabled(boolean repeatingGridEnabled) {
        this.repeatingGridEnabled = repeatingGridEnabled;
    }

    public int getRepeatingGridOriginX() {
        return repeatingGridOriginX;
    }

    public int getRepeatingGridOriginZ() {
        return repeatingGridOriginZ;
    }

    public void setRepeatingGridOrigin(int x, int z) {
        this.repeatingGridOriginX = x;
        this.repeatingGridOriginZ = z;
    }

    public String getLayersPreset() {
        return layersPreset == null || layersPreset.isBlank()
                ? "minecraft:bedrock,1;minecraft:dirt,3;minecraft:grass_block,1"
                : layersPreset;
    }

    public void setLayersPreset(String layersPreset) {
        if (layersPreset == null || layersPreset.isBlank()) {
            layersPreset = "";
        }
        this.layersPreset = layersPreset;
    }

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String FILE_NAME = "personalspace_config.json";

    private static Path getDimensionPath(ServerLevel level) {
        return getDimensionPath(level.getServer(), level.dimension());
    }

    private static Path getDimensionPath(
            MinecraftServer server,
            ResourceKey<Level> dimensionKey
    ) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        ResourceLocation dimensionId = dimensionKey.location();

        return worldRoot
                .resolve("dimensions")
                .resolve(dimensionId.getNamespace())
                .resolve(dimensionId.getPath());
    }

    private static Path getLegacyDimensionPath(ServerLevel level) {
        Path worldRoot = level.getServer().getWorldPath(LevelResource.ROOT);
        String dimName = level.dimension().location().getPath();

        return worldRoot.resolve(dimName);
    }

    public static PersonalSpaceData load(
            MinecraftServer server,
            ResourceKey<Level> dimensionKey
    ) {
        Path dimPath = getDimensionPath(server, dimensionKey);
        File configFile = dimPath.resolve(FILE_NAME).toFile();

        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                PersonalSpaceData data = GSON.fromJson(reader, PersonalSpaceData.class);
                if (data != null) {
                    data.normalize();
                    return data;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new PersonalSpaceData();
    }

    public static PersonalSpaceData load(ServerLevel level) {
        Path dimPath = getDimensionPath(level);
        File configFile = dimPath.resolve(FILE_NAME).toFile();

        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                PersonalSpaceData data = GSON.fromJson(reader, PersonalSpaceData.class);

                if (data != null) {
                    data.normalize();
                    return data;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File legacyConfigFile = getLegacyDimensionPath(level).resolve(FILE_NAME).toFile();

        if (legacyConfigFile.exists()) {
            try (Reader reader = new FileReader(legacyConfigFile)) {
                PersonalSpaceData data = GSON.fromJson(reader, PersonalSpaceData.class);

                if (data != null) {
                    data.normalize();
                    save(level, data);
                    return data;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new PersonalSpaceData();
    }

    public static void save(ServerLevel level, PersonalSpaceData data) {
        save(level.getServer(), level.dimension(), data);
    }

    public static void save(
            MinecraftServer server,
            ResourceKey<Level> dimensionKey,
            PersonalSpaceData data
    ) {
        Path dimPath = getDimensionPath(server, dimensionKey);
        File configFile = dimPath.resolve(FILE_NAME).toFile();
        configFile.getParentFile().mkdirs();
        data.normalize();
        try (Writer writer = new FileWriter(configFile)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void normalize() {
        if (type == null) {
            type = WorldType.VOID;
        }

        timeOfDay = normalizeTime(timeOfDay);

        skyRed = clampColor(skyRed);
        skyGreen = clampColor(skyGreen);
        skyBlue = clampColor(skyBlue);

        starBrightness = Math.max(0.0F, Math.min(1.0F, starBrightness));

        if (biomeName == null || biomeName.isBlank()) {
            biomeName = "minecraft:plains";
        }

        if (layersPreset == null) {
            layersPreset = "";
        }
        boundaryChunksX = Math.max(0, Math.min(16, boundaryChunksX));
        boundaryChunksZ = Math.max(0, Math.min(16, boundaryChunksZ));
        gapChunks = Math.max(0, Math.min(16, gapChunks));

        if (boundaryBlock == null || boundaryBlock.isBlank()) {
            boundaryBlock = "minecraft:yellow_concrete";
        }

        if (roadBlock == null || roadBlock.isBlank()) {
            roadBlock = "minecraft:black_concrete";
        }

        if (centerMarkerBlock == null || centerMarkerBlock.isBlank()) {
            centerMarkerBlock = "minecraft:beacon";
        }
        if (respawnPosY <= -64 || respawnPosY >= 320) {
            resetRespawnPos();
        }

        protectionOwnerType = normalizeMetadata(protectionOwnerType);
        protectionOwnerId = normalizeMetadata(protectionOwnerId);
        protectionOwnerName = normalizeMetadata(protectionOwnerName);
        protectionTeamId = normalizeMetadata(protectionTeamId).replace("-", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeMetadata(String value) {
        return value == null ? "" : value.trim();
    }

    private static long normalizeTime(long value) {
        long result = value % 24000L;
        if (result < 0L) {
            result += 24000L;
        }
        return result;
    }
    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
    public WorldType getType() {
        return type;
    }
    public void setType(WorldType type) {
        this.type = type == null ? WorldType.VOID : type;
    }
    public int getGroundLevel() {
        return groundLevel;
    }
    public void setGroundLevel(int groundLevel) {
        this.groundLevel = groundLevel;

        if (respawnPosX == 7 && respawnPosZ == 7) {
            int oldDefaultY = respawnPosY;
            int newDefaultY = groundLevel + 2;

            if (oldDefaultY <= 0 || Math.abs(oldDefaultY - newDefaultY) <= 4) {
                respawnPosY = newDefaultY;
            }
        }
    }
    public String getReturnLevel() {
        return returnLevel;
    }
    public void setReturnLevel(String returnLevel) {
        this.returnLevel = returnLevel;
    }
    public BlockPos getReturnPos() {
        return new BlockPos(returnPosX, returnPosY, returnPosZ);
    }
    public void setReturnPos(BlockPos pos) {
        if (pos == null) {
            return;
        }
        this.returnPosX = pos.getX();
        this.returnPosY = pos.getY();
        this.returnPosZ = pos.getZ();
    }

    public BlockPos getRespawnPos() {
        return new BlockPos(respawnPosX, respawnPosY, respawnPosZ);
    }

    public void setRespawnPos(BlockPos pos) {
        if (pos == null) {
            return;
        }

        this.respawnPosX = pos.getX();
        this.respawnPosY = pos.getY();
        this.respawnPosZ = pos.getZ();
    }

    public void resetRespawnPos() {
        BlockPos defaultRespawnPos = getDefaultRespawnPos();

        this.respawnPosX = defaultRespawnPos.getX();
        this.respawnPosY = defaultRespawnPos.getY();
        this.respawnPosZ = defaultRespawnPos.getZ();
    }

    public BlockPos getDefaultRespawnPos() {
        return new BlockPos(7, groundLevel + 2, 7);
    }

    public long getTimeOfDay() {
        return normalizeTime(timeOfDay);
    }
    public void setTimeOfDay(long timeOfDay) {
        this.timeOfDay = normalizeTime(timeOfDay);
    }
    public int getSkyRed() {
        return clampColor(skyRed);
    }
    public int getSkyGreen() {
        return clampColor(skyGreen);
    }
    public int getSkyBlue() {
        return clampColor(skyBlue);
    }
    public void setSkyColor(int red, int green, int blue) {
        this.skyRed = clampColor(red);
        this.skyGreen = clampColor(green);
        this.skyBlue = clampColor(blue);
    }

    public String getProtectionOwnerType() { return protectionOwnerType == null ? "" : protectionOwnerType; }
    public String getProtectionOwnerId() { return protectionOwnerId == null ? "" : protectionOwnerId; }
    public String getProtectionOwnerName() { return protectionOwnerName == null ? "" : protectionOwnerName; }
    public String getProtectionTeamId() { return protectionTeamId == null ? "" : protectionTeamId; }

    public void setProtectionOwnerPlayer(java.util.UUID playerId, String playerName, java.util.UUID teamId) {
        this.protectionOwnerType = "player";
        this.protectionOwnerId = playerId == null ? "" : playerId.toString();
        this.protectionOwnerName = playerName == null ? "" : playerName.trim();
        this.protectionTeamId = teamId == null ? "" : teamId.toString().replace("-", "").toLowerCase(java.util.Locale.ROOT);
    }

    public void setProtectionOwnerTeam(String compactTeamId) {
        this.protectionOwnerType = "team";
        this.protectionOwnerId = compactTeamId == null ? "" : compactTeamId.replace("-", "").toLowerCase(java.util.Locale.ROOT);
        this.protectionOwnerName = "";
        this.protectionTeamId = this.protectionOwnerId;
    }

    public boolean hasProtectionOwner() {
        return !getProtectionOwnerType().isBlank() && (!getProtectionOwnerId().isBlank() || !getProtectionOwnerName().isBlank());
    }

}