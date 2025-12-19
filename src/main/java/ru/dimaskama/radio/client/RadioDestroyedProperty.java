package ru.dimaskama.radio.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.class_10460;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_638;
import net.minecraft.class_811;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.item.ModItems.DataComponents;

public record RadioDestroyedProperty() implements class_10460 {
	public static final MapCodec<RadioDestroyedProperty> CODEC = MapCodec.unit(new RadioDestroyedProperty());

	public boolean method_65638(class_1799 stack, @Nullable class_638 world, @Nullable class_1309 entity, int seed, class_811 displayContext) {
		return stack.method_58694(DataComponents.RADIO_STATE) == RadioState.DESTROYED;
	}

	public MapCodec<RadioDestroyedProperty> method_65637() {
		return CODEC;
	}
}
