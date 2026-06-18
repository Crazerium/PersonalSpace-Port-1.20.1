package me.eigenraven.personalspace.client.gui;


import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.network.CreateDimensionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.stream.IntStream;

public class PersonalSpaceScreen extends Screen {
    private PersonalSpaceData.WorldType selectedType = PersonalSpaceData.WorldType.VOID;
    private int selectedHeight = 64;
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

        Button voidBtn = Button.builder(
                Component.literal("Пустой мир (Void)"),
                btn -> selectedType = PersonalSpaceData.WorldType.VOID
        ).bounds(width/2 - 100, height/2 - 50, 200, 20).build();
        addRenderableWidget(voidBtn);

        Button flatBtn = Button.builder(
                Component.literal("Мир с землёй (Flat)"),
                btn -> selectedType = PersonalSpaceData.WorldType.FLAT
        ).bounds(width/2 - 100, height/2 - 20, 200, 20).build();
        addRenderableWidget(flatBtn);

        CycleButton<Integer> heightSlider = CycleButton.<Integer>builder(
                        val -> Component.literal("Высота: " + val)
                ).withValues(IntStream.range(0, 256).boxed().toList())
                .withInitialValue(selectedHeight)
                .displayOnlyValue()
                .create(width/2 - 100, height/2 + 10, 200, 20, Component.empty(), (btn, val) -> selectedHeight = val);
        addRenderableWidget(heightSlider);

        Button createBtn = Button.builder(
                Component.literal("Создать и телепортироваться"),
                btn -> {
                    PersonalSpace.CHANNEL.sendToServer(new CreateDimensionPacket(selectedType, selectedHeight, portalPos));
                    Minecraft.getInstance().setScreen(null);
                }
        ).bounds(width/2 - 100, height/2 + 50, 200, 20).build();
        addRenderableWidget(createBtn);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, Component.literal("Выберите параметры мира"), width/2, height/2 - 80, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}