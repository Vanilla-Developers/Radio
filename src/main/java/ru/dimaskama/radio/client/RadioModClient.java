package ru.dimaskama.radio.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;

import ru.dimaskama.radio.RadioMod;

public class RadioModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // TODO: Замените PLACEHOLDER_REGISTRY на точный реестр (например, BuiltinRegistries.SOME_REGISTRY или Registries.SOME_KEY)
        // Вариант A: если это встроенный реестр (builtin):
        BuiltinRegistries.register(BuiltinRegistries.PLACEHOLDER_REGISTRY, RadioMod.id("radio_destroyed"), RadioDestroyedProperty.CODEC);

        // Вариант B: если нужно использовать общий Registry API:
        // Registry.register(Registries.PLACEHOLDER_REGISTRY, RadioMod.id("radio_destroyed"), RadioDestroyedProperty.CODEC);
    }
}
