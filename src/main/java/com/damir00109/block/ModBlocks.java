package com.damir00109.block;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.BooleanProperty;
import com.damir00109.RadioMod;
import com.damir00109.RadioState;

public final class ModBlocks {
	private static final List<Pair<Identifier, Block>> BLOCKS_TO_REGISTER = new ArrayList<>();
	public static final RadioBlock RADIO = registerOnInit(
		"radio",
			AbstractBlock.Settings.create()
					.mapColor(MapColor.IRON_GRAY)
					.strength(3.0f, 6.0f)
					.requiresTool(),
		RadioBlock::new
	);

	public static void init() {
		ModBlocks.Properties.init();
		BLOCKS_TO_REGISTER.forEach(pair -> Registry.register(Registries.BLOCK, pair.getFirst(), pair.getSecond()));
		BLOCKS_TO_REGISTER.clear();
	}

	private static <T extends Block> T registerOnInit(String radioModId, AbstractBlock.Settings settings, Function<AbstractBlock.Settings, T> factory) {
		Identifier id = RadioMod.id(radioModId);
		// Fabric требует, чтобы у блока был установлен registry key до создания экземпляра
		settings.registryKey(RegistryKey.of(Registries.BLOCK.getKey(), id));
		T block = factory.apply(settings);
		BLOCKS_TO_REGISTER.add(new Pair<>(id, block));
		return block;
	}

	public static final class Properties {
		public static final EnumProperty<RadioState> RADIO_STATE = EnumProperty.of("radio_state", RadioState.class);
		public static final BooleanProperty LEFT_INDICATOR = BooleanProperty.of("left_indicator");

		private static void init() {
		}
	}
}
