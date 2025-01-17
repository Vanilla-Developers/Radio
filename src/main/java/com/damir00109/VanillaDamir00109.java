package com.damir00109;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaDamir00109 implements ModInitializer {
	public static final String MOD_ID = "vpl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		DModItems.registerModItems();   // Регистрация предметов
		DModBlocks.registerModBlocks(); // Регистрация блоков
	}
}
