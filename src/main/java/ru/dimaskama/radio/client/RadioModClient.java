package ru.dimaskama.radio.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import ru.dimaskama.radio.RadioMod;

public class RadioModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Для Minecraft 1.21.9 реестр типов свойств называется BLOCK_STATE_PROPERTY_TYPE
        Registry.register(
                Registries.BLOCK_TYPE,
                RadioMod.id("radio_destroyed"),
                RadioDestroyedProperty.CODEC
        );
    }
}