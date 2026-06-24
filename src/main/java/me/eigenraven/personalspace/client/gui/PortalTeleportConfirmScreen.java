package me.eigenraven.personalspace.client.gui;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.client.ClientPersonalSpaceSettings;
import me.eigenraven.personalspace.network.UpdatePersonalSpaceSettingsPacket;
import me.eigenraven.personalspace.network.UsePortalPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

public final class PortalTeleportConfirmScreen extends Screen {
    private final BlockPos portalPos;
    private final ResourceLocation clickedLevelId;
    private final Component dimensionName;
    private final boolean personalSpaceLevel;

    private int selectedTime;
    private int selectedRed;
    private int selectedGreen;
    private int selectedBlue;
    private float selectedStarBrightness;

    private String lockedBiomeName;
    private boolean lockedTreesEnabled;
    private boolean lockedFoliageEnabled;
    private boolean selectedWeatherEnabled;
    private boolean selectedCloudsEnabled;
    private String lockedLayersPreset;

    private Button timeButton;

    private static final long[] TIME_VALUES = {
            1000L,
            6000L,
            12000L,
            18000L
    };

    private enum TimePreset {
        MORNING("settings.time.morning"),
        DAY("settings.time.day"),
        EVENING("settings.time.evening"),
        NIGHT("settings.time.night");

        private final String key;

        TimePreset(String key) {
            this.key = key;
        }
    }

    private static final TimePreset[] TIME_PRESETS = {
            TimePreset.MORNING,
            TimePreset.DAY,
            TimePreset.EVENING,
            TimePreset.NIGHT
    };

    public PortalTeleportConfirmScreen(
            BlockPos portalPos,
            ResourceLocation clickedLevelId,
            Component dimensionName
    ) {
        super(Component.translatable("screen.personalspace.portal.title"));
        this.portalPos = portalPos;
        this.clickedLevelId = clickedLevelId;
        this.dimensionName = dimensionName;
        this.personalSpaceLevel = clickedLevelId.getNamespace().equals(PersonalSpace.MODID);

        if (personalSpaceLevel) {
            ClientPersonalSpaceSettings.Settings settings = ClientPersonalSpaceSettings.get(clickedLevelId);

            this.selectedTime = (int) settings.timeOfDay();
            this.selectedRed = settings.skyRed();
            this.selectedGreen = settings.skyGreen();
            this.selectedBlue = settings.skyBlue();
            this.selectedStarBrightness = settings.starBrightness();

            this.lockedBiomeName = settings.biomeName();
            this.lockedTreesEnabled = settings.treesEnabled();
            this.lockedFoliageEnabled = settings.foliageEnabled();

            this.selectedWeatherEnabled = settings.weatherEnabled();
            this.selectedCloudsEnabled = settings.cloudsEnabled();

            this.lockedLayersPreset = settings.layersPreset();
        }
    }

    private static Component settingsText(String key) {
        return Component.translatable("gui.personalspace." + key);
    }

    private static Component settingsText(String key, Object... args) {
        return Component.translatable("gui.personalspace." + key, args);
    }

    private static Component onOff(boolean value) {
        return value ? settingsText("on") : settingsText("off");
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int startY = personalSpaceLevel ? height / 2 - 145 : height / 2 - 45;

        addRenderableWidget(Button.builder(
                Component.translatable("screen.personalspace.portal.enter"),
                button -> {
                    PacketDistributor.sendToServer(new UsePortalPacket(
                            portalPos,
                            clickedLevelId
                    ));

                    Minecraft.getInstance().setScreen(null);
                }
        ).bounds(centerX - 120, startY + 45, 240, 20).build());

        if (!personalSpaceLevel) {
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.cancel"),
                    button -> Minecraft.getInstance().setScreen(null)
            ).bounds(centerX - 120, startY + 75, 240, 20).build());

