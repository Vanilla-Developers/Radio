package ru.dimaskama.radio.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockItem;
import net.minecraft.block.BlockRotation;
import net.minecraft.block.BlockMirror;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.state.property.Properties; // referenced as net.minecraft.state.property.Properties.HORIZONTAL_FACING / POWER
import net.minecraft.state.StateManager;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.block.ModBlocks.Properties as ModProperties;
import ru.dimaskama.radio.blockentity.ModBlockEntities;
import ru.dimaskama.radio.blockentity.RadioBlockEntity;
import ru.dimaskama.radio.item.ModItems;
import ru.dimaskama.radio.item.ModItems.DataComponents;

/**
 * Renamed obfuscated names to Yarn mappings for Fabric 1.21.9 as best-effort.
 * NOTE: some method/field mappings (especially codec registration and item component accesses)
 * are approximated so they read clearly with Yarn names. Adjust further if you need exact compile-time API calls.
 */
public class RadioBlock extends Block {
	private final MapCodec<RadioBlock> CODEC = MapCodec.of(RadioBlock::new);

	protected RadioBlock(Settings settings) {
		super(settings);
		// Initialize default block state with readable property names.
		this.setDefaultState(
			this.getDefaultState()
				.with(ModBlocks.Properties.RADIO_STATE, RadioState.DISABLED)
				.with(ModBlocks.Properties.LEFT_INDICATOR, false)
				.with(net.minecraft.state.property.Properties.POWER, 0)
				.with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, Direction.NORTH)
		);
	}

	protected MapCodec<RadioBlock> getCodec() {
		return this.CODEC;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new RadioBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, ModBlockEntities.RADIO_TYPE, (world1, pos, state1, blockEntity) -> blockEntity.tick(world1, pos, state1));
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		super.onStateReplaced(state, world, pos, newState, moved);
		// If the block type changed (not just state), update the block entity on the server
		if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
			this.update(pos, newState, serverWorld);
		}
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable BlockPos fromPos, boolean notify) {
		super.neighborUpdate(state, world, pos, sourceBlock, fromPos, notify);
		if (world instanceof ServerWorld serverWorld) {
			this.update(pos, state, serverWorld);
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

	// Interaction without explicit hand (delegates to toggle behavior)
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, net.minecraft.entity.player.PlayerEntity player, BlockHitResult hit) {
		return this.tryToggle(state, world, pos);
	}

	// Called when the block is exploded
	@Override
	public void onBlockExploded(BlockState state, World world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
		if (explosion.causesFire()) {
			this.tryToggle(state, world, pos);
		}
		super.onBlockExploded(state, world, pos, explosion, stackMerger);
	}

	protected ActionResult tryToggle(BlockState state, World world, BlockPos pos) {
		return (world instanceof ServerWorld serverWorld && world.getBlockEntity(pos) instanceof RadioBlockEntity blockEntity)
			? blockEntity.tryToggle(serverWorld, pos, state)
			: ActionResult.PASS;
	}

	// Interaction with an item stack (when player attempts to place another block/item on this block)
	@Override
	public ActionResult onUse(ItemStack stack, BlockState state, World world, BlockPos pos, net.minecraft.entity.player.PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof BlockItem && new net.minecraft.item.ItemPlacementContext(player, hand, stack, hit).canPlace()) {
			return ActionResult.SUCCESS;
		} else {
			return ActionResult.PASS;
		}
	}

	@Override
	public boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
		BlockEntity be = world.getBlockEntity(pos);
		return be instanceof RadioBlockEntity radioBlockEntity ? radioBlockEntity.getComparatorOutput() : 0;
	}

	@Nullable
	@Override
	public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext ctx) {
		return this.getDefaultState()
			.with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing())
			.with(ModBlocks.Properties.RADIO_STATE, (RadioState)ctx.getStack().getComponent(DataComponents.RADIO_STATE, RadioState.DISABLED));
	}

	@Override
	public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
		ItemStack stack = super.getPickStack(world, pos, state);
		RadioState radioState = state.get(ModBlocks.Properties.RADIO_STATE);
		stack.getComponent(DataComponents.RADIO_STATE).set(radioState == RadioState.DESTROYED ? RadioState.DESTROYED : RadioState.DISABLED);
		return stack;
	}

	@Override
	public void appendStacks(ItemGroup group, DefaultedList<ItemStack> stacks) {
		RadioState radioState = this.getDefaultState().get(ModBlocks.Properties.RADIO_STATE);
		super.appendStacks(group, stacks);
		for (ItemStack stack : stacks) {
			if (stack.isOf(ModItems.RADIO)) {
				stack.getComponent(DataComponents.RADIO_STATE).set(radioState == RadioState.DESTROYED ? RadioState.DESTROYED : RadioState.DISABLED);
			}
		}
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, rotation.rotate(state.get(net.minecraft.state.property.Properties.HORIZONTAL_FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return this.rotate(state, mirror.getRotation(state.get(net.minecraft.state.property.Properties.HORIZONTAL_FACING)));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(ModBlocks.Properties.RADIO_STATE, ModBlocks.Properties.LEFT_INDICATOR, net.minecraft.state.property.Properties.POWER, net.minecraft.state.property.Properties.HORIZONTAL_FACING);
	}

	@Override
	public void onSyncedBlockEvent(BlockState state, World world, BlockPos pos, net.minecraft.structure.StructurePiece.Info info) {
		super.onSyncedBlockEvent(state, world, pos, info);
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getBlockEntity(pos) instanceof RadioBlockEntity radioBlockEntity) {
			BlockState newState = radioBlockEntity.newBurnedState(world, pos, state);
			if (newState != null) {
				world.setBlockState(pos, newState, 2);
				world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, net.minecraft.sound.SoundEvents.BLOCK_FIRE_AMBIENT, net.minecraft.sound.SoundCategory.BLOCKS, 0.5F, 0.7F);
			}
		}
	}

	public static void burnRadio(ServerWorld world, BlockPos pos) {
		if (world.getBlockEntity(pos) instanceof RadioBlockEntity radioBlockEntity) {
			radioBlockEntity.markBurning();
			world.createAndScheduleBlockTick(pos, ModBlocks.RADIO, 6);
		}
	}
}
