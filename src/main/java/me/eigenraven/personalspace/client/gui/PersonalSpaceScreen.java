package me.eigenraven.personalspace.client.gui;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.data.PersonalSpacePresets;
import me.eigenraven.personalspace.network.CreateDimensionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.stream.IntStream;

public class PersonalSpaceScreen extends Screen {
    private static final long[] TIME_VALUES = {
            1000L,
            6000L,
            12000L,
            18000L
    };

    private static final String[] TIME_NAMES = {
            "Утро",
            "День",
            "Вечер",
            "Ночь"
    };

    private PersonalSpaceData.WorldType selectedType = PersonalSpaceData.WorldType.VOID;
    private int selectedHeight = 64;

    private long selectedTime = 6000L;

    private int selectedRed = 128;
    private int selectedGreen = 192;
    private int selectedBlue = 255;

    private float selectedStarBrightness = 1.0F;
    private String selectedBiomeName = "minecraft:plains";

    private boolean selectedTreesEnabled = false;
    private boolean selectedFoliageEnabled = false;
    private boolean selectedWeatherEnabled = false;
    private boolean selectedCloudsEnabled = false;

    private String selectedLayersPreset = PersonalSpacePresets.VOID;

    private Button timeButton;
    private Button typeButton;
    private Button treesButton;
    private Button foliageButton;
    private Button weatherButton;
    private Button cloudsButton;
    private EditBox biomeNameBox;

    private final Level level;
    private final BlockPos portalPos;

