package ru.dimaskama.radio.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.class_10459;
import ru.dimaskama.radio.RadioMod;

public class RadioModClient implements ClientModInitializer {
	public void onInitializeClient() {
		class_10459.field_55372.method_65325(RadioMod.id("radio_destroyed"), RadioDestroyedProperty.CODEC);
	}
}
