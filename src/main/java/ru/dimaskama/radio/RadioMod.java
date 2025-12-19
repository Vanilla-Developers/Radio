package ru.dimaskama.radio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndWorldTick;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.class_2960;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dimaskama.radio.block.ModBlocks;
import ru.dimaskama.radio.blockentity.ModBlockEntities;
import ru.dimaskama.radio.config.JsonConfig;
import ru.dimaskama.radio.config.RadioConfig;
import ru.dimaskama.radio.extend.ServerWorldExtend;
import ru.dimaskama.radio.item.ModItems;

public class RadioMod implements ModInitializer {
	public static final String MOD_ID = "radio";
	public static final Logger LOGGER = LoggerFactory.getLogger("radio");
	public static final JsonConfig<RadioConfig> CONFIG = new JsonConfig<>("config/radio/config.json", RadioConfig.CODEC, RadioConfig::new);
	public static final ModContainer MOD_CONTAINER = (ModContainer)FabricLoader.getInstance().getModContainer("radio").orElseThrow();
	public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("radio");
	public static final Path SOUNDS_DIR = CONFIG_DIR.resolve("sounds");
	public static final String COMMAND_PERMISSION = "radio.command";

	public void onInitialize() {
		createDir(CONFIG_DIR);
		createDir(CONFIG_DIR.resolve("sounds"));
		CONFIG.loadOrCreate();
		ModBlocks.init();
		ModBlockEntities.init();
		ModItems.init();
		CommandRegistrationCallback.EVENT.register(new RadioCommand());
		ServerTickEvents.END_WORLD_TICK.register((EndWorldTick)world -> {
			if (world.method_54719().method_54751()) {
				WorldRadioManager radioManager = ((ServerWorldExtend)world).radio_getRadioManager();
				if (radioManager != null) {
					radioManager.tick();
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

	public static class_2960 id(String path) {
		return class_2960.method_60655("radio", path);
	}
}
