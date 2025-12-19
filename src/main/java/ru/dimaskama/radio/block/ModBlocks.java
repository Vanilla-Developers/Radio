package ru.dimaskama.radio.block;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.BooleanProperty;
import ru.dimaskama.radio.RadioMod;
import ru.dimaskama.radio.RadioState;

public final class ModBlocks {
	private static final List<Pair<Identifier, Block>> BLOCKS_TO_REGISTER = new ArrayList<>();
	public static final RadioBlock RADIO = registerOnInit(
		"radio",
		AbstractBlock.Settings.of(Material.METAL).strength(1.5F, 3.5F).sounds(BlockSoundGroup.METAL),
		RadioBlock::new
	);

	public static void init() {
		ModBlocks.Properties.init();
		BLOCKS_TO_REGISTER.forEach(pair -> Registry.register(Registries.BLOCK, pair.getFirst(), pair.getSecond()));
		BLOCKS_TO_REGISTER.clear();
	}

	private static <T extends Block> T registerOnInit(String radioModId, AbstractBlock.Settings settings, Function<AbstractBlock.Settings, T> factory) {
		Identifier id = RadioMod.id(radioModId);
		// Если требуется автоматически назначать креативную вкладку для BlockItem,
		// это обычно делается при создании BlockItem (например new BlockItem(block, new Item.Settings().group(...))).
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
