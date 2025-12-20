package com.damir00109.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.util.math.BlockPos.Mutable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.damir00109.block.ModBlocks;
import com.damir00109.block.RadioBlock;

@Mixin(LightningEntity.class)
abstract class LightningEntityMixin extends Entity {
    private World world;

    private LightningEntityMixin(EntityType<?> type, World world) {
        super(type, world);
        this.world = world;
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
        if (this.world instanceof ServerWorld world) {

            Mutable mutable = pos.mutableCopy();

            while (
                    state.isOf(Blocks.LIGHTNING_ROD)
                            && state.get(LightningRodBlock.FACING) == Direction.UP
            ) {
                mutable.move(Direction.DOWN);
                state = world.getBlockState(mutable);
            }

            if (state.isOf(ModBlocks.RADIO)) {
                RadioBlock.burnRadio(world, mutable);
            }
        }
    }
}
