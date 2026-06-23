package com.damir00109.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import com.damir00109.RadioMod;

public class RadioModClient implements ClientModInitializer {
    public void onInitializeClient() {
        ConditionalItemModelProperties.ID_MAPPER.put(RadioMod.id("radio_destroyed"), RadioDestroyedProperty.CODEC);
    }
}