// Файл 2: BlockCoordTooltipComponent.java
package com.damir00109;

import com.damir00109.items.DModItems;
import com.damir00109.items.DModItems.BlockCoordTooltipData;
import net.minecraft.block.Block;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class BlockCoordTooltipComponent implements TooltipComponent {
    // Координаты блока
    private final ItemStack iconStack;
    private final String coordsText;

    // Константы отрисовки
    private static final int ORIGINAL_ICON_SIZE = 16;
    private static final float SCALE = 0.7f;
    private static final int ICON_SIZE = (int)(ORIGINAL_ICON_SIZE * SCALE);
    private static final int H_PAD_TOP = 2;
    private static final int H_PAD_BOTTOM = 2;
    private static final int H_PAD_SIDES = 2;
    private static final int TEXT_GAP = 2;

    public BlockCoordTooltipComponent(Block block, int x, int y, int z) {
        this.iconStack = new ItemStack(block.asItem());
        this.coordsText = x + " " + y + " " + z;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return H_PAD_TOP + ICON_SIZE + H_PAD_BOTTOM;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return H_PAD_SIDES + ICON_SIZE + TEXT_GAP + textRenderer.getWidth(coordsText) + H_PAD_SIDES;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        int innerY = y + H_PAD_TOP;

        context.getMatrices().push();
        context.getMatrices().translate(x + H_PAD_SIDES, innerY, 0);
        context.getMatrices().scale(SCALE, SCALE, SCALE);
        context.drawItem(iconStack, 0, 0);
        context.getMatrices().pop();

        int textX = x + H_PAD_SIDES + ICON_SIZE + TEXT_GAP;
        int textY = innerY + (ICON_SIZE - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, coordsText, textX, textY, 0xAAAAAA, false);
    }

    // Регистрация компонента с координатами
    public static void register() {
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof BlockCoordTooltipData d) {
                return new BlockCoordTooltipComponent(d.block(), d.x(), d.y(), d.z());
            }
            return null;
        });
    }

    // Регистрация тултипа для кисти
    public static void registerBrushTooltip() {
        ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> {
            if (!stack.isOf(DModItems.GLOWING_BRUSH)) return;
            var bdata = stack.get(DModItems.GLOWING_BRUSH_DATA);
            if (bdata != null && !bdata.lastType().isEmpty()) {
                lines.add(Text.literal("Entity: " + bdata.lastType())
                        .formatted(Formatting.GRAY));
            }
        });
    }
}