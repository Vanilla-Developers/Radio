package ru.dimaskama.radio.blockentity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import ru.dimaskama.radio.RadioMod;
import ru.dimaskama.radio.block.ModBlocks;

public final class ModBlockEntities {
	public static final BlockEntityType<RadioBlockEntity> RADIO_TYPE = FabricBlockEntityTypeBuilder.create(RadioBlockEntity::new, new Block[]{ModBlocks.RADIO})
		.build();

	public static void init() {
		Registry.register(Registries.BLOCK_ENTITY_TYPE, RadioMod.id("radio"), RADIO_TYPE);
	}
}
