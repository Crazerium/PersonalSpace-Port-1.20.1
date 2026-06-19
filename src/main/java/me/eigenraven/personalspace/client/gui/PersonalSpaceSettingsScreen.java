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