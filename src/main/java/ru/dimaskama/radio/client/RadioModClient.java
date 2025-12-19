package ru.dimaskama.radio.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import ru.dimaskama.radio.RadioMod;

public class RadioModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Для регистрации PropertyType используется Registries.PROPERTY_TYPE
        Registry.register(
                Registries.PROPERTY_TYPE,
                RadioMod.id("radio_destroyed"),
                RadioDestroyedProperty.CODEC
        );
    }
}