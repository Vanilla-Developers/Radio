package com.damir00109;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.block.Block;

// Импортируем именно DModItems.BlockIconTooltipData
import com.damir00109.DModItems.BlockIconTooltipData;

public class VanillaDamir00109Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Рендерим иконку блока
		TooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof BlockIconTooltipData d) {
				return new BlockIconTooltipComponent(d.block());
			}
			return null;
		});

		// Добавляем текст для сущностей (опционально)
		ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> {
			if (!stack.isOf(DModItems.GLOWING_BRUSH)) return;
			var bdata = stack.get(DModItems.GLOWING_BRUSH_DATA);
			if (bdata != null && !bdata.lastType().isEmpty()) {
				lines.add(Text.literal("Entity: " + bdata.lastType())
						.formatted(Formatting.GRAY));
			}
		});
	}

	public static class BlockIconTooltipComponent implements TooltipComponent {
		private final ItemStack iconStack;
		public BlockIconTooltipComponent(Block block) {
			this.iconStack = new ItemStack(block.asItem());
		}
		@Override public int getHeight(TextRenderer textRenderer) { return 20; }
		@Override public int getWidth(TextRenderer textRenderer)  { return 20; }
		@Override
		public void drawItems(TextRenderer textRenderer,
							  int x, int y,
							  int width, int height,
							  DrawContext context) {
			context.drawItem(iconStack, x, y);
		}
	}
}
