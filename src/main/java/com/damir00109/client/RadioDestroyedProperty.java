package com.damir00109.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import com.damir00109.RadioState;
import com.damir00109.item.ModItems.DataComponents;

public record RadioDestroyedProperty() implements ConditionalItemModelProperty {
    public static final MapCodec<RadioDestroyedProperty> CODEC = MapCodec.unit(new RadioDestroyedProperty());

    public MapCodec<RadioDestroyedProperty> type() {
        return CODEC;
    }

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed, ItemDisplayContext displayContext) {
        return stack.get(DataComponents.RADIO_STATE) == RadioState.DESTROYED;
    }
}