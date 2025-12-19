package ru.dimaskama.radio.blockentity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.class_2248;
import net.minecraft.class_2378;
import net.minecraft.class_2591;
import net.minecraft.class_7923;
import ru.dimaskama.radio.RadioMod;
import ru.dimaskama.radio.block.ModBlocks;

public final class ModBlockEntities {
	public static final class_2591<RadioBlockEntity> RADIO_TYPE = FabricBlockEntityTypeBuilder.create(RadioBlockEntity::new, new class_2248[]{ModBlocks.RADIO})
		.build();

	public static void init() {
		class_2378.method_10230(class_7923.field_41181, RadioMod.id("radio"), RADIO_TYPE);
	}
}
