package com.damir00109.blocks;

import com.damir00109.VanillaDamir00109;
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

import java.util.ArrayList;
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
					.with(FACING, Direction.EAST)
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
					.with(POWER, ctx.getWorld().getReceivedRedstonePower(ctx.getBlockPos()))
					.with(LISTEN, true)
					.with(ACTIVE, false);
		}

		@Override
		protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
			if (world.isClient) return;
			update(world.getBlockState(pos), world, pos);
		}

		private BlockState getBlockAbove(BlockPos pos, World world, Block target, int radius) {
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();

			for (int yOffset = 1; yOffset <= radius; yOffset++) {
				mutablePos.set(pos.getX(), pos.getY() + yOffset, pos.getZ());

				BlockState blockstate = VanillaDamir00109.getAnyBlockAbove(mutablePos, world, radius);
				if (blockstate != null && blockstate.isOf(target)) {
					return blockstate;
				}
			}
			return null;
		}

		@Override
		public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
			if (world.isClient) return;
			VanillaDamir00109.radios.put(pos, world.getBlockState(pos));
			update(world.getBlockState(pos), world, pos);
		}

		@Override
		public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
			if (world.isClient) return state;
			VanillaDamir00109.radios.remove(pos);
			return state;
		}

		@Override
		protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
			if (world.isClient) return ActionResult.PASS;

			boolean hasFirstRod = world.getBlockState(pos.up()).isOf(Blocks.LIGHTNING_ROD);
			boolean hasSecondRod = world.getBlockState(pos.up(2)).isOf(Blocks.LIGHTNING_ROD);

			if (hasFirstRod && hasSecondRod) {
				List<BlockState> rodException = List.of(Blocks.LIGHTNING_ROD.getDefaultState());
				BlockState obstructingBlockOverSecondRod = VanillaDamir00109.getAnyBlockAbove(pos.up(2), world, world.getHeight() - pos.getY() - 3, rodException);

				if (obstructingBlockOverSecondRod != null) {
					return ActionResult.FAIL;
				}

				BlockState newState = state.with(LISTEN, !state.get(LISTEN));
				world.setBlockState(pos, newState, 3);
				update(newState, world, pos);
				player.swingHand(Hand.MAIN_HAND);
				return ActionResult.SUCCESS;
			}

			return ActionResult.PASS;
		}

		public void update(BlockState state, World world, BlockPos currentPos) {
			if (world.isClient) return;

			boolean hasFirstRod = world.getBlockState(currentPos.up()).isOf(Blocks.LIGHTNING_ROD);
			boolean isObstructed = false;

			if (hasFirstRod) {
				List<BlockState> rodException = List.of(Blocks.LIGHTNING_ROD.getDefaultState());
				BlockState obstructingBlock = VanillaDamir00109.getAnyBlockAbove(currentPos.up(), world, world.getHeight() - currentPos.getY() - 2, rodException);
				isObstructed = obstructingBlock != null;
			}

			boolean newActive = hasFirstRod && !isObstructed;
			int rawRedstonePower = world.getReceivedRedstonePower(currentPos);
			int finalPowerForState = newActive ? rawRedstonePower : 0;
			boolean currentListen = state.get(LISTEN);

			BlockState potentiallyNewState = state;

			if (state.get(ACTIVE) != newActive || state.get(POWER) != finalPowerForState) {
				potentiallyNewState = state.with(ACTIVE, newActive).with(POWER, finalPowerForState);
				world.setBlockState(currentPos, potentiallyNewState, 3);
			}
			
			VanillaDamir00109.radios.put(currentPos, world.getBlockState(currentPos));

			Channel channel = VanillaDamir00109.getOrCreate(rawRedstonePower);
			VoicechatServerApi api = VanillaDamir00109.getAPI();
			
			Listener listener = getListener(potentiallyNewState, currentPos, api.fromServerLevel(world));
			Sender sender = getSender(potentiallyNewState, currentPos, api.fromServerLevel(world));

			listener.setActive(newActive && currentListen);
			sender.setActive(newActive && !currentListen);
		}

		@Override
		public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
			if (world.isClient) return;
			update(world.getBlockState(pos), world, pos);
		}

		public Sender getSender(BlockState state, BlockPos pos, ServerLevel level) {
			Channel channel = VanillaDamir00109.getOrCreate(state.get(POWER));
			VoicechatServerApi api = VanillaDamir00109.getAPI();
			return channel.getOrCreateSender(level, pos);
		}
		public Listener getListener(BlockState state, BlockPos pos, ServerLevel level) {
			Channel channel = VanillaDamir00109.getOrCreate(state.get(POWER));
			VoicechatServerApi api = VanillaDamir00109.getAPI();
			return channel.getOrCreateListener(level, pos);
		}

		public void onMicrophoneNearby(BlockState state, BlockPos pos, ServerLevel level, MicrophonePacket packet) {
			Sender sender = getSender(state, pos, level);
			sender.send(packet);
		}

		public void onStruckByLightning(World world, BlockPos pos) {
			if (!world.isClient()) {
				BlockState currentState = world.getBlockState(pos);
				int power = currentState.get(POWER);

				// Превращаем в сгоревшее радио
				world.setBlockState(pos, DModBlocks.BURNT_RADIO.getDefaultState(), 3);

				// Удаляем радио из активных
				if (VanillaDamir00109.radios.containsKey(pos)) {
					Channel channel = VanillaDamir00109.getChannel(power);
					if (channel != null) {
						channel.removeRadio(pos);
					}
					VanillaDamir00109.radios.remove(pos);
				}
				// Тут можно добавить дополнительные эффекты, например, дым или звук
			}
		}
	}
}