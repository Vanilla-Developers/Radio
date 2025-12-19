package ru.dimaskama.radio.block;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2378;
import net.minecraft.class_2746;
import net.minecraft.class_2754;
import net.minecraft.class_2960;
import net.minecraft.class_3620;
import net.minecraft.class_5321;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import net.minecraft.class_4970.class_2251;
import ru.dimaskama.radio.RadioMod;
import ru.dimaskama.radio.RadioState;

public final class ModBlocks {
	private static final List<Pair<class_2960, class_2248>> BLOCKS_TO_REGISTER = new ArrayList();
	public static final RadioBlock RADIO = registerOnInit(
		"radio", class_2251.method_9637().method_31710(class_3620.field_16023).method_9629(1.5F, 3.5F).method_26236(class_2246::method_26122), RadioBlock::new
	);

	public static void init() {
		ModBlocks.Properties.init();
		BLOCKS_TO_REGISTER.forEach(pair -> class_2378.method_10230(class_7923.field_41175, (class_2960)pair.getFirst(), (class_2248)pair.getSecond()));
		BLOCKS_TO_REGISTER.clear();
	}

	private static <T extends class_2248> T registerOnInit(String radioModId, class_2251 settings, Function<class_2251, T> factory) {
		class_2960 id = RadioMod.id(radioModId);
		settings.method_63500(class_5321.method_29179(class_7924.field_41254, id));
		T block = (T)factory.apply(settings);
		BLOCKS_TO_REGISTER.add(new Pair(id, block));
		return block;
	}

	public static final class Properties {
		public static final class_2754<RadioState> RADIO_STATE = class_2754.method_11850("radio_state", RadioState.class);
		public static final class_2746 LEFT_INDICATOR = class_2746.method_11825("left_indicator");

		private static void init() {
		}
	}
}
