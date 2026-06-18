package me.eigenraven.personalspace.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.Path;

public class PersonalSpaceData {
    public enum WorldType { VOID, FLAT }

    private WorldType type = WorldType.VOID;
    private int groundLevel = 64;

    // Данные для возврата
    private String returnLevel = null;
    private int returnPosX = 0;
    private int returnPosY = 64;
    private int returnPosZ = 0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "personalspace_config.json";

    private static Path getDimensionPath(ServerLevel level) {
        Path worldRoot = level.getServer().getWorldPath(LevelResource.ROOT);
        String dimName = level.dimension().location().getPath();
        return worldRoot.resolve(dimName);
    }

    public static PersonalSpaceData load(ServerLevel level) {
        Path dimPath = getDimensionPath(level);
        File configFile = dimPath.resolve(FILE_NAME).toFile();
        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                PersonalSpaceData data = GSON.fromJson(reader, PersonalSpaceData.class);
                if (data != null) return data;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new PersonalSpaceData();
    }

    public static void save(ServerLevel level, PersonalSpaceData data) {
        Path dimPath = getDimensionPath(level);
        File configFile = dimPath.resolve(FILE_NAME).toFile();
        configFile.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(configFile)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WorldType getType() { return type; }
    public void setType(WorldType type) { this.type = type; }
    public int getGroundLevel() { return groundLevel; }
    public void setGroundLevel(int groundLevel) { this.groundLevel = groundLevel; }

    public String getReturnLevel() { return returnLevel; }
    public void setReturnLevel(String returnLevel) { this.returnLevel = returnLevel; }
    public BlockPos getReturnPos() { return new BlockPos(returnPosX, returnPosY, returnPosZ); }
    public void setReturnPos(BlockPos pos) { this.returnPosX = pos.getX(); this.returnPosY = pos.getY(); this.returnPosZ = pos.getZ(); }
}