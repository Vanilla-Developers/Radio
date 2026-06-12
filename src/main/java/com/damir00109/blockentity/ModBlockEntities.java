package com.damir00109.blockentity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.damir00109.RadioMod;
import com.damir00109.block.ModBlocks;

public final class ModBlockEntities {
	public static final BlockEntityType<RadioBlockEntity> RADIO_TYPE = FabricBlockEntityTypeBuilder.create(RadioBlockEntity::new, new Block[]{ModBlocks.RADIO})
		.build();

	public static void init() {
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, RadioMod.id("radio"), RADIO_TYPE);
	}
}
