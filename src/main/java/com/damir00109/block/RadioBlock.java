package com.damir00109.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import com.damir00109.RadioState;
import com.damir00109.blockentity.ModBlockEntities;
import com.damir00109.blockentity.RadioBlockEntity;
import com.damir00109.item.ModItems;
import com.damir00109.item.ModItems.DataComponents;

public class RadioBlock extends BaseEntityBlock {

    public static final MapCodec<RadioBlock> CODEC = simpleCodec(RadioBlock::new);

    public RadioBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(ModBlocks.Properties.RADIO_STATE, RadioState.DISABLED)
                        .setValue(ModBlocks.Properties.LEFT_INDICATOR, false)
                        .setValue(BlockStateProperties.POWER, 0)
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
        );
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type
    ) {
        if (world.isClientSide()) return null;
        if (type != ModBlockEntities.RADIO_TYPE) return null;

        return (world1, pos, state1, be) ->
                ((RadioBlockEntity) be).tick(world1, pos, state1);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        if (!oldState.is(state.getBlock()) && world instanceof ServerLevel serverWorld) {
            update(pos, state, serverWorld);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        if (world instanceof ServerLevel serverWorld) {
            update(pos, state, serverWorld);
        }
    }

    private void update(BlockPos pos, BlockState state, ServerLevel world) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity blockEntity) {
            BlockState newState = blockEntity.updateState(pos, state, world, false);
            if (newState != null) {
                world.setBlock(pos, newState, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hit) {
        return tryToggle(state, world, pos);
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel world, BlockPos pos,
                              net.minecraft.world.level.Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
        if (explosion.canTriggerBlocks()) {
            tryToggle(state, world, pos);
        }
        super.onExplosionHit(state, world, pos, explosion, stackMerger);
    }

    protected InteractionResult tryToggle(BlockState state, Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverWorld
                && world.getBlockEntity(pos) instanceof RadioBlockEntity blockEntity) {
            return blockEntity.tryToggle(serverWorld, pos, state);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                         Player player, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem
                && new BlockPlaceContext(player, hand, stack, hit).canPlace()) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            return radio.getComparatorOutput();
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(ModBlocks.Properties.RADIO_STATE,
                        ctx.getItemInHand().getOrDefault(DataComponents.RADIO_STATE, RadioState.DISABLED));
    }

    // Убрали @Override
    protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(world, pos, state, includeData);

        RadioState radioState = state.getValue(ModBlocks.Properties.RADIO_STATE);
        stack.set(DataComponents.RADIO_STATE,
                radioState == RadioState.DESTROYED ? RadioState.DESTROYED : RadioState.DISABLED);

        return stack;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        RadioState radioState = state.getValue(ModBlocks.Properties.RADIO_STATE);
        List<ItemStack> list = super.getDrops(state, builder);

        for (ItemStack stack : list) {
            if (stack.is(ModItems.RADIO)) {
                stack.set(DataComponents.RADIO_STATE,
                        radioState == RadioState.DESTROYED ? RadioState.DESTROYED : RadioState.DISABLED);
            }
        }
        return list;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                ModBlocks.Properties.RADIO_STATE,
                ModBlocks.Properties.LEFT_INDICATOR,
                BlockStateProperties.POWER,
                BlockStateProperties.HORIZONTAL_FACING
        );
    }

    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            BlockState newState = radio.newBurnedState(world, pos, state);
            if (newState != null) {
                world.setBlock(pos, newState, Block.UPDATE_ALL);
                world.playSound(
                        null,
                        pos,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.5F,
                        0.7F
                );
            }
        }
    }

    public static void burnRadio(ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            BlockState newState = radio.newBurnedState(world, pos, world.getBlockState(pos));
            if (newState == null) {
                newState = world.getBlockState(pos).setValue(ModBlocks.Properties.RADIO_STATE, RadioState.DESTROYED);
            }
            world.setBlock(pos, newState, Block.UPDATE_ALL);
            world.playSound(
                    null,
                    pos,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.5F,
                    0.7F
            );
        }
    }
}
