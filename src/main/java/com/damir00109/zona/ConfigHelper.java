package com.damir00109.zona;

import com.damir00109.VanillaDamir00109;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ConfigHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaDamir00109.SVSP_MOD_ID + "_ConfigHelper");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir().resolve(VanillaDamir00109.SVSP_MOD_ID + "_ZonaConfig.json");

    public static ZonaConfig loadConfig() {
        if (Files.exists(CONFIG_FILE_PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(CONFIG_FILE_PATH, StandardCharsets.UTF_8)) {
                ZonaConfig config = GSON.fromJson(reader, ZonaConfig.class);
                if (config != null) {
                    LOGGER.info("Successfully loaded ZonaConfig from {}", CONFIG_FILE_PATH.toString());
                    return config;
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                LOGGER.error("Failed to load or parse ZonaConfig from " + CONFIG_FILE_PATH.toString(), e);
            }
        }
        LOGGER.info("ZonaConfig not found or failed to load, creating default configuration.");
        ZonaConfig defaultConfig = new ZonaConfig();
        saveConfig(defaultConfig);
        return defaultConfig;
    }

    public static void saveConfig(ZonaConfig config) {
        // Создаем JSON вручную, чтобы добавить комментарии
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  // Включена ли основная логика зоны (границы, эффекты и т.д.)\n");
        sb.append("  \"zonaEnabled\": ").append(config.zonaEnabled).append(",\n\n");

        sb.append("  // Определение точек для обычной границы мира.\n");
        sb.append("  // Каждая точка имеет координаты X и Z.\n");
        sb.append("  // Граница должна быть замкнутым многоугольником (обычно прямоугольником).\n");
        sb.append("  \"borderNormalPoints\": ").append(GSON.toJson(config.borderNormalPoints)).append(",\n\n");

        sb.append("  // Определение точек для уменьшенной границы мира (например, при низком сознании игрока).\n");
        sb.append("  \"borderReducedPoints\": ").append(GSON.toJson(config.borderReducedPoints)).append(",\n\n");

        sb.append("  // Определение точек для аварийной границы мира (самая дальняя, за которой мгновенная смерть).\n");
        sb.append("  \"emergencyBorderPoints\": ").append(GSON.toJson(config.emergencyBorderPoints)).append("\n");
        // Примечание: если будут добавляться новые поля, нужно следить за последней запятой.
        // Здесь emergencyBorderPoints - последнее поле, поэтому после него нет запятой.

        sb.append("}\n");

        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE_PATH, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(sb.toString());
            LOGGER.info("Successfully saved ZonaConfig with comments to {}", CONFIG_FILE_PATH.toString());
        } catch (IOException e) {
            LOGGER.error("Failed to save ZonaConfig with comments to " + CONFIG_FILE_PATH.toString(), e);
        }
    }
} 