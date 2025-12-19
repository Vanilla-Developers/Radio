package ru.dimaskama.radio.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.item.ModItems.DataComponents;

public record RadioDestroyedProperty() /* implements <интерфейс-предикат-пункт> */ {
    public static final MapCodec<RadioDestroyedProperty> CODEC = MapCodec.unit(new RadioDestroyedProperty());

    public boolean test(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed, /*ModelTransformation.Mode*/ Object displayContext) {
        return stack.get(DataComponents.RADIO_STATE) == RadioState.DESTROYED;
    }

    public MapCodec<RadioDestroyedProperty> codec() {
        return CODEC;
    }
}
