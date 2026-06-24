package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class UpdatePersonalSpaceSettingsPacket {
    private final long timeOfDay;
    private final int skyRed;
    private final int skyGreen;
    private final int skyBlue;

    private final float starBrightness;
    private final String biomeName;

    private final boolean treesEnabled;
    private final boolean foliageEnabled;
    private final boolean weatherEnabled;
    private final boolean cloudsEnabled;

    private final String layersPreset;

    public UpdatePersonalSpaceSettingsPacket(
            long timeOfDay,
            int skyRed,
            int skyGreen,
            int skyBlue,
            float starBrightness,
            String biomeName,
            boolean treesEnabled,
            boolean foliageEnabled,
            boolean weatherEnabled,
            boolean cloudsEnabled,
            String layersPreset
    ) {
        this.timeOfDay = timeOfDay;
        this.skyRed = skyRed;
        this.skyGreen = skyGreen;
        this.skyBlue = skyBlue;
        this.starBrightness = starBrightness;
        this.biomeName = biomeName;
        this.treesEnabled = treesEnabled;
        this.foliageEnabled = foliageEnabled;
        this.weatherEnabled = weatherEnabled;
        this.cloudsEnabled = cloudsEnabled;
        this.layersPreset = layersPreset;
    }

    public static void encode(UpdatePersonalSpaceSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.timeOfDay);
        buf.writeInt(msg.skyRed);
        buf.writeInt(msg.skyGreen);
        buf.writeInt(msg.skyBlue);

        buf.writeFloat(msg.starBrightness);
        buf.writeUtf(msg.biomeName);

        buf.writeBoolean(msg.treesEnabled);
        buf.writeBoolean(msg.foliageEnabled);
        buf.writeBoolean(msg.weatherEnabled);
        buf.writeBoolean(msg.cloudsEnabled);

        buf.writeUtf(msg.layersPreset);
    }

    public static UpdatePersonalSpaceSettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdatePersonalSpaceSettingsPacket(
                buf.readLong(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),

                buf.readFloat(),
                buf.readUtf(),

                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),

                buf.readUtf()
        );
    }

    public void handleServerSide(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (!level.dimension().location().getNamespace().equals(PersonalSpace.MODID)) {
            return;
        }

        PersonalSpaceData data = PersonalSpaceData.load(level);

        data.setTimeOfDay(timeOfDay);
        data.setSkyColor(skyRed, skyGreen, skyBlue);

        data.setStarBrightness(starBrightness);
        data.setBiomeName(biomeName);

        data.setTreesEnabled(treesEnabled);
        data.setFoliageEnabled(foliageEnabled);
        data.setWeatherEnabled(weatherEnabled);
        data.setCloudsEnabled(cloudsEnabled);

        data.setLayersPreset(layersPreset);

        PersonalSpaceData.save(level, data);

        level.setDayTime(data.getTimeOfDay());

        if (!data.isWeatherEnabled()) {
            level.setRainLevel(0.0F);
            level.setThunderLevel(0.0F);
            level.setWeatherParameters(6000, 0, false, false);
        }

        PersonalSpaceSettingsSync.syncToPlayersIn(level);
    }

    public long timeOfDay() {
        return timeOfDay;
    }

    public int skyRed() {
        return skyRed;
    }

    public int skyGreen() {
        return skyGreen;
    }

    public int skyBlue() {
        return skyBlue;
    }

    public float starBrightness() {
        return starBrightness;
    }

    public String biomeName() {
        return biomeName;
    }

    public boolean treesEnabled() {
        return treesEnabled;
    }

    public boolean foliageEnabled() {
        return foliageEnabled;
    }

    public boolean weatherEnabled() {
        return weatherEnabled;
    }

    public boolean cloudsEnabled() {
        return cloudsEnabled;
    }

    public String layersPreset() {
        return layersPreset;
    }
}