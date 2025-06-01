package com.damir00109.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class vpl implements ClientModInitializer {

    public static final String MOD_ID = "vpl"; // Используем тот же MOD_ID для консистентности логов
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {} client components...", MOD_ID);
        // Сюда вы можете добавить код, который должен выполняться только на клиенте
        // Например, регистрация рендереров, экранов, кейбиндингов и т.д.
        LOGGER.info("{} client components initialized.", MOD_ID);
    }
} 