package com.damir00109;

import net.fabricmc.api.ClientModInitializer;

public class vplClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockCoordTooltipComponent.register(); // Регистрация тултипа с координатами
		BlockCoordTooltipComponent.registerBrushTooltip(); // Регистрация тултипа кисти
		CompassRender.start();
	}
}