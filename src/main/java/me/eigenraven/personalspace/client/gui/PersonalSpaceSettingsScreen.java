package me.eigenraven.personalspace.client.gui;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.client.ClientPersonalSpaceSettings;
import me.eigenraven.personalspace.data.PersonalSpacePresets;
import me.eigenraven.personalspace.network.UpdatePersonalSpaceSettingsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PersonalSpaceSettingsScreen extends Screen {
    private final ResourceLocation levelId;

    private int selectedTime;
    private int selectedRed;
    private int selectedGreen;
    private int selectedBlue;
    private float selectedStarBrightness;
    private String selectedBiomeName;

    private boolean selectedTreesEnabled;
    private boolean selectedFoliageEnabled;
    private boolean selectedWeatherEnabled;
    private boolean selectedCloudsEnabled;

    private String selectedLayersPreset;

    private Button treesButton;
    private Button foliageButton;
    private Button weatherButton;
    private Button cloudsButton;
    private EditBox biomeNameBox;
    private static final long[] TIME_VALUES = {
            1000L,   // morning
            6000L,   // day
            12000L,  // evening
            18000L   // night
    };

    private static final String[] TIME_NAMES = {
            "Утро",
            "День",
            "Вечер",
            "Ночь"
    };

    private Button timeButton;
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
        return Component.literal("Время: " + TIME_NAMES[getTimeIndex()]);
    }

    private void cycleTime() {
        int nextIndex = (getTimeIndex() + 1) % TIME_VALUES.length;
        selectedTime = (int) TIME_VALUES[nextIndex];

        if (timeButton != null) {
            timeButton.setMessage(getTimeButtonText());
        }

        preview();
    }

    public PersonalSpaceSettingsScreen(ResourceLocation levelId) {
        super(Component.literal("Personal Space Settings"));
        this.levelId = levelId;

        ClientPersonalSpaceSettings.Settings settings = ClientPersonalSpaceSettings.get(levelId);

        this.selectedTime = (int) settings.timeOfDay();
        this.selectedRed = settings.skyRed();
        this.selectedGreen = settings.skyGreen();
        this.selectedBlue = settings.skyBlue();

        this.selectedStarBrightness = settings.starBrightness();
        this.selectedBiomeName = settings.biomeName();

        this.selectedTreesEnabled = settings.treesEnabled();
        this.selectedFoliageEnabled = settings.foliageEnabled();
        this.selectedWeatherEnabled = settings.weatherEnabled();
        this.selectedCloudsEnabled = settings.cloudsEnabled();

        this.selectedLayersPreset = settings.layersPreset();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int startY = height / 2 - 80;

        timeButton = Button.builder(
                getTimeButtonText(),
                button -> cycleTime()
        ).bounds(centerX - 120, startY, 240, 20).build();

        addRenderableWidget(timeButton);

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 30,
                240,
                20,
                0,
                255,
                selectedRed,
                value -> Component.literal("Sky R: " + value),
                value -> {
                    selectedRed = value;
                    preview();
                }
        ));

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 60,
                240,
                20,
                0,
                255,
                selectedGreen,
                value -> Component.literal("Sky G: " + value),
                value -> {
                    selectedGreen = value;
                    preview();
                }
        ));

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 90,
                240,
                20,
                0,
                255,
                selectedBlue,
                value -> Component.literal("Sky B: " + value),
                value -> {
                    selectedBlue = value;
                    preview();
                }
        ));
        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 120,
                240,
                20,
                0,
                100,
                Math.round(selectedStarBrightness * 100.0F),
                value -> Component.literal("Star brightness: " + String.format("%.2f", value / 100.0F)),
                value -> {
                    selectedStarBrightness = value / 100.0F;
                    preview();
                }
        ));
        biomeNameBox = new EditBox(
                font,
                centerX - 120,
                startY + 150,
                240,
                20,
                Component.literal("Biome name")
        );

        biomeNameBox.setValue(selectedBiomeName);
        biomeNameBox.setMaxLength(128);
        biomeNameBox.setResponder(value -> {
            selectedBiomeName = value;
            preview();
        });

        addRenderableWidget(biomeNameBox);
        treesButton = Button.builder(
                toggleText("Trees", selectedTreesEnabled),
                button -> {
                    selectedTreesEnabled = !selectedTreesEnabled;
                    button.setMessage(toggleText("Trees", selectedTreesEnabled));
                    preview();
                }
        ).bounds(centerX - 120, startY + 180, 115, 20).build();

        foliageButton = Button.builder(
                toggleText("Foliage", selectedFoliageEnabled),
                button -> {
                    selectedFoliageEnabled = !selectedFoliageEnabled;
                    button.setMessage(toggleText("Foliage", selectedFoliageEnabled));
                    preview();
                }
        ).bounds(centerX + 5, startY + 180, 115, 20).build();

        weatherButton = Button.builder(
                toggleText("Weather", selectedWeatherEnabled),
                button -> {
                    selectedWeatherEnabled = !selectedWeatherEnabled;
                    button.setMessage(toggleText("Weather", selectedWeatherEnabled));
                    preview();
                }
        ).bounds(centerX - 120, startY + 205, 115, 20).build();

        cloudsButton = Button.builder(
                toggleText("Clouds", selectedCloudsEnabled),
                button -> {
                    selectedCloudsEnabled = !selectedCloudsEnabled;
                    button.setMessage(toggleText("Clouds", selectedCloudsEnabled));
                    preview();
                }
        ).bounds(centerX + 5, startY + 205, 115, 20).build();

        addRenderableWidget(treesButton);
        addRenderableWidget(foliageButton);
        addRenderableWidget(weatherButton);
        addRenderableWidget(cloudsButton);
        int presetY = startY + 235;
        int presetX = centerX - 120;

        for (int i = 1; i <= 4; i++) {
            final int presetIndex = i;

            addRenderableWidget(Button.builder(
                    presetText(i),
                    button -> applyPreset(presetIndex)
            ).bounds(presetX + (i - 1) * 45, presetY, 40, 20).build());
        }
        addRenderableWidget(Button.builder(
                Component.literal("Сохранить"),
                button -> {
                    PersonalSpace.CHANNEL.sendToServer(new UpdatePersonalSpaceSettingsPacket(
                            selectedTime,
                            selectedRed,
                            selectedGreen,
                            selectedBlue,
                            selectedStarBrightness,
                            selectedBiomeName,
                            selectedTreesEnabled,
                            selectedFoliageEnabled,
                            selectedWeatherEnabled,
                            selectedCloudsEnabled,
                            selectedLayersPreset
                    ));

                    Minecraft.getInstance().setScreen(null);
                }
        ).bounds(centerX - 120, startY + 265, 115, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Отмена"),
                button -> Minecraft.getInstance().setScreen(null)
        ).bounds(centerX + 5, startY + 265, 115, 20).build());

        preview();
    }
    private void preview() {
        ClientPersonalSpaceSettings.set(
                levelId,
                selectedTime,
                selectedRed,
                selectedGreen,
                selectedBlue,
                selectedStarBrightness,
                selectedBiomeName,
                selectedTreesEnabled,
                selectedFoliageEnabled,
                selectedWeatherEnabled,
                selectedCloudsEnabled,
                selectedLayersPreset
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.drawCenteredString(
                font,
                Component.literal("Настройки Personal Space"),
                width / 2,
                height / 2 - 110,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                font,
                Component.literal("Shift + ПКМ по порталу открывает это меню"),
                width / 2,
                height / 2 - 96,
                0xAAAAAA
        );

        graphics.drawString(
                font,
                Component.literal("Biome name"),
                width / 2 - 120,
                height / 2 + 64,
                0xFFFFFF,
                false
        );

        graphics.drawString(
                font,
                Component.literal("Presets"),
                width / 2 - 120,
                height / 2 + 149,
                0xFFFFFF,
                false
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    private Component toggleText(String name, boolean value) {
        return Component.literal(name + ": " + (value ? "ON" : "OFF"));
    }

    private Component presetText(int index) {
        return Component.literal(String.valueOf(index));
    }

    private void applyPreset(int index) {
        switch (index) {
            case 1 -> selectedLayersPreset = PersonalSpacePresets.VOID;
            case 2 -> selectedLayersPreset = PersonalSpacePresets.SHORT_GRASS;
            case 3 -> selectedLayersPreset = PersonalSpacePresets.TALL_GRASS;
            case 4 -> selectedLayersPreset = PersonalSpacePresets.STONE_PLATFORM;
            default -> {
                return;
            }
        }

        preview();
    }
}