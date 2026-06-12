package com.damir00109.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.damir00109.block.ModBlocks;
import com.damir00109.block.RadioBlock;

@Mixin(LightningBolt.class)
abstract class LightningEntityMixin extends Entity {
    private LightningEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
        throw new AssertionError();
    }

    @Inject(
            method = "powerLightningRod",
            at = @At("TAIL")
    )
    private void destroyRadio(
            CallbackInfo ci,
            @Local BlockPos pos,
            @Local BlockState state
    ) {
        if (this.level() instanceof ServerLevel world) {

            MutableBlockPos mutable = pos.mutable();

            while (
                    state.getBlock() instanceof LightningRodBlock
                            && state.getValue(LightningRodBlock.FACING) == Direction.UP
            ) {
                mutable.move(Direction.DOWN);
                state = world.getBlockState(mutable);
            }

            if (state.is(ModBlocks.RADIO)) {
                RadioBlock.burnRadio(world, mutable);
            }
        }
    }
}
