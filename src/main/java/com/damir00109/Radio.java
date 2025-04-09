package com.damir00109;

import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.registry.*;
import net.minecraft.block.*;
import net.minecraft.item.*;

public class Radio {
	public static final IntProperty POWER = IntProperty.of("power", 0, 15);
	public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class);
	public static final BooleanProperty LISTEN = BooleanProperty.of("listen");
	public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

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
		private BlockPos pos;

		public RadioBlock(Settings settings) {
			super(settings);
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
		public void onMicrophoneNearby(MicrophonePacket packet) {
			return;
		}

		@Override
		protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
			if (world.isClient) return;
			this.pos = pos;
			world.scheduleBlockTick(pos, this, 1/20);
		}

		@Override
		protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
			this.pos = pos;

			update(state, world);
			world.scheduleBlockTick(pos, this, 1/20);
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

		public void update(BlockState state, World world) {
			if (world.isClient) return;

			BlockPos abovePos = pos.up();
			BlockState aboveState = world.getBlockState(abovePos);

			boolean hasRodAbove = aboveState.isOf(Blocks.LIGHTNING_ROD);
			boolean hasAdjacentRod = getBlockAbove(pos.add(0, 0, 0), world, Blocks.LIGHTNING_ROD, 1) != null;
			boolean hasAdjacentBlocks = VanillaDamir00109.getAnyBlockAbove(pos.add(0,2,0), world, 500) != null;
			boolean newActive = hasRodAbove && !hasAdjacentBlocks;
			boolean newListen = !(newActive && hasAdjacentRod);
			int newPower = world.getReceivedRedstonePower(pos);
			VanillaDamir00109.LOGGER.debug("Radio has updated");

			if (newActive) {
				world.setBlockState(pos, state.with(LISTEN, newListen).with(POWER, newPower).with(ACTIVE, true), 3);
			} else {
				world.setBlockState(pos, state.with(LISTEN, true).with(POWER, 0).with(ACTIVE, false), 3);
			}
		}

		@Override
		public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
			if (world.isClient) return;
			boolean hasAdjacentBlocks = VanillaDamir00109.getAnyBlockAbove(pos.add(0,2,0), world, 500) != null;
			int newPower = world.getReceivedRedstonePower(pos);

			if (hasAdjacentBlocks) newPower = 0;

			if (state.get(POWER) != newPower) world.setBlockState(pos, state.with(POWER, newPower), 2);
		}
	}
}