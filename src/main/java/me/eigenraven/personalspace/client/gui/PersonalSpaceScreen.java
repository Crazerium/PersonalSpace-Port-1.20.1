package me.eigenraven.personalspace.client.gui;

import me.eigenraven.personalspace.PersonalSpace;
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

public class PersonalSpaceScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int BASE_PANEL_HEIGHT = 225;
    private static final int ADVANCED_PANEL_HEIGHT = 350;

    private static final int MIN_GROUND_LEVEL = 1;
    private static final int MAX_GROUND_LEVEL = 240;

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

    private String layersPreset = "minecraft:bedrock,1;minecraft:dirt,2;minecraft:grass_block,1";

    private int boundaryChunksX = 1;
    private int boundaryChunksZ = 1;
    private int gapChunks = 1;

    private String boundaryBlock = "minecraft:barrier";
    private String roadBlock = "minecraft:stone";
    private String centerMarkerBlock = "minecraft:glowstone";

    private boolean centerMarkerEnabled = true;
    private boolean advancedVisible = false;

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
        NIGHT_VOID("preset.night_void");

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
                        .withValues(WorldPreset.VOID, WorldPreset.FLAT, WorldPreset.TECH, WorldPreset.NIGHT_VOID)
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
                    selectedHeight = getClampedHeight();

                    PersonalSpace.CHANNEL.sendToServer(new CreateDimensionPacket(
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

                            centerMarkerEnabled
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

                layersPreset = "minecraft:bedrock,1;minecraft:dirt,2;minecraft:grass_block,1";

                boundaryChunksX = 1;
                boundaryChunksZ = 1;
                gapChunks = 1;

                boundaryBlock = "minecraft:barrier";
                roadBlock = "minecraft:stone";
                centerMarkerBlock = "minecraft:glowstone";

                centerMarkerEnabled = true;
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

                boundaryChunksX = 1;
                boundaryChunksZ = 1;
                gapChunks = 1;

                boundaryBlock = "minecraft:barrier";
                roadBlock = "minecraft:stone_bricks";
                centerMarkerBlock = "minecraft:glowstone";

                centerMarkerEnabled = true;
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

                boundaryChunksX = 1;
                boundaryChunksZ = 1;
                gapChunks = 1;

                boundaryBlock = "minecraft:barrier";
                roadBlock = "minecraft:light_gray_concrete";
                centerMarkerBlock = "minecraft:sea_lantern";

                centerMarkerEnabled = true;
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

                layersPreset = "minecraft:bedrock,1;minecraft:dirt,2;minecraft:grass_block,1";

                boundaryChunksX = 1;
                boundaryChunksZ = 1;
                gapChunks = 1;

                boundaryBlock = "minecraft:barrier";
                roadBlock = "minecraft:deepslate_tiles";
                centerMarkerBlock = "minecraft:end_rod";

                centerMarkerEnabled = true;
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

        boundaryBlockField = createBlockIdField(
                left,
                top + 30,
                halfWidth,
                "boundary_block",
                boundaryBlock,
                value -> boundaryBlock = value
        );

        roadBlockField = createBlockIdField(
                left + halfWidth + 8,
                top + 30,
                halfWidth,
                "road_block",
                roadBlock,
                value -> roadBlock = value
        );

        centerMarkerBlockField = createBlockIdField(
                left,
                top + 60,
                widgetWidth,
                "center_block",
                centerMarkerBlock,
                value -> centerMarkerBlock = value
        );

        addRenderableWidget(boundaryBlockField);
        addRenderableWidget(roadBlockField);
        addRenderableWidget(centerMarkerBlockField);

        boundaryChunksXField = createIntField(
                left,
                top + 90,
                thirdWidth,
                "boundary_x",
                boundaryChunksX,
                0,
                16,
                value -> boundaryChunksX = value
        );

        boundaryChunksZField = createIntField(
                left + thirdWidth + 8,
                top + 90,
                thirdWidth,
                "boundary_z",
                boundaryChunksZ,
                0,
                16,
                value -> boundaryChunksZ = value
        );

        gapChunksField = createIntField(
                left + (thirdWidth + 8) * 2,
                top + 90,
                thirdWidth,
                "gap",
                gapChunks,
                0,
                16,
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
        EditBox field = new EditBox(
                font,
                x,
                y,
                width,
                20,
                text(labelKey)
        );

        field.setValue(Integer.toString(currentValue));
        field.setMaxLength(2);
        field.setFilter(value -> value.isEmpty() || value.matches("\\d{1,2}"));
        field.setResponder(value -> {
            try {
                setter.set(Mth.clamp(Integer.parseInt(value), minValue, maxValue));
            } catch (NumberFormatException ignored) {
            }
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

    private void updateCreateButtonState() {
        if (createButton != null) {
            createButton.active = hasValidHeight();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

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

        super.render(graphics, mouseX, mouseY, partialTick);
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