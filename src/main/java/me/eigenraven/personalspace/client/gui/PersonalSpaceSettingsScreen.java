package me.eigenraven.personalspace.client.gui;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.client.ClientPersonalSpaceSettings;
import me.eigenraven.personalspace.network.UpdatePersonalSpaceSettingsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PersonalSpaceSettingsScreen extends Screen {
    private final ResourceLocation levelId;

    private int selectedTime;
    private int selectedRed;
    private int selectedGreen;
    private int selectedBlue;

    public PersonalSpaceSettingsScreen(ResourceLocation levelId) {
        super(Component.literal("Personal Space Settings"));
        this.levelId = levelId;

        ClientPersonalSpaceSettings.Settings settings = ClientPersonalSpaceSettings.get(levelId);

        this.selectedTime = (int) settings.timeOfDay();
        this.selectedRed = settings.skyRed();
        this.selectedGreen = settings.skyGreen();
        this.selectedBlue = settings.skyBlue();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int startY = height / 2 - 80;

        addRenderableWidget(new IntSliderButton(
                centerX - 120,
                startY,
                240,
                20,
                0,
                23999,
                selectedTime,
                value -> Component.literal("Время суток: " + value),
                value -> {
                    selectedTime = value;
                    preview();
                }
        ));

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

        addRenderableWidget(Button.builder(
                Component.literal("День"),
                button -> {
                    selectedTime = 6000;
                    rebuildWidgets();
                }
        ).bounds(centerX - 120, startY + 125, 75, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Закат"),
                button -> {
                    selectedTime = 12000;
                    rebuildWidgets();
                }
        ).bounds(centerX - 37, startY + 125, 75, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Ночь"),
                button -> {
                    selectedTime = 18000;
                    rebuildWidgets();
                }
        ).bounds(centerX + 46, startY + 125, 75, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Сохранить"),
                button -> {
                    PersonalSpace.CHANNEL.sendToServer(new UpdatePersonalSpaceSettingsPacket(
                            selectedTime,
                            selectedRed,
                            selectedGreen,
                            selectedBlue
                    ));

                    Minecraft.getInstance().setScreen(null);
                }
        ).bounds(centerX - 120, startY + 155, 115, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Отмена"),
                button -> Minecraft.getInstance().setScreen(null)
        ).bounds(centerX + 5, startY + 155, 115, 20).build());

        preview();
    }
    private void preview() {
        ClientPersonalSpaceSettings.set(
                levelId,
                selectedTime,
                selectedRed,
                selectedGreen,
                selectedBlue
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

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}