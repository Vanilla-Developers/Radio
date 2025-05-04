// Файл 1: VanillaDamir00109Client.java
package com.damir00109;

import net.fabricmc.api.ClientModInitializer;

public class VanillaDamir00109Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockCoordTooltipComponent.register(); // Регистрация тултипа с координатами
		BlockCoordTooltipComponent.registerBrushTooltip(); // Регистрация тултипа кисти
	}
}