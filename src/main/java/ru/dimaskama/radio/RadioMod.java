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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
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
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final JsonConfig<RadioConfig> CONFIG = new JsonConfig<>("config/radio/config.json", RadioConfig.CODEC, RadioConfig::new);
	public static final ModContainer MOD_CONTAINER = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
	public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
	public static final Path SOUNDS_DIR = CONFIG_DIR.resolve("sounds");
	public static final String COMMAND_PERMISSION = "radio.command";

	@Override
	public void onInitialize() {
		createDir(CONFIG_DIR);
		createDir(SOUNDS_DIR);
		CONFIG.loadOrCreate();
		ModBlocks.init();
		ModBlockEntities.init();
		ModItems.init();
		CommandRegistrationCallback.EVENT.register(new RadioCommand());
		ServerTickEvents.END_WORLD_TICK.register((ServerWorld world) -> {
			if (world.getServer().isRunning()) {
				WorldRadioManager radioManager = ((ServerWorldExtend) world).radio_getRadioManager();
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

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
