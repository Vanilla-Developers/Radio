package com.damir00109;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaDamir00109.MOD_ID + " Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), VanillaDamir00109.MOD_ID + ".json");

    public List<String> allowedDimensions = new ArrayList<>(List.of("minecraft:overworld"));
    public boolean enableBorderSystem = false; // Default value changed to false

    // Настройки границы мира
    public double borderMinX = -1000.0;
    public double borderMaxX = 1000.0;
    public double borderMinZ = -1000.0;
    public double borderMaxZ = 1000.0;
    public int borderWarningTime = 15; // Default warning time in seconds
    public int borderWarningBlocks = 5; // Default warning distance in blocks
    public double borderDamageBuffer = 5.0; // Default damage buffer
    public double borderDamagePerBlock = 0.2; // Default damage per block

    public static ModConfig load() {
        ModConfig config = new ModConfig(); // This will now have enableBorderSystem = false by default
        try {
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    config = GSON.fromJson(reader, ModConfig.class);
                    if (config == null) { // If config is malformed or empty
                        config = new ModConfig();
                        LOGGER.warn("Config file was malformed or empty, loaded defaults.");
                    } else {
                        // Ensure default fields are present if old config is loaded
                        if (config.allowedDimensions == null) {
                            config.allowedDimensions = new ArrayList<>(List.of("minecraft:overworld"));
                        }
                        // No need to explicitly set enableBorderSystem to false here,
                        // as it's already the default for a new ModConfig instance.
                        // If the field is missing in an old config, GSON might leave it as the default (false).
                        // If it's present in an old config, its value will be loaded.
                    }
                }
            } else {
                LOGGER.info("Config file not found. Creating new one with default values (enableBorderSystem=false).");
                save(config); 
            }
        } catch (IOException e) {
            LOGGER.error("Could not load or create config file for " + VanillaDamir00109.MOD_ID, e);
            // Fallback to default config if loading fails catastrophically
            config = new ModConfig();
        }
        // Save the config again to ensure any new fields are written if an old config was loaded
        // or if the file was just created. This ensures consistency.
        save(config); 
        return config;
    }

    public static void save(ModConfig config) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            LOGGER.error("Could not save config file for " + VanillaDamir00109.MOD_ID, e);
        }
    }
} 