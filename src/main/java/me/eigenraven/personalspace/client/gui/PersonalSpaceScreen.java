package me.eigenraven.personalspace.client.gui;

import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.network.CreateDimensionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class PersonalSpaceScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int BASE_PANEL_HEIGHT = 225;
    private static final int ADVANCED_PANEL_HEIGHT = 350;
    private static final int PREVIEW_WIDTH = 190;
    private static final int PREVIEW_HEIGHT = 210;
    private static final int PREVIEW_GAP = 10;

    private static final int MIN_GROUND_LEVEL = 1;
    private static final int MAX_GROUND_LEVEL = 240;
    private static final int MIN_CHUNKS_VALUE = 0;
    private static final int MAX_CHUNKS_VALUE = 16;

    private PersonalSpaceData.WorldType selectedType = PersonalSpaceData.WorldType.VOID;
    private int selectedHeight = 64;

    private long timeOfDay = 6000L;

    private int skyRed = 80;
    private int skyGreen = 120;
    private int skyBlue = 255;

    private float starBrightness = 1.0F;
    private String biomeName = "minecraft:plains";

    private boolean treesEnabled = false;
    private boolean foliageEnabled = false;
    private boolean weatherEnabled = false;
    private boolean cloudsEnabled = true;

    private String layersPreset = "minecraft:obsidian,1";

    private int boundaryChunksX = 0;
    private int boundaryChunksZ = 0;
    private int gapChunks = 0;

    private String boundaryBlock = "minecraft:barrier";
    private String roadBlock = "minecraft:stone";
    private String centerMarkerBlock = "minecraft:glowstone";

    private boolean centerMarkerEnabled = false;
    private boolean advancedVisible = false;
    private boolean repeatingGridEnabled = false;

    private WorldPreset selectedPreset = WorldPreset.VOID;
    private AdvancedPage advancedPage = AdvancedPage.WORLD;

    private final Level level;
    private final BlockPos portalPos;

    private EditBox heightField;
    private EditBox timeField;
    private EditBox biomeField;
    private EditBox skyRedField;
    private EditBox skyGreenField;
    private EditBox skyBlueField;
    private EditBox starBrightnessField;

    private EditBox layersPresetField;
    private EditBox boundaryBlockField;
    private EditBox roadBlockField;
    private EditBox centerMarkerBlockField;
    private EditBox boundaryChunksXField;
    private EditBox boundaryChunksZField;
    private EditBox gapChunksField;

    private Button createButton;

    public PersonalSpaceScreen(Level level, BlockPos portalPos) {
        super(text("create"));
        this.level = level;
        this.portalPos = portalPos;
    }

    private enum WorldPreset {
        VOID("preset.void"),
        FLAT("preset.flat"),
        TECH("preset.tech"),
        NIGHT_VOID("preset.night_void"),
        ROAD_GRID("preset.road_grid");

        private final String translationKey;

        WorldPreset(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private enum AdvancedPage {
        WORLD,
        BLOCKS
    }

    private static Component text(String key) {
        return Component.translatable("gui.personalspace." + key);
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.personalspace." + key, args);
    }

    @Override
    protected void init() {
        super.init();

        int panelHeight = getPanelHeight();
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - panelHeight) / 2;

        int left = panelX + 20;
        int top = panelY + 36;
        int widgetWidth = PANEL_WIDTH - 40;

        CycleButton<PersonalSpaceData.WorldType> worldTypeButton =
                CycleButton.<PersonalSpaceData.WorldType>builder(this::worldTypeName)
                        .withValues(PersonalSpaceData.WorldType.VOID, PersonalSpaceData.WorldType.FLAT)
                        .withInitialValue(selectedType)
                        .create(
                                left,
                                top,
                                widgetWidth,
                                20,
                                text("world_type"),
                                (button, value) -> selectedType = value
                        );

        worldTypeButton.setTooltip(Tooltip.create(text("world_type.tooltip")));
        addRenderableWidget(worldTypeButton);

        CycleButton<WorldPreset> presetButton =
                CycleButton.<WorldPreset>builder(preset -> text(preset.translationKey))
                        .withValues(
                                WorldPreset.VOID,
                                WorldPreset.FLAT,
                                WorldPreset.TECH,
                                WorldPreset.NIGHT_VOID,
                                WorldPreset.ROAD_GRID
                        )
                        .withInitialValue(selectedPreset)
                        .create(
                                left,
                                top + 28,
                                widgetWidth,
                                20,
                                text("preset"),
                                (button, value) -> {
                                    selectedPreset = value;
                                    applyPreset(value);
                                    rebuildPersonalSpaceWidgets();
                                }
                        );

        presetButton.setTooltip(Tooltip.create(text("preset.tooltip")));
        addRenderableWidget(presetButton);

        heightField = new EditBox(
                font,
                left,
                top + 56,
                widgetWidth,
                20,
                text("ground_height")
        );
        heightField.setValue(Integer.toString(selectedHeight));
        heightField.setMaxLength(3);
        heightField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,3}"));
        heightField.setResponder(value -> {
            if (hasValidHeight()) {
                selectedHeight = getClampedHeight();
            }

            updateCreateButtonState();
        });
        heightField.setTooltip(Tooltip.create(text(
                "height.tooltip",
                MIN_GROUND_LEVEL,
                MAX_GROUND_LEVEL
        )));
        addRenderableWidget(heightField);

        int smallButtonWidth = (widgetWidth - 18) / 4;

        addRenderableWidget(Button.builder(
                Component.literal("-16"),
                button -> changeHeight(-16)
        ).bounds(left, top + 84, smallButtonWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("-1"),
                button -> changeHeight(-1)
        ).bounds(left + smallButtonWidth + 6, top + 84, smallButtonWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("+1"),
                button -> changeHeight(1)
        ).bounds(left + (smallButtonWidth + 6) * 2, top + 84, smallButtonWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("+16"),
                button -> changeHeight(16)
        ).bounds(left + (smallButtonWidth + 6) * 3, top + 84, smallButtonWidth, 20).build());

        int advancedTop = top + 118;

        if (advancedVisible) {
            addAdvancedWidgets(left, advancedTop, widgetWidth);
        }

        if (!advancedVisible) {
            addRenderableWidget(Button.builder(
                    text("advanced.hidden"),
                    button -> {
                        advancedVisible = true;
                        advancedPage = AdvancedPage.WORLD;
                        rebuildPersonalSpaceWidgets();
                    }
            ).bounds(left, panelY + panelHeight - 58, widgetWidth, 20).build());
        }

        int createButtonY = advancedVisible ? advancedTop + 174 : panelY + panelHeight - 30;

        createButton = Button.builder(
                text("create_and_teleport"),
                button -> {
                    if (!hasValidHeight() || !hasValidChunkSettings()) {
                        updateCreateButtonState();
                        return;
                    }

                    selectedHeight = getClampedHeight();

                    PacketDistributor.sendToServer(new CreateDimensionPacket(
                            selectedType,
                            selectedHeight,
                            portalPos,
                            level.dimension().location(),

                            timeOfDay,
                            skyRed,
                            skyGreen,
                            skyBlue,

                            starBrightness,
                            biomeName,

                            treesEnabled,
                            foliageEnabled,
                            weatherEnabled,
                            cloudsEnabled,

                            layersPreset,
                            boundaryChunksX,
                            boundaryChunksZ,
                            gapChunks,

                            boundaryBlock,
                            roadBlock,
                            centerMarkerBlock,

                            centerMarkerEnabled,
                            repeatingGridEnabled
                    ));

                    Minecraft.getInstance().setScreen(null);
                }
        ).bounds(left, createButtonY, widgetWidth, 20).build();

        addRenderableWidget(createButton);
        updateCreateButtonState();
    }

    private void applyPreset(WorldPreset preset) {
        switch (preset) {
            case VOID -> {
                selectedType = PersonalSpaceData.WorldType.VOID;
                selectedHeight = 64;

                timeOfDay = 6000L;

                skyRed = 80;
                skyGreen = 120;
                skyBlue = 255;

                starBrightness = 1.0F;
                biomeName = "minecraft:plains";

                treesEnabled = false;
                foliageEnabled = false;
                weatherEnabled = false;
                cloudsEnabled = true;

                layersPreset = "minecraft:obsidian,1";

                boundaryChunksX = 0;
                boundaryChunksZ = 0;
                gapChunks = 0;

                boundaryBlock = "minecraft:barrier";
                roadBlock = "minecraft:stone";
                centerMarkerBlock = "minecraft:glowstone";

                centerMarkerEnabled = false;
                repeatingGridEnabled = false;
            }

            case FLAT -> {
                selectedType = PersonalSpaceData.WorldType.FLAT;
                selectedHeight = 64;

                timeOfDay = 6000L;

                skyRed = 100;
                skyGreen = 150;
                skyBlue = 255;

                starBrightness = 0.6F;
                biomeName = "minecraft:plains";

                treesEnabled = true;
                foliageEnabled = true;
                weatherEnabled = true;
                cloudsEnabled = true;

                layersPreset = "minecraft:bedrock,1;minecraft:dirt,2;minecraft:grass_block,1";

                boundaryChunksX = 0;
                boundaryChunksZ = 0;
                gapChunks = 0;

                boundaryBlock = "minecraft:barrier";
                roadBlock = "minecraft:stone_bricks";
                centerMarkerBlock = "minecraft:glowstone";

                centerMarkerEnabled = false;
                repeatingGridEnabled = false;
            }

            case TECH -> {
                selectedType = PersonalSpaceData.WorldType.FLAT;
                selectedHeight = 64;

                timeOfDay = 6000L;

                skyRed = 45;
                skyGreen = 55;
                skyBlue = 70;

                starBrightness = 0.2F;
                biomeName = "minecraft:the_void";

                treesEnabled = false;
                foliageEnabled = false;
                weatherEnabled = false;
                cloudsEnabled = false;

                layersPreset = "minecraft:bedrock,1;minecraft:smooth_stone,3";

                boundaryChunksX = 0;
                boundaryChunksZ = 0;
                gapChunks = 0;

                boundaryBlock = "minecraft:barrier";
                roadBlock = "minecraft:light_gray_concrete";
                centerMarkerBlock = "minecraft:white_concrete";

                centerMarkerEnabled = false;
                repeatingGridEnabled = false;
            }

            case NIGHT_VOID -> {
                selectedType = PersonalSpaceData.WorldType.VOID;
                selectedHeight = 80;

                timeOfDay = 18000L;

                skyRed = 10;
                skyGreen = 15;
                skyBlue = 35;

                starBrightness = 1.0F;
                biomeName = "minecraft:the_void";

                treesEnabled = false;
                foliageEnabled = false;
                weatherEnabled = false;
                cloudsEnabled = false;

                layersPreset = "minecraft:obsidian,1";

                boundaryChunksX = 0;
                boundaryChunksZ = 0;
                gapChunks = 0;

                boundaryBlock = "minecraft:barrier";
                roadBlock = "minecraft:deepslate_tiles";
                centerMarkerBlock = "minecraft:end_rod";

                centerMarkerEnabled = false;
                repeatingGridEnabled = false;
            }

            case ROAD_GRID -> {
                selectedType = PersonalSpaceData.WorldType.FLAT;
                selectedHeight = 64;

                timeOfDay = 6000L;

                skyRed = 120;
                skyGreen = 170;
                skyBlue = 255;

                starBrightness = 0.5F;
                biomeName = "minecraft:plains";

                treesEnabled = false;
                foliageEnabled = false;
                weatherEnabled = false;
                cloudsEnabled = true;

                layersPreset = "minecraft:bedrock,1;minecraft:dirt,3;minecraft:grass_block,1";

                boundaryChunksX = 2;
                boundaryChunksZ = 2;
                gapChunks = 1;

                boundaryBlock = "minecraft:white_concrete";
                roadBlock = "minecraft:cobbled_deepslate";
                centerMarkerBlock = "minecraft:sea_lantern";

                centerMarkerEnabled = true;
                repeatingGridEnabled = true;
            }

        }
    }

    private void addAdvancedWidgets(int left, int top, int widgetWidth) {
        if (advancedPage == AdvancedPage.WORLD) {
            addWorldAdvancedWidgets(left, top, widgetWidth);
        } else {
            addBlocksAdvancedWidgets(left, top, widgetWidth);
        }
    }

    private void addWorldAdvancedWidgets(int left, int top, int widgetWidth) {
        int halfWidth = (widgetWidth - 8) / 2;
        int thirdWidth = (widgetWidth - 16) / 3;

        timeField = new EditBox(
                font,
                left,
                top,
                halfWidth,
                20,
                text("time")
        );
        timeField.setValue(Long.toString(timeOfDay));
        timeField.setMaxLength(5);
        timeField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,5}"));
        timeField.setResponder(value -> {
            try {
                long parsed = Long.parseLong(value);
                timeOfDay = Math.max(0L, Math.min(24000L, parsed));
            } catch (NumberFormatException ignored) {
            }
        });
        timeField.setTooltip(Tooltip.create(text("time.tooltip")));
        addRenderableWidget(timeField);

        starBrightnessField = new EditBox(
                font,
                left + halfWidth + 8,
                top,
                halfWidth,
                20,
                text("star_brightness")
        );
        starBrightnessField.setValue(Float.toString(starBrightness));
        starBrightnessField.setMaxLength(4);
        starBrightnessField.setFilter(value -> value.isEmpty() || value.matches("\\d?(\\.\\d{0,2})?"));
        starBrightnessField.setResponder(value -> {
            try {
                starBrightness = Mth.clamp(Float.parseFloat(value), 0.0F, 1.0F);
            } catch (NumberFormatException ignored) {
            }
        });
        starBrightnessField.setTooltip(Tooltip.create(text("star_brightness.tooltip")));
        addRenderableWidget(starBrightnessField);

        biomeField = new EditBox(
                font,
                left,
                top + 30,
                widgetWidth,
                20,
                text("biome")
        );
        biomeField.setValue(biomeName);
        biomeField.setMaxLength(80);
        biomeField.setFilter(value -> value.isEmpty() || value.matches("[a-z0-9_:.\\-/]+"));
        biomeField.setResponder(value -> {
            if (!value.isBlank()) {
                biomeName = value.trim();
            }
        });
        biomeField.setTooltip(Tooltip.create(text("biome.tooltip")));
        addRenderableWidget(biomeField);

        skyRedField = createColorField(left, top + 60, thirdWidth, "sky_red", skyRed, value -> skyRed = value);
        skyGreenField = createColorField(left + thirdWidth + 8, top + 60, thirdWidth, "sky_green", skyGreen, value -> skyGreen = value);
        skyBlueField = createColorField(left + (thirdWidth + 8) * 2, top + 60, thirdWidth, "sky_blue", skyBlue, value -> skyBlue = value);

        addRenderableWidget(skyRedField);
        addRenderableWidget(skyGreenField);
        addRenderableWidget(skyBlueField);

        addRenderableWidget(CycleButton.onOffBuilder(treesEnabled)
                .create(
                        left,
                        top + 90,
                        halfWidth,
                        20,
                        text("trees"),
                        (button, value) -> treesEnabled = value
                ));

        addRenderableWidget(CycleButton.onOffBuilder(foliageEnabled)
                .create(
                        left + halfWidth + 8,
                        top + 90,
                        halfWidth,
                        20,
                        text("foliage"),
                        (button, value) -> foliageEnabled = value
                ));

        addRenderableWidget(CycleButton.onOffBuilder(weatherEnabled)
                .create(
                        left,
                        top + 120,
                        halfWidth,
                        20,
                        text("weather"),
                        (button, value) -> weatherEnabled = value
                ));

        addRenderableWidget(CycleButton.onOffBuilder(cloudsEnabled)
                .create(
                        left + halfWidth + 8,
                        top + 120,
                        halfWidth,
                        20,
                        text("clouds"),
                        (button, value) -> cloudsEnabled = value
                ));

        addRenderableWidget(CycleButton.onOffBuilder(centerMarkerEnabled)
                .create(
                        left,
                        top + 150,
                        halfWidth,
                        20,
                        text("center_marker"),
                        (button, value) -> centerMarkerEnabled = value
                ));

        addRenderableWidget(Button.builder(
                text("blocks_and_boundaries"),
                button -> {
                    advancedPage = AdvancedPage.BLOCKS;
                    rebuildPersonalSpaceWidgets();
                }
        ).bounds(left + halfWidth + 8, top + 150, halfWidth, 20).build());
    }

    private void addBlocksAdvancedWidgets(int left, int top, int widgetWidth) {
        int halfWidth = (widgetWidth - 8) / 2;
        int thirdWidth = (widgetWidth - 16) / 3;

        layersPresetField = new EditBox(
                font,
                left,
                top,
                widgetWidth,
                20,
                text("layers")
        );
        layersPresetField.setValue(layersPreset);
        layersPresetField.setMaxLength(160);
        layersPresetField.setFilter(value -> value.isEmpty() || value.matches("[a-z0-9_:.\\-/,;]+"));
        layersPresetField.setResponder(value -> {
            if (!value.isBlank()) {
                layersPreset = value.trim();
            }
        });
        layersPresetField.setTooltip(Tooltip.create(text("layers.tooltip")));
        addRenderableWidget(layersPresetField);

        addRenderableWidget(createBlockPickerButton(
                left,
                top + 30,
                halfWidth,
                "boundary_block",
                boundaryBlock,
                value -> boundaryBlock = value
        ));

        addRenderableWidget(createBlockPickerButton(
                left + halfWidth + 8,
                top + 30,
                halfWidth,
                "road_block",
                roadBlock,
                value -> roadBlock = value
        ));

        addRenderableWidget(createBlockPickerButton(
                left,
                top + 60,
                widgetWidth,
                "center_block",
                centerMarkerBlock,
                value -> centerMarkerBlock = value
        ));

        boundaryChunksXField = createIntField(
                left,
                top + 90,
                thirdWidth,
                "boundary_x",
                boundaryChunksX,
                MIN_CHUNKS_VALUE,
                MAX_CHUNKS_VALUE,
                value -> boundaryChunksX = value
        );

        boundaryChunksZField = createIntField(
                left + thirdWidth + 8,
                top + 90,
                thirdWidth,
                "boundary_z",
                boundaryChunksZ,
                MIN_CHUNKS_VALUE,
                MAX_CHUNKS_VALUE,
                value -> boundaryChunksZ = value
        );

        gapChunksField = createIntField(
                left + (thirdWidth + 8) * 2,
                top + 90,
                thirdWidth,
                "gap",
                gapChunks,
                MIN_CHUNKS_VALUE,
                MAX_CHUNKS_VALUE,
                value -> gapChunks = value
        );

        addRenderableWidget(boundaryChunksXField);
        addRenderableWidget(boundaryChunksZField);
        addRenderableWidget(gapChunksField);

        addRenderableWidget(Button.builder(
                text("back_to_world"),
                button -> {
                    advancedPage = AdvancedPage.WORLD;
                    rebuildPersonalSpaceWidgets();
                }
        ).bounds(left, top + 150, halfWidth, 20).build());

        addRenderableWidget(Button.builder(
                text("advanced.open"),
                button -> {
                    advancedVisible = false;
                    advancedPage = AdvancedPage.WORLD;
                    rebuildPersonalSpaceWidgets();
                }
        ).bounds(left + halfWidth + 8, top + 150, halfWidth, 20).build());
    }

    private EditBox createColorField(
            int x,
            int y,
            int width,
            String labelKey,
            int currentValue,
            IntValueSetter setter
    ) {
        EditBox field = new EditBox(
                font,
                x,
                y,
                width,
                20,
                text(labelKey)
        );

        field.setValue(Integer.toString(currentValue));
        field.setMaxLength(3);
        field.setFilter(value -> value.isEmpty() || value.matches("\\d{1,3}"));
        field.setResponder(value -> {
            try {
                setter.set(Mth.clamp(Integer.parseInt(value), 0, 255));
            } catch (NumberFormatException ignored) {
            }
        });
        field.setTooltip(Tooltip.create(text("color.tooltip", text(labelKey))));

        return field;
    }

    private EditBox createBlockIdField(
            int x,
            int y,
            int width,
            String labelKey,
            String currentValue,
            StringValueSetter setter
    ) {
        EditBox field = new EditBox(
                font,
                x,
                y,
                width,
                20,
                text(labelKey)
        );

        field.setValue(currentValue);
        field.setMaxLength(80);
        field.setFilter(value -> value.isEmpty() || value.matches("[a-z0-9_:.\\-/]+"));
        field.setResponder(value -> {
            if (!value.isBlank()) {
                setter.set(value.trim());
            }
        });
        field.setTooltip(Tooltip.create(text("block.tooltip", text(labelKey))));

        return field;
    }

    private EditBox createIntField(
            int x,
            int y,
            int width,
            String labelKey,
            int currentValue,
            int minValue,
            int maxValue,
            IntValueSetter setter
    ) {
        EditBox field = new EditBox(font, x, y, width, 20, text(labelKey));
        field.setValue(Integer.toString(currentValue));
        field.setMaxLength(3);
        field.setFilter(value -> value.isEmpty() || value.matches("\\d{1,3}"));

        field.setResponder(value -> {
            try {
                int parsed = Integer.parseInt(value);
                setter.set(parsed);
            } catch (NumberFormatException ignored) {
            }

            updateCreateButtonState();
        });

        field.setTooltip(Tooltip.create(text("int.tooltip", text(labelKey), minValue, maxValue)));
        return field;
    }

    private int getPanelHeight() {
        return advancedVisible ? ADVANCED_PANEL_HEIGHT : BASE_PANEL_HEIGHT;
    }

    private void rebuildPersonalSpaceWidgets() {
        clearWidgets();
        init();
    }

    private Component worldTypeName(PersonalSpaceData.WorldType type) {
        return switch (type) {
            case VOID -> text("world_type.void");
            case FLAT -> text("world_type.flat");
        };
    }

    private void changeHeight(int delta) {
        int next = Mth.clamp(getHeightOrDefault() + delta, MIN_GROUND_LEVEL, MAX_GROUND_LEVEL);
        selectedHeight = next;

        if (heightField != null) {
            heightField.setValue(Integer.toString(next));
        }

        updateCreateButtonState();
    }

    private int getHeightOrDefault() {
        if (heightField == null) {
            return selectedHeight;
        }

        try {
            return Integer.parseInt(heightField.getValue());
        } catch (NumberFormatException ignored) {
            return selectedHeight;
        }
    }

    private int getClampedHeight() {
        return Mth.clamp(getHeightOrDefault(), MIN_GROUND_LEVEL, MAX_GROUND_LEVEL);
    }

    private boolean hasValidHeight() {
        if (heightField == null) {
            return selectedHeight >= MIN_GROUND_LEVEL && selectedHeight <= MAX_GROUND_LEVEL;
        }

        try {
            int value = Integer.parseInt(heightField.getValue());
            return value >= MIN_GROUND_LEVEL && value <= MAX_GROUND_LEVEL;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean hasValidChunkSettings() {
        return isValidIntValue(boundaryChunksX, MIN_CHUNKS_VALUE, MAX_CHUNKS_VALUE)
                && isValidIntValue(boundaryChunksZ, MIN_CHUNKS_VALUE, MAX_CHUNKS_VALUE)
                && isValidIntValue(gapChunks, MIN_CHUNKS_VALUE, MAX_CHUNKS_VALUE)
                && isValidIntField(boundaryChunksXField, MIN_CHUNKS_VALUE, MAX_CHUNKS_VALUE)
                && isValidIntField(boundaryChunksZField, MIN_CHUNKS_VALUE, MAX_CHUNKS_VALUE)
                && isValidIntField(gapChunksField, MIN_CHUNKS_VALUE, MAX_CHUNKS_VALUE);
    }

    private boolean isValidIntField(EditBox field, int minValue, int maxValue) {
        if (field == null) {
            return true;
        }

        try {
            int value = Integer.parseInt(field.getValue().trim());
            return isValidIntValue(value, minValue, maxValue);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean isValidIntValue(int value, int minValue, int maxValue) {
        return value >= minValue && value <= maxValue;
    }


    private void updateCreateButtonState() {
        if (createButton != null) {
            createButton.active = hasValidHeight() && hasValidChunkSettings();
        }
    }
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPersonalSpaceBackground(graphics);

        int panelHeight = getPanelHeight();
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - panelHeight) / 2;

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xCC101010);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, 0xFF666666);
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + PANEL_WIDTH, panelY + panelHeight, 0xFF000000);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF666666);
        graphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xFF000000);

        graphics.drawCenteredString(
                font,
                text("create"),
                width / 2,
                panelY + 12,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                font,
                text("subtitle"),
                width / 2,
                panelY + 24,
                0xA0A0A0
        );

        if (hasValidHeight()) {
            graphics.drawCenteredString(
                    font,
                    text("portal_y", getClampedHeight() + 1),
                    width / 2,
                    panelY + 146,
                    0x808080
            );
        } else {
            graphics.drawCenteredString(
                    font,
                    text("height_error", MIN_GROUND_LEVEL, MAX_GROUND_LEVEL),
                    width / 2,
                    panelY + 146,
                    0xFF5555
            );
        }
        renderPresetPreview(graphics, panelX, panelY, panelHeight);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPresetPreview(GuiGraphics graphics, int panelX, int panelY, int panelHeight) {
        int previewX = panelX + PANEL_WIDTH + PREVIEW_GAP;
        int previewY = panelY;

        if (previewX + PREVIEW_WIDTH > width - 6) {
            return;
        }

        int previewHeight = Math.min(Math.max(230, panelHeight), height - previewY - 6);

        graphics.fill(previewX, previewY, previewX + PREVIEW_WIDTH, previewY + previewHeight, 0xCC101010);
        graphics.fill(previewX, previewY, previewX + PREVIEW_WIDTH, previewY + 1, 0xFF666666);
        graphics.fill(previewX, previewY + previewHeight - 1, previewX + PREVIEW_WIDTH, previewY + previewHeight, 0xFF000000);
        graphics.fill(previewX, previewY, previewX + 1, previewY + previewHeight, 0xFF666666);
        graphics.fill(previewX + PREVIEW_WIDTH - 1, previewY, previewX + PREVIEW_WIDTH, previewY + previewHeight, 0xFF000000);

        int textX = previewX + 10;
        int y = previewY + 10;

        graphics.drawString(font, text("preview.title"), textX, y, 0xFFFFFF, false);
        y += 14;

        graphics.drawString(font, text("preview.map"), textX, y, 0xA0A0A0, false);
        y += 10;

        int mapWidth = PREVIEW_WIDTH - 20;
        int mapHeight = 120;

        renderTopDownMiniMap(graphics, previewX + 10, y, mapWidth, mapHeight);

        y += mapHeight + 8;

        graphics.drawString(font, text("preview.preset", text(selectedPreset.translationKey)), textX, y, 0xA0A0A0, false);
        y += 12;

        graphics.drawString(font, text("preview.size", boundaryChunksX, boundaryChunksZ), textX, y, 0xA0A0A0, false);
        y += 12;

        graphics.drawString(font, text("preview.gap", gapChunks), textX, y, 0xA0A0A0, false);
        y += 12;

        graphics.drawString(font, text("preview.type", worldTypeName(selectedType)), textX, y, 0xA0A0A0, false);
    }

    private Button createBlockPickerButton(
            int x,
            int y,
            int width,
            String labelKey,
            String currentValue,
            StringValueSetter setter
    ) {
        return Button.builder(
                text("block_picker.button", text(labelKey), shortenBlockIdForPreview(currentValue)),
                button -> Minecraft.getInstance().setScreen(new PersonalSpaceBlockPickerScreen(
                        this,
                        text(labelKey),
                        currentValue,
                        value -> {
                            setter.set(value);
                            rebuildPersonalSpaceWidgets();
                        }
                ))
        ).bounds(x, y, width, 20).build();
    }

    private String shortenForPreview(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String shortenBlockIdForPreview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String blockId = value;

        int colonIndex = blockId.indexOf(':');

        if (colonIndex >= 0 && colonIndex + 1 < blockId.length()) {
            blockId = blockId.substring(colonIndex + 1);
        }

        return shortenForPreview(blockId, 18);
    }


    private void renderSimplePresetPreview(GuiGraphics graphics, int x, int y, int width, int height) {
        int cellSize = Math.max(4, Math.min(width, height) / 9);
        int drawWidth = cellSize * 7;
        int drawHeight = cellSize * 7;
        int startX = x + (width - drawWidth) / 2;
        int startY = y + (height - drawHeight) / 2;

        int platformColor = getPlatformPreviewColor();
        int markerColor = getBlockPreviewColor(centerMarkerBlock, 0xFFFFFF55);

        for (int gridZ = 0; gridZ < 7; gridZ++) {
            for (int gridX = 0; gridX < 7; gridX++) {
                drawMiniMapCell(
                        graphics,
                        startX + gridX * cellSize,
                        startY + gridZ * cellSize,
                        cellSize,
                        platformColor
                );
            }
        }

        if (centerMarkerEnabled) {
            drawMiniMapCell(
                    graphics,
                    startX + 3 * cellSize,
                    startY + 3 * cellSize,
                    cellSize,
                    markerColor
            );
        }
    }

    private void renderTopDownMiniMap(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF080808);

        if (!repeatingGridEnabled) {
            renderSimplePresetPreview(graphics, x, y, width, height);
            return;
        }

        int safeBoundaryX = Math.max(1, boundaryChunksX);
        int safeBoundaryZ = Math.max(1, boundaryChunksZ);
        int safeGap = Math.max(0, gapChunks);

        int halfCellsX = safeBoundaryX + safeGap + 1;
        int halfCellsZ = safeBoundaryZ + safeGap + 1;

        int totalCellsX = halfCellsX * 2 + 1;
        int totalCellsZ = halfCellsZ * 2 + 1;

        int cellSize = Math.max(3, Math.min(width / totalCellsX, height / totalCellsZ));

        int drawWidth = totalCellsX * cellSize;
        int drawHeight = totalCellsZ * cellSize;

        int startX = x + (width - drawWidth) / 2;
        int startY = y + (height - drawHeight) / 2;

        int centerX = halfCellsX;
        int centerZ = halfCellsZ;

        int boundaryMinX = centerX - (safeBoundaryX + safeGap);
        int boundaryMaxX = centerX + (safeBoundaryX + safeGap);
        int boundaryMinZ = centerZ - (safeBoundaryZ + safeGap);
        int boundaryMaxZ = centerZ + (safeBoundaryZ + safeGap);

        int platformMinX = centerX - safeBoundaryX;
        int platformMaxX = centerX + safeBoundaryX;
        int platformMinZ = centerZ - safeBoundaryZ;
        int platformMaxZ = centerZ + safeBoundaryZ;

        int voidColor = 0xFF111111;
        int gapColor = 0xFF1C1C1C;
        int platformColor = getPlatformPreviewColor();
        int boundaryColor = getBlockPreviewColor(boundaryBlock, 0xFFFF5555);
        int roadColor = getBlockPreviewColor(roadBlock, 0xFF777777);
        int markerColor = getBlockPreviewColor(centerMarkerBlock, 0xFFFFFF55);
        int treeColor = 0xFF2E8B57;

        for (int gridZ = 0; gridZ < totalCellsZ; gridZ++) {
            for (int gridX = 0; gridX < totalCellsX; gridX++) {
                int color = voidColor;

                boolean insideBoundary =
                        gridX >= boundaryMinX && gridX <= boundaryMaxX &&
                                gridZ >= boundaryMinZ && gridZ <= boundaryMaxZ;

                boolean insidePlatform =
                        gridX >= platformMinX && gridX <= platformMaxX &&
                                gridZ >= platformMinZ && gridZ <= platformMaxZ;

                boolean onBoundaryEdge =
                        insideBoundary &&
                                (gridX == boundaryMinX || gridX == boundaryMaxX || gridZ == boundaryMinZ || gridZ == boundaryMaxZ);

                boolean onRoad =
                        (gridX == centerX && insidePlatform) ||
                                (gridZ == centerZ && insidePlatform);

                if (insideBoundary) {
                    color = gapColor;
                }

                if (selectedType == PersonalSpaceData.WorldType.FLAT && insidePlatform) {
                    color = platformColor;
                }

                if (onRoad) {
                    color = roadColor;
                }

                if (onBoundaryEdge) {
                    color = boundaryColor;
                }

                drawMiniMapCell(
                        graphics,
                        startX + gridX * cellSize,
                        startY + gridZ * cellSize,
                        cellSize,
                        color
                );
            }
        }

        if (treesEnabled && selectedType == PersonalSpaceData.WorldType.FLAT) {
            drawPreviewTree(graphics, startX, startY, cellSize, centerX - 1, centerZ - 1, treeColor);
            drawPreviewTree(graphics, startX, startY, cellSize, centerX + 1, centerZ - 1, treeColor);
            drawPreviewTree(graphics, startX, startY, cellSize, centerX - 1, centerZ + 1, treeColor);
            drawPreviewTree(graphics, startX, startY, cellSize, centerX + 1, centerZ + 1, treeColor);
        }

        if (centerMarkerEnabled) {
            drawMiniMapCell(
                    graphics,
                    startX + centerX * cellSize,
                    startY + centerZ * cellSize,
                    cellSize,
                    markerColor
            );
        }
    }

    private void drawMiniMapCell(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x, y, x + size, y + size, 0xFF000000);

        int inset = size >= 5 ? 1 : 0;
        graphics.fill(x + inset, y + inset, x + size - inset, y + size - inset, color);
    }

    private void drawPreviewTree(GuiGraphics graphics, int startX, int startY, int cellSize, int gridX, int gridZ, int color) {
        int px = startX + gridX * cellSize;
        int py = startY + gridZ * cellSize;
        drawMiniMapCell(graphics, px, py, cellSize, color);
    }

    private int getPlatformPreviewColor() {
        String topLayerBlock = getTopLayerBlockId();

        if (selectedType == PersonalSpaceData.WorldType.VOID) {
            return 0xFF111111;
        }

        return getBlockPreviewColor(topLayerBlock, 0xFF6A8F3A);
    }

    private String getTopLayerBlockId() {
        if (layersPreset == null || layersPreset.isBlank()) {
            return "minecraft:grass_block";
        }

        String[] layers = layersPreset.split(";");
        if (layers.length == 0) {
            return "minecraft:grass_block";
        }

        String last = layers[layers.length - 1];
        String[] parts = last.split(",");
        if (parts.length < 1) {
            return "minecraft:grass_block";
        }

        return parts[0].trim();
    }

    private int getBlockPreviewColor(String blockId, int fallback) {
        if (blockId == null) {
            return fallback;
        }

        String id = blockId.toLowerCase();

        if (id.contains("barrier")) return 0xFFFF5555;
        if (id.contains("glowstone")) return 0xFFFFD54F;
        if (id.contains("sea_lantern")) return 0xFFB3E5FC;
        if (id.contains("end_rod")) return 0xFFF8F8E8;
        if (id.contains("stone_bricks")) return 0xFF8A8A8A;
        if (id.contains("smooth_stone")) return 0xFF9A9A9A;
        if (id.contains("stone")) return 0xFF7A7A7A;
        if (id.contains("deepslate")) return 0xFF4A4A55;
        if (id.contains("light_gray_concrete")) return 0xFFBDBDBD;
        if (id.contains("grass_block")) return 0xFF6FAF45;
        if (id.contains("dirt")) return 0xFF8B5A2B;
        if (id.contains("sand")) return 0xFFE7D28B;
        if (id.contains("snow")) return 0xFFF2F6FF;
        if (id.contains("water")) return 0xFF3F76E4;
        if (id.contains("wood")) return 0xFF8B6B3F;
        if (id.contains("planks")) return 0xFFA67C52;

        return fallback;
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

    @FunctionalInterface
    private interface IntValueSetter {
        void set(int value);
    }

    @FunctionalInterface
    private interface StringValueSetter {
        void set(String value);
    }
}