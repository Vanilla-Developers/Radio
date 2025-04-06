package com.damir00109;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.item.ItemPlacementContext;
import org.jetbrains.annotations.Nullable;

public class Radio {
	private static int lastSenderIndex = -1;
	private static int lastListenerIndex = -1;


	public static final IntProperty POWER = IntProperty.of("power", 0, 15);
	public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class);
	public static final BooleanProperty LISTEN = BooleanProperty.of("listen");

	public static final Block RADIO = registerBlock("radio", new RadioBlock(Block.Settings.create()
			.mapColor(MapColor.STONE_GRAY)
			.strength(1.5f)
			.pistonBehavior(PistonBehavior.BLOCK)
			.registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(VanillaDamir00109.MOD_ID, "radio")))
	));

	private static Block registerBlock(String name, Block block) {
		Identifier id = Identifier.of(VanillaDamir00109.MOD_ID, name);
		Registry.register(Registries.BLOCK, id, block);
		Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()
				.registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
		));
		return block;
	}

	public static void registerModBlocks() {
		VanillaDamir00109.LOGGER.info("Registering Mod Blocks for " + VanillaDamir00109.MOD_ID);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
			entries.add(RADIO);
		});
	}

	public static class RadioBlock extends Block {
		private ServerLevel level;
		private BlockPos pos;
		private int power = 0;
		private RadioChannel channel;
		private RadioSender sender;
		private RadioListener listener;
		private BlockState state;

		public RadioBlock(Settings settings) {
			super(settings);
			this.setDefaultState(this.stateManager.getDefaultState()
					.with(POWER, 0)
					.with(FACING, Direction.EAST)
					.with(LISTEN, true));
		}

		@Override
		protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
			builder.add(POWER, FACING, LISTEN);
		}

		@Override
		public BlockState getPlacementState(ItemPlacementContext ctx) {
			return this.getDefaultState()
					.with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
					.with(POWER, ctx.getWorld().getReceivedRedstonePower(ctx.getBlockPos()))
					.with(LISTEN, true);
		}
		public void onMicrophoneNearby(MicrophonePacket packet) {
			updateChannel();
			if (channel == null) return;
			VanillaDamir00109.LOGGER.info("LISTEN={}", state.get(LISTEN));

			if (!state.get(LISTEN)) {
				byte[] audio = packet.getOpusEncodedData();

				createSender(-1);
				if (sender == null) return;
				sender.send(audio);
			}
		}


		private void updateChannel() {
			if (channel != null || power < 1) return;
			channel = VanillaDamir00109.createChannel(power);

			VanillaDamir00109.LOGGER.info("Channel num.: {}", (power-1));
		}

		@Override
		protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
			if (world.isClient) return;
			super.onBlockAdded(state, world, pos, oldState, notify);
			level = VanillaDamir00109.get_VCAPI().fromServerLevel(world);
			this.pos = pos;
			world.scheduleBlockTick(pos, this, 1/20);
		}

		@Override
		protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
			level = VanillaDamir00109.get_VCAPI().fromServerLevel(world);
			this.pos = pos;

			update(state, world);
			updateChannel();
			world.scheduleBlockTick(pos, this, 1/20);
		}

		protected void onListenSwitch(
				boolean new_listen,
				BlockState state,
				World world,
				BlockPos pos) {
			updateChannel();
			if(power < 1 || channel == null) return;
			createSender(-1);
			createListener(-1);

			if (!new_listen) {
				// If Radio mode is "speaking"
				listener.setActive(false);
			} else {
				// If Radio mode is "listen"
				sender.setActive(false);
			}
		}
		private void createSender(int index) {
			if (index < 0) index = Radio.lastSenderIndex+1;
			if (channel == null) return;
			if (sender != null) return;
			if (channel.getSender(Radio.lastSenderIndex+1) != null) {
				createSender(index+1);
			} else {
				sender = channel.newSenderWith(Radio.lastSenderIndex + 1, level, pos.getX(), pos.getY(), pos.getZ());
			}
			if (index-1 == Radio.lastSenderIndex) Radio.lastSenderIndex = sender.getIndex();
		}

		private void createListener(int index) {
			if (index < 0) index = Radio.lastListenerIndex+1;
			if (channel == null) return;
			if (listener != null) return;
			if (channel.getListener(index) != null) {
				createListener(index+1);
			} else {
				listener = channel.newListenerWith(index, level, pos.getX(), pos.getY(), pos.getZ());
			}
			if (index-1 == Radio.lastListenerIndex) Radio.lastListenerIndex = listener.getIndex();
		}

		private BlockState getAnyBlockAbove(BlockPos pos, World world, int radius) {
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();

			for (int yOffset = 1; yOffset <= radius; yOffset++) {
				mutablePos.set(pos.getX(), pos.getY()+yOffset, pos.getZ());

				BlockState blockstate = world.getBlockState(mutablePos);
				if (blockstate.isOf(Blocks.VOID_AIR) || blockstate.isOf(Blocks.AIR)) continue;
				return blockstate;

			}
			return null;
		}

		private BlockState getBlockAbove(BlockPos pos, World world, Block target, int radius) {
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();

			for (int yOffset = 1; yOffset <= radius; yOffset++) {
				mutablePos.set(pos.getX(), pos.getY() + yOffset, pos.getZ());

				BlockState blockstate = getAnyBlockAbove(mutablePos, world, radius);
				if (blockstate != null && blockstate.isOf(target)) {
					return blockstate;
				}
			}
			return null;
		}

		public void update(BlockState state, World world) {
			if (world.isClient) return;

			BlockPos abovePos = pos.up();
			BlockState aboveState = world.getBlockState(abovePos);

			boolean hasRodAbove = aboveState.isOf(Blocks.LIGHTNING_ROD);
			boolean hasAdjacentRod = getBlockAbove(pos.add(0, 0, 0), world, Blocks.LIGHTNING_ROD, 1) != null;
			boolean hasAdjacentBlocks = getAnyBlockAbove(pos.add(0,2,0), world, 500) != null;
			boolean newListen = hasRodAbove && !hasAdjacentRod && !hasAdjacentBlocks;

			world.setBlockState(pos, state.with(LISTEN, newListen), 2);
			this.onListenSwitch(newListen, state, world, pos);
			this.state = state;
		}

		@Override
		public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
			if (world.isClient) return;
			boolean hasAdjacentBlocks = getAnyBlockAbove(pos.add(0,2,0), world, 500) != null;

			int newPower = world.getReceivedRedstonePower(pos);

			if (hasAdjacentBlocks) newPower = 0;
			updateChannel();
			if (!state.get(LISTEN)) {createSender(-1);}else{createListener(-1);}

			if (state.get(POWER) != newPower) {
				world.setBlockState(pos, state.with(POWER, newPower), 2);
				this.power = newPower;
			}
		}
	}
}