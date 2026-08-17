package com.damir00109;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else
//import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.damir00109.block.ModBlocks;
import com.damir00109.blockentity.ModBlockEntities;
import com.damir00109.config.JsonConfig;
import com.damir00109.config.RadioConfig;
import com.damir00109.extend.ServerWorldExtend;
import com.damir00109.item.ModItems;

public class RadioMod implements ModInitializer {
    public static final String MOD_ID = "vpl";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final JsonConfig<RadioConfig> CONFIG = new JsonConfig<>("config/radio/config.json", RadioConfig.CODEC, RadioConfig::new);
    public static final ModContainer MOD_CONTAINER = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    public static final Path SOUNDS_DIR = CONFIG_DIR.resolve("sounds");
    public static final String COMMAND_PERMISSION = "radio.command";

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Vanilla+ Radio mod...");
        createDir(CONFIG_DIR);
        createDir(SOUNDS_DIR);
        CONFIG.loadOrCreate();
        ModBlocks.init();
        ModBlockEntities.init();
        ModItems.init();
        LOGGER.info("Registering radio command...");
        CommandRegistrationCallback.EVENT.register(new RadioCommand());
        LOGGER.info("Radio command registered successfully");
        //? if >=26.1 {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.isRunning()) {
                for (ServerLevel world : server.getAllLevels()) {
                    WorldRadioManager radioManager = ((ServerWorldExtend) world).radio_getRadioManager();
                    if (radioManager != null) {
                        radioManager.tick();
                    }
                }
            }
        });
        //?} else {
        /*ServerTickEvents.END_WORLD_TICK.register((ServerLevel world) -> {
            if (world.getServer().isRunning()) {
                WorldRadioManager radioManager = ((ServerWorldExtend) world).radio_getRadioManager();
                if (radioManager != null) {
                    radioManager.tick();
                }
            }
        });
        *///?}
    }

    private static void createDir(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException var2) {
            throw new RuntimeException("Failed to create directory " + path, var2);
        }
    }

    //? if >=1.21.11 {
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
    //?} else {
    /*public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    *///?}
}