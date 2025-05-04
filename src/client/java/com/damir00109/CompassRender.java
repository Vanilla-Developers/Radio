package com.damir00109;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.ChunkBuilderMode;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Класс, отвечающий за рендер координат игрока и переключение опций при держании компаса.
 */
public class CompassRender {
    private static boolean initialized = false;

    public static void start() {
        if (initialized) return;
        initialized = true;

        // Регистрируем обработчик тика клиента для отображения координат
        ClientTickEvents.END_CLIENT_TICK.register(CompassRender::onEndClientTick);
    }

    private static void onEndClientTick(MinecraftClient client) {
        if (client.player == null) return;

        boolean holdingCompass = client.player.getMainHandStack().getItem() == Items.COMPASS;
        GameOptions opts = client.options;

        // Включаем или отключаем Reduced Debug Info
        opts.getReducedDebugInfo().setValue(!holdingCompass);

        // Переключаем отображение границ чанков: NONE ↔ NEARBY
        opts.getChunkBuilderMode().setValue(
                holdingCompass
                        ? ChunkBuilderMode.NEARBY
                        : ChunkBuilderMode.NONE
        );

        // Отправляем обновлённые настройки на сервер
        opts.sendClientSettings();

        // Если держим компас, показываем координаты в Action Bar
        if (holdingCompass) {
            int x = client.player.getBlockPos().getX();
            int y = client.player.getBlockPos().getY();
            int z = client.player.getBlockPos().getZ();
            Text coordText = Text.literal("X: " + x + " Y: " + y + " Z: " + z);
            client.inGameHud.setOverlayMessage(coordText, false);
        }
    }
}
