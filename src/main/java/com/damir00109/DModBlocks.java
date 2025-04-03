package com.damir00109;

import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ChunkUpdateState;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.item.ItemPlacementContext;
import org.jetbrains.annotations.Nullable;

public class DModBlocks {

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
		private boolean listen_sate = true;
		private LocationalAudioChannel channel;

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

		protected void onListenSwitch(
				boolean new_listen,
				BlockState state,
				World world,
				BlockPos pos) {

			if (new_listen) {

			} else {

			}
		}

		public void ListenSwitch(BlockState state, World world, BlockPos pos) {
			if (world.isClient) return;
			int power = world.getReceivedRedstonePower(pos);
			boolean newListen = !this.listen_sate;

			this.onListenSwitch(newListen, state, world, pos);

			this.listen_sate = newListen;
			world.setBlockState(pos, state.with(LISTEN, newListen), 1);

			VanillaDamir00109.LOGGER.info("Block clocked");
		}

		private BlockState getAnyBlockAbove(BlockPos pos, World world, int radius) {
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();

			for (int yOffset = 1; yOffset <= radius; yOffset++) {
				mutablePos.set(pos.getX(), pos.getY()+yOffset, pos.getZ());

				BlockState blockstate = world.getBlockState(mutablePos);
				if (blockstate.isOf(Blocks.VOID_AIR)) continue;
				if (!blockstate.isOf(Blocks.AIR)) return blockstate;

			}
			return null;
		}

		private BlockState getBlockAbove(BlockPos pos, World world, Block target, int radius) {
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();

			for (int yOffset = 1; yOffset <= radius; yOffset++) {
				mutablePos.set(pos.getX(), pos.getY() + yOffset, pos.getZ());

				//if (mutablePos.getY() > world.getTopY()) continue;

				BlockState blockstate = getAnyBlockAbove(mutablePos, world, radius);
				VanillaDamir00109.LOGGER.info("Block up: {}", blockstate);
				if (blockstate != null && blockstate.isOf(target)) {
					return blockstate;
				}
			}
			return null;
		}

		@Override
		public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
			if (!world.isClient) {
				BlockPos abovePos = pos.up();
				BlockState aboveState = world.getBlockState(abovePos);
				boolean hasRodAbove = aboveState.isOf(Blocks.LIGHTNING_ROD);
				boolean hasAdjacentRod = getBlockAbove(pos.add(0, 1, 0), world, Blocks.LIGHTNING_ROD, 1) != null;
				boolean hasAdjacentBlocks = getAnyBlockAbove(pos.add(0,2,0), world, 500) != null;

				int newPower = world.getReceivedRedstonePower(pos);
				boolean newListen = false;

				if (hasRodAbove && !hasAdjacentRod && !hasAdjacentBlocks) {
					newListen = true;
					// Optional: Notify players
					world.getPlayers().forEach(player -> player.sendMessage(
							Text.literal("Lightning Rod detected on Radio!"),
							true
					));
				} else if (hasAdjacentBlocks) {
					newPower = 0;
				}

				if (state.get(POWER) != newPower || state.get(LISTEN) != newListen) {
					world.setBlockState(pos, state.with(POWER, newPower).with(LISTEN, newListen), 2);
				}
			}
		}
	}
}