    public PersonalSpaceScreen(Level level, BlockPos portalPos) {
        super(Component.translatable("gui.personalspace.create"));
        this.level = level;
        this.portalPos = portalPos;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int startY = height / 2 - 120;

        timeButton = Button.builder(
                getTimeButtonText(),
                button -> cycleTime()
        ).bounds(centerX - 120, startY, 240, 20).build();

        addRenderableWidget(timeButton);

        typeButton = Button.builder(
                getTypeButtonText(),
                button -> cycleWorldType()
        ).bounds(centerX - 120, startY + 25, 240, 20).build();

        addRenderableWidget(typeButton);

        CycleButton<Integer> heightSlider = CycleButton.<Integer>builder(
                        value -> Component.literal("Высота: " + value)
                )
                .withValues(IntStream.range(0, 256).boxed().toList())
                .withInitialValue(selectedHeight)
                .displayOnlyValue()
                .create(
                        centerX - 120,
                        startY + 50,
                        240,
                        20,
                        Component.empty(),
                        (button, value) -> selectedHeight = value
                );

        addRenderableWidget(heightSlider);

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 80,
                240,
                20,
                0,
                255,
                selectedRed,
                value -> Component.literal("Sky R: " + value),
                value -> selectedRed = value
        ));

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 105,
                240,
                20,
                0,
                255,
                selectedGreen,
                value -> Component.literal("Sky G: " + value),
                value -> selectedGreen = value
        ));

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 130,
                240,
                20,
                0,
                255,
                selectedBlue,
                value -> Component.literal("Sky B: " + value),
                value -> selectedBlue = value
        ));

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY + 155,
                240,
                20,
                0,
                100,
                Math.round(selectedStarBrightness * 100.0F),
                value -> Component.literal("Star brightness: " + String.format("%.2f", value / 100.0F)),
                value -> selectedStarBrightness = value / 100.0F
        ));

        biomeNameBox = new EditBox(
                font,
                centerX - 120,
                startY + 185,
                240,
                20,
                Component.literal("Biome name")
        );

        biomeNameBox.setValue(selectedBiomeName);
        biomeNameBox.setMaxLength(128);
        biomeNameBox.setResponder(value -> selectedBiomeName = value);

        addRenderableWidget(biomeNameBox);

        treesButton = Button.builder(
                toggleText("Trees", selectedTreesEnabled),
                button -> {
                    selectedTreesEnabled = !selectedTreesEnabled;
                    button.setMessage(toggleText("Trees", selectedTreesEnabled));
                }
        ).bounds(centerX - 120, startY + 215, 115, 20).build();

        foliageButton = Button.builder(
                toggleText("Foliage", selectedFoliageEnabled),
                button -> {
                    selectedFoliageEnabled = !selectedFoliageEnabled;
                    button.setMessage(toggleText("Foliage", selectedFoliageEnabled));
                }
        ).bounds(centerX + 5, startY + 215, 115, 20).build();

        weatherButton = Button.builder(
                toggleText("Weather", selectedWeatherEnabled),
                button -> {
                    selectedWeatherEnabled = !selectedWeatherEnabled;
                    button.setMessage(toggleText("Weather", selectedWeatherEnabled));
                }
        ).bounds(centerX - 120, startY + 240, 115, 20).build();

        cloudsButton = Button.builder(
                toggleText("Clouds", selectedCloudsEnabled),
                button -> {
                    selectedCloudsEnabled = !selectedCloudsEnabled;
                    button.setMessage(toggleText("Clouds", selectedCloudsEnabled));
                }
        ).bounds(centerX + 5, startY + 240, 115, 20).build();

        addRenderableWidget(treesButton);
        addRenderableWidget(foliageButton);
        addRenderableWidget(weatherButton);
        addRenderableWidget(cloudsButton);

        int presetY = startY + 270;
        int presetX = centerX - 120;

        for (int i = 1; i <= 4; i++) {
            final int presetIndex = i;

            addRenderableWidget(Button.builder(
                    Component.literal(String.valueOf(i)),
                    button -> applyPreset(presetIndex)
            ).bounds(presetX + (i - 1) * 45, presetY, 40, 20).build());
        }

        Button createBtn = Button.builder(
                Component.literal("Создать и телепортироваться"),
                button -> {
                    ResourceLocation sourceLevelId = level.dimension().location();

                    PersonalSpace.CHANNEL.sendToServer(new CreateDimensionPacket(
                            selectedType,
                            selectedHeight,
                            portalPos,
                            sourceLevelId,

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
        ).bounds(centerX - 120, startY + 305, 240, 20).build();

        addRenderableWidget(createBtn);

        addRenderableWidget(Button.builder(
                Component.literal("Отмена"),
                button -> Minecraft.getInstance().setScreen(null)
        ).bounds(centerX - 120, startY + 330, 240, 20).build());
    }

    private void cycleWorldType() {
        selectedType = selectedType == PersonalSpaceData.WorldType.VOID
                ? PersonalSpaceData.WorldType.FLAT
                : PersonalSpaceData.WorldType.VOID;

        if (typeButton != null) {
            typeButton.setMessage(getTypeButtonText());
        }
    }

    private Component getTypeButtonText() {
        return Component.literal("Тип мира: " + selectedType.name());
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
        return Component.literal("Время: " + TIME_NAMES[getTimeIndex()]);
    }

    private void cycleTime() {
        int nextIndex = (getTimeIndex() + 1) % TIME_VALUES.length;
        selectedTime = TIME_VALUES[nextIndex];

        if (timeButton != null) {
            timeButton.setMessage(getTimeButtonText());
        }
    }

    private Component toggleText(String name, boolean value) {
        return Component.literal(name + ": " + (value ? "ON" : "OFF"));
    }

    private void applyPreset(int index) {
        switch (index) {
            case 1 -> {
                selectedType = PersonalSpaceData.WorldType.VOID;
                selectedHeight = 64;
                selectedLayersPreset = PersonalSpacePresets.VOID;
            }
            case 2 -> {
                selectedType = PersonalSpaceData.WorldType.FLAT;
                selectedHeight = 64;
                selectedLayersPreset = PersonalSpacePresets.SHORT_GRASS;
            }
            case 3 -> {
                selectedType = PersonalSpaceData.WorldType.FLAT;
                selectedHeight = 80;
                selectedLayersPreset = PersonalSpacePresets.TALL_GRASS;
            }
            case 4 -> {
                selectedType = PersonalSpaceData.WorldType.FLAT;
                selectedHeight = 64;
                selectedLayersPreset = PersonalSpacePresets.STONE_PLATFORM;
            }
            default -> {
                return;
            }
        }

        if (typeButton != null) {
            typeButton.setMessage(getTypeButtonText());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.drawCenteredString(
                font,
                Component.literal("Создание Personal Space"),
                width / 2,
                height / 2 - 150,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                font,
                Component.literal("Настройки генерации задаются до создания мира"),
                width / 2,
                height / 2 - 136,
                0xAAAAAA
        );

        graphics.drawString(
                font,
                Component.literal("Biome name"),
                width / 2 - 120,
                height / 2 + 61,
                0xFFFFFF,
                false
        );

        graphics.drawString(
                font,
                Component.literal("Presets"),
                width / 2 - 120,
                height / 2 + 146,
                0xFFFFFF,
                false
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}