package me.eigenraven.personalspace.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
    private long timeOfDay = 6000L;
    private int skyRed = 128;
    private int skyGreen = 192;
    private int skyBlue = 255;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

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

    public static void save(ServerLevel level, PersonalSpaceData data) {
        Path dimPath = getDimensionPath(level);
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
}