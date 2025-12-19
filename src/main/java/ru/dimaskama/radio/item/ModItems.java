package ru.dimaskama.radio.item;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_7706;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import net.minecraft.class_9331;
import net.minecraft.class_1792.class_1793;
import ru.dimaskama.radio.RadioMod;
import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.block.ModBlocks;

public final class ModItems {
	private static final List<Pair<class_2960, class_1792>> ITEMS_TO_REGISTER = new ArrayList();
	public static final RadioItem RADIO = registerOnInit(
		"radio",
		new class_1793().method_7889(64).method_57349(ModItems.DataComponents.RADIO_STATE, RadioState.DISABLED).method_63685(),
		settings -> new RadioItem(ModBlocks.RADIO, settings)
	);

	public static void init() {
		ModItems.DataComponents.init();
		ITEMS_TO_REGISTER.forEach(pair -> class_2378.method_10230(class_7923.field_41178, (class_2960)pair.getFirst(), (class_1792)pair.getSecond()));
		ITEMS_TO_REGISTER.clear();
		ItemGroupEvents.modifyEntriesEvent(class_7706.field_40198)
			.register((ModifyEntries)entries -> entries.addAfter(class_1802.field_46791, new class_1935[]{RADIO}));
	}

	private static <T extends class_1792> T registerOnInit(String radioModId, class_1793 settings, Function<class_1793, T> factory) {
		class_2960 id = RadioMod.id(radioModId);
		settings.method_63686(class_5321.method_29179(class_7924.field_41197, id));
		T item = (T)factory.apply(settings);
		ITEMS_TO_REGISTER.add(new Pair(id, item));
		return item;
	}

	public static final class DataComponents {
		public static final class_9331<RadioState> RADIO_STATE = class_9331.method_57873()
			.method_57881(RadioState.CODEC)
			.method_57882(RadioState.PACKET_CODEC)
			.method_57880();

		private static void init() {
			class_2378.method_10230(class_7923.field_49658, RadioMod.id("radio_state"), RADIO_STATE);
		}
	}
}
