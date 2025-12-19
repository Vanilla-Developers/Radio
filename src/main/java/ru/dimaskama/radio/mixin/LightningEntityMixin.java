package ru.dimaskama.radio.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1538;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_3218;
import net.minecraft.class_3481;
import net.minecraft.class_2338.class_2339;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.radio.block.ModBlocks;
import ru.dimaskama.radio.block.RadioBlock;

@Mixin({class_1538.class})
abstract class LightningEntityMixin extends class_1297 {
	private LightningEntityMixin(class_1299<?> type, class_1937 world) {
		super(type, world);
		throw new AssertionError();
	}

	@Inject(
		method = {"powerLightningRod"},
		at = {@At("TAIL")}
	)
	private void destroyRadio(CallbackInfo ci, @Local class_2338 pos, @Local class_2680 state) {
		if (this.method_73183() instanceof class_3218 world) {
			class_2339 mutable = pos.method_25503();

			while (state.method_26164(class_3481.field_61207) && state.method_61767(class_2741.field_12525, class_2350.field_11036) == class_2350.field_11036) {
				mutable.method_33098(mutable.method_10264() - 1);
				state = world.method_8320(mutable);
			}

			if (state.method_27852(ModBlocks.RADIO)) {
				RadioBlock.burnRadio(world, mutable);
			}
		}
	}
}
