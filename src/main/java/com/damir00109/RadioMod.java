package com.damir00109;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
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
    }

    private static void createDir(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException var2) {
            throw new RuntimeException("Failed to create directory " + path, var2);
        }
    }

    public static Identifier id(String path) {
        // Создаём Identifier через рефлексию или статический метод
        try {
            // Попытка использовать Identifier.of() (может быть в более новых версиях)
            java.lang.reflect.Method ofMethod = Identifier.class.getDeclaredMethod("of", String.class, String.class);
            return (Identifier) ofMethod.invoke(null, MOD_ID, path);
        } catch (ReflectiveOperationException e1) {
            try {
                // Попытка использовать fromNamespaceAndPath
                java.lang.reflect.Method fromMethod = Identifier.class.getDeclaredMethod("fromNamespaceAndPath", String.class, String.class);
                return (Identifier) fromMethod.invoke(null, MOD_ID, path);
            } catch (ReflectiveOperationException e2) {
                // Крайняя мера: попробуем Identifier.tryParse() или другой способ
                try {
                    java.lang.reflect.Method parseMethod = Identifier.class.getDeclaredMethod("tryParse", String.class);
                    return (Identifier) parseMethod.invoke(null, MOD_ID + ":" + path);
                } catch (ReflectiveOperationException e3) {
                    throw new RuntimeException("Cannot create Identifier for " + MOD_ID + ":" + path, e3);
                }
            }
        }
    }
}