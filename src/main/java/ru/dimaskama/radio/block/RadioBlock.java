package ru.dimaskama.radio.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.block.ModBlocks;
import ru.dimaskama.radio.blockentity.ModBlockEntities;
import ru.dimaskama.radio.blockentity.RadioBlockEntity;
import ru.dimaskama.radio.item.ModItems;
import ru.dimaskama.radio.item.ModItems.DataComponents;

public class RadioBlock extends Block {

    private static final MapCodec<RadioBlock> CODEC =
            MapCodec.unit(() -> new RadioBlock(Settings.create()));

    public RadioBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getDefaultState()
                        .with(ModBlocks.Properties.RADIO_STATE, RadioState.DISABLED)
                        .with(ModBlocks.Properties.LEFT_INDICATOR, false)
                        .with(Properties.POWER, 0)
                        .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
        );
    }

    @Override
    public MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Nullable
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RadioBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (world.isClient()) return null;
        return createTickerHelper(
                type,
                ModBlockEntities.RADIO_TYPE,
                (world1, pos, state1, be) -> be.tick(world1, pos, state1)
        );
    }

    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
            update(pos, newState, serverWorld);
        }
    }

    public void neighborUpdate(
            BlockState state,
            World world,
            BlockPos pos,
            Block sourceBlock,
            @Nullable BlockPos fromPos,
            boolean notify
    ) {
        super.neighborUpdate(state, world, pos, sourceBlock, fromPos, notify);
        if (world instanceof ServerWorld serverWorld) {
            update(pos, state, serverWorld);
        }
    }

    private void update(BlockPos pos, BlockState state, ServerWorld world) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof RadioBlockEntity blockEntity) {
            BlockState s = blockEntity.updateState(pos, state, world, false);
            if (s != null) {
                world.setBlockState(pos, s, 2);
            }
        }
    }

    public ActionResult onUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            Hand hand,
            BlockHitResult hit
    ) {
        return tryToggle(state, world, pos);
    }

    public void onBlockExploded(BlockState state, World world, BlockPos pos, Explosion explosion) {
        if (explosion.causesFire()) {
            tryToggle(state, world, pos);
        }
        super.onBlockExploded(state, world, pos, explosion);
    }

    protected ActionResult tryToggle(BlockState state, World world, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld &&
                world.getBlockEntity(pos) instanceof RadioBlockEntity blockEntity) {
            return blockEntity.tryToggle(serverWorld, pos, state);
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof RadioBlockEntity r ? r.getComparatorOutput() : 0;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState()
                .with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing())
                .with(
                        ModBlocks.Properties.RADIO_STATE,
                        ctx.getStack().getOrDefault(DataComponents.RADIO_STATE, RadioState.DISABLED)
                );
    }

    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        ItemStack stack = super.getPickStack(world, pos, state);
        stack.set(
                DataComponents.RADIO_STATE,
                state.get(ModBlocks.Properties.RADIO_STATE) == RadioState.DESTROYED
                        ? RadioState.DESTROYED
                        : RadioState.DISABLED
        );
        return stack;
    }


    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(
                Properties.HORIZONTAL_FACING,
                rotation.rotate(state.get(Properties.HORIZONTAL_FACING))
        );
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
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
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            BlockState newState = radio.newBurnedState(world, pos, state);
            if (newState != null) {
                world.setBlockState(pos, newState, 2);
                world.playSound(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SoundEvents.BLOCK_FIRE_AMBIENT,
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
            world.scheduleBlockTick(pos, ModBlocks.RADIO, 6);
        }
    }
}
