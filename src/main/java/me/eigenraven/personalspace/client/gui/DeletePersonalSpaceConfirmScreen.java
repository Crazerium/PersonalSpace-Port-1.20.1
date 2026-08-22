package me.eigenraven.personalspace.client.gui;

import me.eigenraven.personalspace.network.DeletePersonalSpacePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DeletePersonalSpaceConfirmScreen extends Screen {
    private final Screen parent;
    private final ResourceLocation levelId;

    public DeletePersonalSpaceConfirmScreen(Screen parent, ResourceLocation levelId) {
        super(Component.translatable("screen.personalspace.delete.title"));
        this.parent = parent;
        this.levelId = levelId;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;

        addRenderableWidget(Button.builder(
                Component.translatable("screen.personalspace.delete.confirm"),
                button -> {
                    PacketDistributor.sendToServer(new DeletePersonalSpacePacket(levelId));
                    Minecraft.getInstance().setScreen(null);
                }
        ).bounds(centerX - 120, centerY + 35, 115, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.personalspace.delete.cancel"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(centerX + 5, centerY + 35, 115, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPersonalSpaceBackground(graphics);

        int centerX = width / 2;
        int centerY = height / 2;

        graphics.drawCenteredString(font, Component.translatable("screen.personalspace.delete.question"), centerX, centerY - 55, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.personalspace.delete.warning_1"), centerX, centerY - 35, 0xAAAAAA);
        graphics.drawCenteredString(font, Component.translatable("screen.personalspace.delete.warning_2"), centerX, centerY - 22, 0xAAAAAA);
        graphics.drawCenteredString(font, Component.translatable("screen.personalspace.delete.warning_3"), centerX, centerY - 5, 0xFF5555);

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
