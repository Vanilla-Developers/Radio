package com.damir00109.block;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import com.damir00109.RadioMod;
import com.damir00109.RadioState;

public final class ModBlocks {
	private static final List<Pair<ResourceLocation, Block>> BLOCKS_TO_REGISTER = new ArrayList<>();
	public static final RadioBlock RADIO = registerOnInit(
		"radio",
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.strength(3.0f, 6.0f)
					.requiresCorrectToolForDrops(),
		RadioBlock::new
	);

	public static void init() {
		ModBlocks.Properties.init();
		BLOCKS_TO_REGISTER.forEach(pair -> Registry.register(BuiltInRegistries.BLOCK, pair.getFirst(), pair.getSecond()));
		BLOCKS_TO_REGISTER.clear();
	}

	private static <T extends Block> T registerOnInit(String radioModId, BlockBehaviour.Properties settings, Function<BlockBehaviour.Properties, T> factory) {
		ResourceLocation id = RadioMod.id(radioModId);
		// Fabric требует, чтобы у блока был установлен registry key до создания экземпляра
		settings.setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), id));
		T block = factory.apply(settings);
		BLOCKS_TO_REGISTER.add(new Pair<>(id, block));
		return block;
	}

	public static final class Properties {
		public static final EnumProperty<RadioState> RADIO_STATE = EnumProperty.create("radio_state", RadioState.class);
		public static final BooleanProperty LEFT_INDICATOR = BooleanProperty.create("left_indicator");

		private static void init() {
		}
	}
}
