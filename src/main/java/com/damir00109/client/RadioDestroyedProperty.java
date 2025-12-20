package com.damir00109.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.render.item.property.bool.BooleanProperty;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import com.damir00109.RadioState;
import com.damir00109.item.ModItems.DataComponents;

public record RadioDestroyedProperty() implements BooleanProperty {
    public static final MapCodec<RadioDestroyedProperty> CODEC = MapCodec.unit(new RadioDestroyedProperty());

    public MapCodec<RadioDestroyedProperty> getCodec() {
        return CODEC;
    }

    @Override
    public boolean test(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed, ItemDisplayContext displayContext) {
        return stack.get(DataComponents.RADIO_STATE) == RadioState.DESTROYED;
    }
}