package ru.dimaskama.radio.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.item.property.bool.BooleanProperties;
import ru.dimaskama.radio.RadioMod;

public class RadioModClient implements ClientModInitializer {
    public void onInitializeClient() {
        BooleanProperties.ID_MAPPER.put(RadioMod.id("radio_destroyed"), RadioDestroyedProperty.CODEC);
    }
}