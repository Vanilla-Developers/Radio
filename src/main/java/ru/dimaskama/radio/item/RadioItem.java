package ru.dimaskama.radio.item;

import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_2248;
import net.minecraft.class_2561;
import net.minecraft.class_1792.class_1793;
import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.item.ModItems.DataComponents;

public class RadioItem extends class_1747 {
	private static final class_2561 DESTROYED_TEXT = class_2561.method_43471("block.radio.radio.destroyed");

	public RadioItem(class_2248 block, class_1793 settings) {
		super(block, settings);
	}

	public class_2561 method_7864(class_1799 stack) {
		return stack.method_58694(DataComponents.RADIO_STATE) != RadioState.DESTROYED ? super.method_7864(stack) : DESTROYED_TEXT;
	}
}