            return;
        }

        timeButton = Button.builder(
                getTimeButtonText(),
                button -> cycleTime()
        ).bounds(centerX - 120, startY + 90, 240, 20).build();

        addRenderableWidget(timeButton);

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 120,
                240,
                20,
                0,
                255,
                selectedRed,
                value -> settingsText("settings.sky_r", value),
                value -> {
                    selectedRed = value;
                    preview();
                }
        ));

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 150,
                240,
                20,
                0,
                255,
                selectedGreen,
                value -> settingsText("settings.sky_g", value),
                value -> {
                    selectedGreen = value;
                    preview();
                }
        ));

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 180,
                240,
                20,
                0,
                255,
                selectedBlue,
                value -> settingsText("settings.sky_b", value),
                value -> {
                    selectedBlue = value;
                    preview();
                }
        ));

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 210,
                240,
                20,
                0,
                100,
                Math.round(selectedStarBrightness * 100.0F),
                value -> settingsText(
                        "settings.star_brightness",
                        String.format(Locale.ROOT, "%.2f", value / 100.0F)
                ),
                value -> {
                    selectedStarBrightness = value / 100.0F;
                    preview();
                }
        ));

        addRenderableWidget(Button.builder(
                settingsText("settings.weather", onOff(selectedWeatherEnabled)),
                button -> {
                    selectedWeatherEnabled = !selectedWeatherEnabled;
                    button.setMessage(settingsText("settings.weather", onOff(selectedWeatherEnabled)));
                    preview();
                }
        ).bounds(centerX - 120, startY + 245, 115, 20).build());

        addRenderableWidget(Button.builder(
                settingsText("settings.clouds", onOff(selectedCloudsEnabled)),
                button -> {
                    selectedCloudsEnabled = !selectedCloudsEnabled;
                    button.setMessage(settingsText("settings.clouds", onOff(selectedCloudsEnabled)));
                    preview();
                }
        ).bounds(centerX + 5, startY + 245, 115, 20).build());

        addRenderableWidget(Button.builder(
                settingsText("settings.save"),
                button -> {
                    PacketDistributor.sendToServer(new UpdatePersonalSpaceSettingsPacket(
                            selectedTime,
                            selectedRed,
                            selectedGreen,
                            selectedBlue,
                            selectedStarBrightness,
                            lockedBiomeName,
                            lockedTreesEnabled,
                            lockedFoliageEnabled,
                            selectedWeatherEnabled,
                            selectedCloudsEnabled,
                            lockedLayersPreset
                    ));

                    Minecraft.getInstance().setScreen(null);
                }
        ).bounds(centerX - 120, startY + 280, 115, 20).build());

        addRenderableWidget(Button.builder(
                settingsText("settings.cancel"),
                button -> Minecraft.getInstance().setScreen(null)
        ).bounds(centerX + 5, startY + 280, 115, 20).build());

        preview();
    }

    private void preview() {
        if (!personalSpaceLevel) {
            return;
        }

        ClientPersonalSpaceSettings.set(
                clickedLevelId,
                selectedTime,
                selectedRed,
                selectedGreen,
                selectedBlue,
                selectedStarBrightness,
                lockedBiomeName,
                lockedTreesEnabled,
                lockedFoliageEnabled,
                selectedWeatherEnabled,
                selectedCloudsEnabled,
                lockedLayersPreset
        );
    }

    private int getTimeIndex() {
        long normalized = selectedTime % 24000L;

        if (normalized < 0L) {
            normalized += 24000L;
        }

        int bestIndex = 0;
        long bestDistance = Long.MAX_VALUE;

        for (int i = 0; i < TIME_VALUES.length; i++) {
            long distance = Math.abs(TIME_VALUES[i] - normalized);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private Component getTimeButtonText() {
        return settingsText("settings.time", settingsText(TIME_PRESETS[getTimeIndex()].key));
    }

    private void cycleTime() {
        int nextIndex = (getTimeIndex() + 1) % TIME_VALUES.length;
        selectedTime = (int) TIME_VALUES[nextIndex];

        if (timeButton != null) {
            timeButton.setMessage(getTimeButtonText());
        }

        preview();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPersonalSpaceBackground(graphics);

        int centerX = width / 2;
        int startY = personalSpaceLevel ? height / 2 - 145 : height / 2 - 45;
        int boxHeight = personalSpaceLevel ? 330 : 120;

        graphics.fill(centerX - 145, startY - 10, centerX + 145, startY + boxHeight, 0xCC101010);
        graphics.fill(centerX - 143, startY - 8, centerX + 143, startY + boxHeight - 2, 0xCC303030);

        graphics.drawCenteredString(
                font,
                title,
                centerX,
                startY + 5,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                font,
                dimensionName,
                centerX,
                startY + 25,
                0xA0D8FF
        );

        if (personalSpaceLevel) {
            graphics.drawCenteredString(
                    font,
                    settingsText("settings.title"),
                    centerX,
                    startY + 70,
                    0xFFFFFF
            );

            graphics.drawString(
                    font,
                    settingsText("settings.worldgen_locked"),
                    centerX - 120,
                    startY + 307,
                    0x777777,
                    false
            );

            graphics.drawString(
                    font,
                    settingsText("settings.locked_biome", lockedBiomeName),
                    centerX - 120,
                    startY + 319,
                    0x777777,
                    false
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    private void renderPersonalSpaceBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0x66000000);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}