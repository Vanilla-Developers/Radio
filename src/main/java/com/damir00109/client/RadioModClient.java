package com.damir00109.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.item.property.bool.BooleanProperties;
import com.damir00109.RadioMod;

public class RadioModClient implements ClientModInitializer {
    public void onInitializeClient() {
        BooleanProperties.ID_MAPPER.put(RadioMod.id("radio_destroyed"), RadioDestroyedProperty.CODEC);
    }
}