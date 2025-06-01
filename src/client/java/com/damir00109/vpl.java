// Файл 1: VanillaDamir00109Client.java
package com.damir00109;

import net.fabricmc.api.ClientModInitializer;

public class vpl implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockCoordTooltipComponent.register(); // Регистрация тултипа с координатами
		BlockCoordTooltipComponent.registerBrushTooltip(); // Регистрация тултипа кисти
		CompassRender.start();
	}
}