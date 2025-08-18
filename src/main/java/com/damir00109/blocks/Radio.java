package com.damir00109.blocks;

import com.damir00109.vpl;
import com.damir00109.audio.*;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Radio {
	public static final IntProperty POWER = IntProperty.of("power", 0, 15);
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty LISTEN = BooleanProperty.of("listen");
	public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

	public static class RadioBlock extends Block {
		public RadioBlock(Settings settings) {
			super(settings.sounds(BlockSoundGroup.WOOD));
			this.setDefaultState(this.stateManager.getDefaultState()
					.with(POWER, 0)
					.with(FACING, Direction.NORTH)
					.with(LISTEN, true)
					.with(ACTIVE, false));
		}

		@Override
		protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
			builder.add(POWER, FACING, LISTEN, ACTIVE);
		}

		@Override
		public BlockState getPlacementState(ItemPlacementContext ctx) {
			return this.getDefaultState()
					.with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
					.with(POWER, 0)
					.with(LISTEN, true)
					.with(ACTIVE, false);
		}

		@Override
		protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
			if (world.isClient) return;
			update(world.getBlockState(pos), world, pos);
		}

		@Override
		public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
			if (world.isClient) return;
			vpl.radios.put(pos, world.getBlockState(pos));
			update(world.getBlockState(pos), world, pos);
		}

		@Override
		public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
			if (world.isClient) return state;

			VoicechatServerApi api = vpl.getAPI();
			if (api == null) return state;

			ServerLevel serverLevel = api.fromServerLevel(world);
			if (serverLevel == null) return state;

			vpl.radios.remove(pos);
			int power = state.get(POWER);

			Channel channel = vpl.getChannel(power);
			if (channel == null) return state;

			channel.removeSender(pos);
			channel.removeListener(pos);

			return state;
		}

		@Override
		protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
			if (world.isClient) return ActionResult.PASS;
			if (!state.get(ACTIVE)) return ActionResult.PASS;

			boolean hasFirstRod = world.getBlockState(pos.up()).isOf(Blocks.LIGHTNING_ROD);
			boolean hasSecondRod = world.getBlockState(pos.up(2)).isOf(Blocks.LIGHTNING_ROD);

			boolean hasBothRods = hasFirstRod && hasSecondRod;

			if (!hasBothRods) return ActionResult.PASS;

			boolean twoRodStackIsClear = vpl.getAnyBlockAbove(pos, world, 5, List.of(world.getBlockState(pos.up()), world.getBlockState(pos.up(2)))) == null;
			if (!twoRodStackIsClear) return ActionResult.FAIL;

			BlockState newState = state.with(LISTEN, !state.get(LISTEN));
			world.setBlockState(pos, newState, 3);
			player.swingHand(Hand.MAIN_HAND);
			((RadioBlock)newState.getBlock()).update(newState, world, pos);

			return ActionResult.SUCCESS;
		}

		public void update(BlockState state, World world, BlockPos currentPos) {
			if (world.isClient) return;

			boolean hasAtLeastOneRod = world.getBlockState(currentPos.up()).isOf(Blocks.LIGHTNING_ROD);
			ArrayList<BlockState> existingRods = new ArrayList<>();
			existingRods.add(world.getBlockState(currentPos.up()));
			boolean isAntennaSetupClear = vpl.getAnyBlockAbove(currentPos, world, 3, existingRods) == null;

			boolean newActiveState = hasAtLeastOneRod && isAntennaSetupClear;
			
			int rawRedstonePower = world.getReceivedRedstonePower(currentPos);
			int finalPowerForState = newActiveState ? rawRedstonePower : 0;

			BlockState potentiallyNewState = state;

			if (state.get(ACTIVE) != newActiveState || state.get(POWER) != finalPowerForState) {
				potentiallyNewState = state.with(ACTIVE, newActiveState).with(POWER, finalPowerForState);
				world.setBlockState(currentPos, potentiallyNewState, 3);
			}
			
			BlockState newState = world.getBlockState(currentPos);
			vpl.radios.put(currentPos, newState);

			VoicechatServerApi api = vpl.getAPI();
			if (api == null) return;

			ServerLevel serverLevel = api.fromServerLevel(world);
			if (serverLevel == null) return;

			Listener listener = getListener(newState, currentPos, serverLevel);
			Sender sender = getSender(newState, currentPos, serverLevel);
					
			boolean currentActive = newState.get(ACTIVE);
			boolean currentListen = newState.get(LISTEN);

			if (!newState.get(ACTIVE)) return;

			if (listener != null) {
				listener.setActive(currentActive && currentListen);
			} else if (sender != null) {
				sender.setActive(currentActive && !currentListen);
			} else {
				throw new IllegalStateException("Neither Listener nor Sender could be created for the radio at " + currentPos);
			}
		}

		@Override
		public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
			if (world.isClient) return;
			update(world.getBlockState(pos), world, pos);
		}

		public Sender getSender(BlockState state, BlockPos pos, ServerLevel level) {
			//if (state.get(POWER) == 0) return null;
			if (!state.get(ACTIVE) || state.get(LISTEN)) return null;
			Channel channel = vpl.getOrCreate(state.get(POWER));
			if (channel == null) return null;
			return channel.getOrCreateSender(level, pos);
		}
		public Listener getListener(BlockState state, BlockPos pos, ServerLevel level) {
			//if (state.get(POWER) == 0) return null;
			if (!state.get(ACTIVE) && !state.get(LISTEN)) return null;
			Channel channel = vpl.getOrCreate(state.get(POWER));
			if (channel == null) return null;
			return channel.getOrCreateListener(level, pos);
		}

		public void onMicrophoneNearby(BlockState state, BlockPos pos, ServerLevel level, MicrophonePacket packet) {
			//if (state.get(POWER) == 0) return;
			Sender sender = getSender(state, pos, level);
			if (sender == null) return;
			sender.send(packet);
		}

		public void onStruckByLightning(World world, BlockPos pos) {
			if (world.isClient()) return;
			BlockState currentState = world.getBlockState(pos);
			int power = currentState.get(POWER);

			world.setBlockState(pos, DModBlocks.BURNT_RADIO.getDefaultState(), 3);

			if (!vpl.radios.containsKey(pos)) return;
			Channel channel = vpl.getChannel(power);
			if (channel != null) {
				channel.removeSender(pos);
				channel.removeListener(pos);
			}
			vpl.radios.remove(pos);
		}
	}
}