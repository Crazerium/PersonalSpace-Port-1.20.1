package me.eigenraven.personalspace.client.gui;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.network.DeletePersonalSpacePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DeletePersonalSpaceConfirmScreen extends Screen {
    private final Screen parent;
    private final ResourceLocation levelId;

    public DeletePersonalSpaceConfirmScreen(Screen parent, ResourceLocation levelId) {
        super(Component.literal("Удаление персоналки"));
        this.parent = parent;
        this.levelId = levelId;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;

        addRenderableWidget(Button.builder(
                Component.literal("§cУдалить навсегда"),
                button -> {
                    PersonalSpace.CHANNEL.sendToServer(new DeletePersonalSpacePacket(levelId));
                    Minecraft.getInstance().setScreen(null);
                }
        ).bounds(centerX - 120, centerY + 35, 115, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("§7Отмена"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(centerX + 5, centerY + 35, 115, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int centerX = width / 2;
        int centerY = height / 2;

        graphics.drawCenteredString(font, "§cУдалить персональное измерение?", centerX, centerY - 55, 0xFFFFFF);
        graphics.drawCenteredString(font, "§7Это удалит все блоки, механизмы и файлы измерения.", centerX, centerY - 35, 0xAAAAAA);
        graphics.drawCenteredString(font, "§7Для командной персоналки потребуется подтверждение участников.", centerX, centerY - 22, 0xAAAAAA);
        graphics.drawCenteredString(font, "§cДействие нельзя отменить без бэкапа.", centerX, centerY - 5, 0xFF5555);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}