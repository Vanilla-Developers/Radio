package com.damir00109.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.world.World;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.loot.context.LootWorldContext;
import org.jetbrains.annotations.Nullable;

import com.damir00109.RadioState;
import com.damir00109.blockentity.ModBlockEntities;
import com.damir00109.blockentity.RadioBlockEntity;
import com.damir00109.item.ModItems;
import com.damir00109.item.ModItems.DataComponents;

public class RadioBlock extends BlockWithEntity {

    public static final MapCodec<RadioBlock> CODEC = createCodec(RadioBlock::new);

    public RadioBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ModBlocks.Properties.RADIO_STATE, RadioState.DISABLED)
                        .with(ModBlocks.Properties.LEFT_INDICATOR, false)
                        .with(Properties.POWER, 0)
                        .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
        );
    }

    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RadioBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type
    ) {
        if (world.isClient()) return null;
        if (type != ModBlockEntities.RADIO_TYPE) return null;

        return (world1, pos, state1, be) ->
                ((RadioBlockEntity) be).tick(world1, pos, state1);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!oldState.isOf(state.getBlock()) && world instanceof ServerWorld serverWorld) {
            update(pos, state, serverWorld);
        }
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (world instanceof ServerWorld serverWorld) {
            update(pos, state, serverWorld);
        }
    }

    private void update(BlockPos pos, BlockState state, ServerWorld world) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity blockEntity) {
            BlockState newState = blockEntity.updateState(pos, state, world, false);
            if (newState != null) {
                world.setBlockState(pos, newState, Block.NOTIFY_ALL);
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, net.minecraft.util.hit.BlockHitResult hit) {
        return tryToggle(state, world, pos);
    }

    @Override
    protected void onExploded(BlockState state, ServerWorld world, BlockPos pos,
                              net.minecraft.world.explosion.Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
        if (explosion.canTriggerBlocks()) {
            tryToggle(state, world, pos);
        }
        super.onExploded(state, world, pos, explosion, stackMerger);
    }

    protected ActionResult tryToggle(BlockState state, World world, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld
                && world.getBlockEntity(pos) instanceof RadioBlockEntity blockEntity) {
            return blockEntity.tryToggle(serverWorld, pos, state);
        }
        return ActionResult.PASS;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, net.minecraft.util.hit.BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem
                && new ItemPlacementContext(player, hand, stack, hit).canPlace()) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    public int getComparatorOutput(BlockState state, BlockView world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            return radio.getComparatorOutput();
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(ModBlocks.Properties.RADIO_STATE,
                        ctx.getStack().getOrDefault(DataComponents.RADIO_STATE, RadioState.DISABLED));
    }

    // Убрали @Override
    protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getPickStack(world, pos, state, includeData);

        RadioState radioState = state.get(ModBlocks.Properties.RADIO_STATE);
        stack.set(DataComponents.RADIO_STATE,
                radioState == RadioState.DESTROYED ? RadioState.DESTROYED : RadioState.DISABLED);

        return stack;
    }

    @Override
    protected List<ItemStack> getDroppedStacks(BlockState state, LootWorldContext.Builder builder) {
        RadioState radioState = state.get(ModBlocks.Properties.RADIO_STATE);
        List<ItemStack> list = super.getDroppedStacks(state, builder);

        for (ItemStack stack : list) {
            if (stack.isOf(ModItems.RADIO)) {
                stack.set(DataComponents.RADIO_STATE,
                        radioState == RadioState.DESTROYED ? RadioState.DESTROYED : RadioState.DISABLED);
            }
        }
        return list;
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(Properties.HORIZONTAL_FACING, rotation.rotate(state.get(Properties.HORIZONTAL_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return rotate(state, mirror.getRotation(state.get(Properties.HORIZONTAL_FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(
                ModBlocks.Properties.RADIO_STATE,
                ModBlocks.Properties.LEFT_INDICATOR,
                Properties.POWER,
                Properties.HORIZONTAL_FACING
        );
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            BlockState newState = radio.newBurnedState(world, pos, state);
            if (newState != null) {
                world.setBlockState(pos, newState, Block.NOTIFY_ALL);
                world.playSound(
                        null,
                        pos,
                        SoundEvents.BLOCK_FIRE_EXTINGUISH,
                        SoundCategory.BLOCKS,
                        0.5F,
                        0.7F
                );
            }
        }
    }

    public static void burnRadio(ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            radio.markBurning();
            world.setBlockState(pos, ModBlocks.RADIO.getDefaultState(), Block.NOTIFY_ALL);
        }
    }
}
