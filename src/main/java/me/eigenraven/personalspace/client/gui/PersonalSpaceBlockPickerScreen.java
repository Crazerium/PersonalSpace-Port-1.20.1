package me.eigenraven.personalspace.client.gui;

import me.eigenraven.personalspace.network.CreateDimensionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PersonalSpaceBlockPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 300;

    private static final int GRID_COLUMNS = 10;
    private static final int GRID_ROWS = 6;
    private static final int CELL_SIZE = 22;
    private static final int BLOCKS_PER_PAGE = GRID_COLUMNS * GRID_ROWS;

    private static final int GRID_WIDTH = GRID_COLUMNS * CELL_SIZE;
    private static final int GRID_HEIGHT = GRID_ROWS * CELL_SIZE;

    private final Screen parent;
    private final Component pickerTitle;
    private final String currentBlockId;
    private final Consumer<String> onSelected;

    private final List<ResourceLocation> allAllowedBlocks = new ArrayList<>();
    private final List<ResourceLocation> filteredBlocks = new ArrayList<>();

    private EditBox searchField;

    private int page = 0;
    private ResourceLocation hoveredBlock = null;

    public PersonalSpaceBlockPickerScreen(
            Screen parent,
            Component pickerTitle,
            String currentBlockId,
            Consumer<String> onSelected
    ) {
        super(pickerTitle);
        this.parent = parent;
        this.pickerTitle = pickerTitle;
        this.currentBlockId = currentBlockId;
        this.onSelected = onSelected;

        collectAllowedBlocks();
        rebuildFilteredBlocks("");
    }

    private static Component text(String key) {
        return Component.translatable("gui.personalspace." + key);
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.personalspace." + key, args);
    }

    private void collectAllowedBlocks() {
        allAllowedBlocks.clear();

        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            Block block = BuiltInRegistries.BLOCK.get(id);

            if (!isAllowedBlock(id, block)) {
                continue;
            }

            allAllowedBlocks.add(id);
        }

        allAllowedBlocks.sort(Comparator.comparing(ResourceLocation::toString));
    }

    private boolean isAllowedBlock(ResourceLocation id, Block block) {
        if (!CreateDimensionPacket.isAllowedPersonalSpaceBlock(id)) {
            return false;
        }

        if (block.asItem() == Items.AIR) {
            return false;
        }

        BlockState state = block.defaultBlockState();

        if (state.isAir()) {
            return false;
        }

        if (state.hasBlockEntity()) {
            return false;
        }

        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    private void rebuildFilteredBlocks(String search) {
        filteredBlocks.clear();

        String query = search == null
                ? ""
                : search.trim().toLowerCase(Locale.ROOT);

        for (ResourceLocation id : allAllowedBlocks) {
            Block block = BuiltInRegistries.BLOCK.get(id);

            String idText = id.toString().toLowerCase(Locale.ROOT);
            String nameText = block.getName().getString().toLowerCase(Locale.ROOT);

            if (query.isEmpty() || idText.contains(query) || nameText.contains(query)) {
                filteredBlocks.add(id);
            }
        }

        int maxPage = getMaxPage();

        if (page > maxPage) {
            page = maxPage;
        }
    }

    @Override
    protected void init() {
        super.init();

        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;

        int left = panelX + 16;
        int top = panelY + 38;

        searchField = new EditBox(
                font,
                left,
                top,
                PANEL_WIDTH - 32,
                20,
                text("block_picker.search")
        );
        searchField.setMaxLength(80);
        searchField.setValue("");
        searchField.setResponder(value -> {
            page = 0;
            rebuildFilteredBlocks(value);
        });

        addRenderableWidget(searchField);

        addRenderableWidget(Button.builder(
                text("block_picker.previous"),
                button -> page = Math.max(0, page - 1)
        ).bounds(left, panelY + PANEL_HEIGHT - 30, 90, 20).build());

        addRenderableWidget(Button.builder(
                text("block_picker.next"),
                button -> page = Math.min(getMaxPage(), page + 1)
        ).bounds(left + 98, panelY + PANEL_HEIGHT - 30, 90, 20).build());

        addRenderableWidget(Button.builder(
                text("settings.cancel"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(panelX + PANEL_WIDTH - 106, panelY + PANEL_HEIGHT - 30, 90, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;

        drawPanel(graphics, panelX, panelY);

        graphics.drawCenteredString(font, pickerTitle, width / 2, panelY + 10, 0xFFFFFF);

        graphics.drawCenteredString(
                font,
                text("block_picker.current", currentBlockId),
                width / 2,
                panelY + 24,
                0xA0A0A0
        );

        super.render(graphics, mouseX, mouseY, partialTick);

        renderBlockGrid(graphics, mouseX, mouseY, panelX, panelY);

        graphics.drawCenteredString(
                font,
                text("block_picker.page", page + 1, getMaxPage() + 1),
                width / 2,
                panelY + PANEL_HEIGHT - 45,
                0x808080
        );

        if (filteredBlocks.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    text("block_picker.no_results"),
                    width / 2,
                    panelY + 145,
                    0xFF7777
            );
        }

        if (hoveredBlock != null) {
            Block block = BuiltInRegistries.BLOCK.get(hoveredBlock);
            graphics.renderTooltip(font, block.getName(), mouseX, mouseY);
        }
    }

    private void drawPanel(GuiGraphics graphics, int panelX, int panelY) {
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC101010);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, 0xFF666666);
        graphics.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF000000);
        graphics.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, 0xFF666666);
        graphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF000000);
    }

    private void renderBlockGrid(GuiGraphics graphics, int mouseX, int mouseY, int panelX, int panelY) {
        hoveredBlock = null;

        int gridX = panelX + (PANEL_WIDTH - GRID_WIDTH) / 2;
        int gridY = panelY + 66;

        graphics.fill(gridX - 4, gridY - 4, gridX + GRID_WIDTH + 4, gridY + GRID_HEIGHT + 4, 0xFF000000);
        graphics.fill(gridX - 3, gridY - 3, gridX + GRID_WIDTH + 3, gridY + GRID_HEIGHT + 3, 0xFF555555);

        int startIndex = page * BLOCKS_PER_PAGE;
        int endIndex = Math.min(startIndex + BLOCKS_PER_PAGE, filteredBlocks.size());

        ResourceLocation currentId = ResourceLocation.tryParse(currentBlockId);

        for (int i = startIndex; i < endIndex; i++) {
            ResourceLocation id = filteredBlocks.get(i);
            Block block = BuiltInRegistries.BLOCK.get(id);

            int localIndex = i - startIndex;
            int column = localIndex % GRID_COLUMNS;
            int row = localIndex / GRID_COLUMNS;

            int cellX = gridX + column * CELL_SIZE;
            int cellY = gridY + row * CELL_SIZE;

            boolean hovered =
                    mouseX >= cellX &&
                            mouseX < cellX + CELL_SIZE &&
                            mouseY >= cellY &&
                            mouseY < cellY + CELL_SIZE;

            boolean selected = currentId != null && currentId.equals(id);

            int background = hovered ? 0xFF777777 : 0xFF333333;
            int border = selected ? 0xFFFFFF55 : 0xFF111111;

            graphics.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, border);
            graphics.fill(cellX + 1, cellY + 1, cellX + CELL_SIZE - 1, cellY + CELL_SIZE - 1, background);

            ItemStack stack = new ItemStack(block.asItem());
            graphics.renderItem(stack, cellX + 3, cellY + 3);

            if (hovered) {
                hoveredBlock = id;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;

        int gridX = panelX + (PANEL_WIDTH - GRID_WIDTH) / 2;
        int gridY = panelY + 66;

        if (button == 0 &&
                mouseX >= gridX &&
                mouseX < gridX + GRID_WIDTH &&
                mouseY >= gridY &&
                mouseY < gridY + GRID_HEIGHT) {

            int column = (int) ((mouseX - gridX) / CELL_SIZE);
            int row = (int) ((mouseY - gridY) / CELL_SIZE);

            int index = page * BLOCKS_PER_PAGE + row * GRID_COLUMNS + column;

            if (index >= 0 && index < filteredBlocks.size()) {
                ResourceLocation selected = filteredBlocks.get(index);
                onSelected.accept(selected.toString());
                Minecraft.getInstance().setScreen(parent);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) {
            page = Math.min(getMaxPage(), page + 1);
            return true;
        }

        if (scrollY > 0) {
            page = Math.max(0, page - 1);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int getMaxPage() {
        if (filteredBlocks.isEmpty()) {
            return 0;
        }

        return Math.max(0, (filteredBlocks.size() - 1) / BLOCKS_PER_PAGE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}