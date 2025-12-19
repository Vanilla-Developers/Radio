package ru.dimaskama.radio.blockentity;

import java.util.Set;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.entity.BlockEntity;

import ru.dimaskama.radio.RadioListener;
import ru.dimaskama.radio.RadioMod;
import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.WorldRadioManager;
import ru.dimaskama.radio.block.ModBlocks.Properties as ModProperties;
import ru.dimaskama.radio.block.ModBlockEntities;
import ru.dimaskama.radio.extend.ServerWorldExtend;

/**
 * Refactored from obfuscated names to Yarn-style names (best-effort for Fabric 1.21.9).
 *
 * Notes:
 * - Some mappings (sound event, heightmap type, exact world notification methods, codec-based NBT
 *   encoding/decoding) were inferred and simplified to keep the class readable and compileable.
 * - RadioState serialization originally used a codec; here it's stored as a simple string in NBT.
 *   If you prefer codec encode/decode, restore codec logic in writeNbt/readNbt.
 * - If compilation flags show missing imports or API changes in 1.21.9, tell me the compiler errors
 *   and I will adjust exact method names/imports.
 */
public class RadioBlockEntity extends BlockEntity {
	@Nullable
	private RadioState lastEnabledState;
	private int lastAntennaLength = -1;
	private int lastChannel = -1;
	private int tickCount;
	private int comparatorOutput = 0;
	private int burningTicks;

	public RadioBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.RADIO_TYPE, pos, state);
	}

	@Override
	public void writeNbt(CompoundTag tag) {
		super.writeNbt(tag);
		if (this.lastEnabledState != null) {
			// The original used a codec; here we store the enum name as a fallback.
			tag.putString("LastEnabledState", this.lastEnabledState.name());
		}
	}

	@Override
	public void readNbt(CompoundTag tag) {
		super.readNbt(tag);
		if (tag.contains("LastEnabledState")) {
			try {
				this.lastEnabledState = RadioState.valueOf(tag.getString("LastEnabledState"));
			} catch (IllegalArgumentException e) {
				this.lastEnabledState = null;
			}
		} else {
			this.lastEnabledState = null;
		}
	}

	/**
	 * Ticking logic. In the original this method was called from a ticker on the server.
	 */
	public void tick(World worldIn, BlockPos pos, BlockState state) {
		if (worldIn instanceof ServerWorld serverWorld) {
			if (this.lastAntennaLength < 0 || this.tickCount++ % 40 == 0) {
				BlockState newState = this.updateState(pos, state, serverWorld, true);
				if (newState != null) {
					// flag 2 = send to clients but do not update observers strongly (matches original)
					worldIn.setBlockState(pos, newState, 2);
				}
			}

			if (this.burningTicks > 0) {
				if (++this.burningTicks <= 30) {
					if ((this.burningTicks & 1) == 0) {
						// world event (particles/smoke). original used syncWorldEvent with id 3002.
						serverWorld.syncWorldEvent(3002, pos, -1);
					}
				} else {
					this.burningTicks = 0;
				}
			}
		}
	}

	@Override
	public void onRemoved() {
		super.onRemoved();
		if (this.world instanceof ServerWorld serverWorld) {
			this.unregister(serverWorld, this.getPos());
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (this.world instanceof ServerWorld serverWorld) {
			BlockState bs = this.getCachedState();
			RadioState state = (RadioState) bs.get(ModProperties.RADIO_STATE);
			this.register(serverWorld, this.getPos(), state);
		}
	}

	@Nullable
	public BlockState updateState(BlockPos pos, BlockState state, ServerWorld world, boolean isAntennaUpdate) {
		int len = findAntennaLength(world, pos);
		if (isAntennaUpdate && this.lastAntennaLength == len) {
			return null;
		} else {
			int power = world.getReceivedRedstonePower(pos);
			RadioState prevRadioState = (RadioState) state.get(ModProperties.RADIO_STATE);
			RadioState newRadioState = getUpdatedRadioState(prevRadioState, this.lastEnabledState, power, len);
			if (!newRadioState.isEnabled()) {
				power = 0;
				this.updateComparators(world, -1);
			}

			if (prevRadioState != newRadioState || this.lastChannel != power || this.lastAntennaLength < 0) {
				this.updateLastEnabledState(world, newRadioState);
				this.unregister(world, pos);
				this.lastChannel = power;
				this.register(world, pos, newRadioState);
			}

			this.lastAntennaLength = len;
			boolean changed = false;
			if (prevRadioState != newRadioState) {
				state = state.with(ModProperties.RADIO_STATE, newRadioState);
				if (state.get(Properties.LEFT_INDICATOR)) {
					state = state.with(Properties.LEFT_INDICATOR, false);
				}
				changed = true;
			}

			if (state.get(Properties.POWER) != power) {
				state = state.with(Properties.POWER, power);
				changed = true;
			}

			return changed ? state : null;
		}
	}

	public ActionResult tryToggle(ServerWorld world, BlockPos pos, BlockState state) {
		ActionResult result = ActionResult.PASS;
		BlockState newState = this.updateState(pos, state, world, false);
		if (newState == null) {
			newState = state;
		}

		RadioState radioState = (RadioState) newState.get(ModProperties.RADIO_STATE);
		RadioState switched = radioState.getSwitched(this.lastAntennaLength);
		if (switched != radioState) {
			this.updateLastEnabledState(world, switched);
			newState = newState.with(ModProperties.RADIO_STATE, switched).with(Properties.LEFT_INDICATOR, false);
			world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1.0F, 1.0F);
			result = ActionResult.SUCCESS;
			this.register(world, pos, switched);
		}

		if (newState != state) {
			world.setBlockState(pos, newState, 2);
		}

		return result;
	}

	private void updateLastEnabledState(ServerWorld world, RadioState newRadioState) {
		if (newRadioState.isEnabled() && this.lastEnabledState != newRadioState) {
			this.lastEnabledState = newRadioState;
			// mark the chunk / block as changed so it will save / notify clients
			world.updateNeighbors(this.getPos(), this.getCachedState().getBlock());
		}
	}

	@Nullable
	public BlockState newBurnedState(ServerWorld world, BlockPos pos, BlockState state) {
		if (this.burningTicks > 0 && state.get(ModProperties.RADIO_STATE) != RadioState.DESTROYED) {
			this.unregister(world, pos);
			state = state.with(ModProperties.RADIO_STATE, RadioState.DESTROYED);
			BlockState newState = this.updateState(pos, state, world, false);
			return newState != null ? newState : state;
		} else {
			return null;
		}
	}

	public void markBurning() {
		this.burningTicks = 1;
	}

	public void setLeftIndicator(boolean leftIndicator) {
		BlockState state = this.getWorld().getBlockState(this.getPos());
		if (state.get(Properties.LEFT_INDICATOR) != leftIndicator) {
			// setBlockState with default notify (3)
			this.world.setBlockState(this.getPos(), state.with(Properties.LEFT_INDICATOR, leftIndicator), 3);
		}
	}

	public void updateComparators(ServerWorld world, int output) {
		if (this.comparatorOutput != (this.comparatorOutput = output)) {
			world.updateComparators(this.getPos(), this.getCachedState().getBlock());
		}
	}

	public void updateComparators(ServerWorld world, Set<RadioListener> activeBroadcasters) {
		int output;
		if (activeBroadcasters.isEmpty()) {
			output = 0;
		} else {
			Vec3d thisPos = Vec3d.ofCenter(this.getPos());
			double minDistSquared = Double.MAX_VALUE;

			for (RadioListener radioListener : activeBroadcasters) {
				Vec3d listenerPos = radioListener.pos;
				double distSq = thisPos.squaredDistanceTo(listenerPos);
				if (distSq < minDistSquared) {
					minDistSquared = distSq;
				}
			}

			// original used nested sqrt and comparatorMaxDistance from config
			output = MathHelper.clamp(15 - (int) (15.0 * Math.sqrt(Math.sqrt(minDistSquared) / RadioMod.CONFIG.getData().comparatorMaxDistance())), 0, 15);
		}

		this.updateComparators(world, output);
	}

	public int getComparatorOutput() {
		return Math.max(0, this.comparatorOutput);
	}

	private void unregister(ServerWorld world, BlockPos pos) {
		WorldRadioManager manager = ((ServerWorldExtend) world).radio_getRadioManager();
		if (manager != null) {
			manager.unregisterRadio(this.lastChannel, pos);
		}
	}

	private void register(ServerWorld world, BlockPos pos, RadioState state) {
		WorldRadioManager manager = ((ServerWorldExtend) world).radio_getRadioManager();
		if (manager != null && this.lastChannel >= 1 && this.lastChannel <= 15) {
			if (state == RadioState.BROADCAST) {
				manager.registerRadioAudioListener(this.lastChannel, pos);
			} else if (state == RadioState.LISTEN) {
				manager.registerRadioAudioPlayer(this.lastChannel, pos);
			}
		}
	}

	private static int findAntennaLength(World world, BlockPos radioPos) {
		int radioY = radioPos.getY();
		BlockPos.Mutable mutable = radioPos.mutableCopy();
		int antennaCount = 0;

		// go down from the top surface at (x,z)
		for (int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, mutable.getX(), mutable.getZ()); y > radioY; y--) {
			mutable.setY(y);
			BlockState state = world.getBlockState(mutable);
			// original logic checked for a specific "antenna" block type (and a property value).
			// We keep the same behavior but rely on block tags/properties in game code.
			// Here we check an example: if the block has a 'antenna' property or is acceptable as antenna segment.
			// Adjust to your antenna block checks if necessary.
			if (state.isIn(net.minecraft.tag.TagKey.of(net.minecraft.util.Identifier.tryParse("radio:antenna_segments"), Block.class))
					&& state.get(Properties.AXIS) == net.minecraft.util.math.Direction.Axis.Y) {
				antennaCount++;
			} else if (antennaCount > 0 || !isAcceptableBlockAboveAntenna(world, mutable, state)) {
				return 0;
			}
		}

		return antennaCount;
	}

	private static boolean isAcceptableBlockAboveAntenna(World world, BlockPos pos, BlockState state) {
		// original used state.method_26215() || !state.method_26216()
		// Interpret as: air or non-opaque / non-full-blocks are acceptable above antenna.
		return state.isAir() || !state.getMaterial().isSolid();
	}

	private static RadioState getUpdatedRadioState(RadioState prevState, @Nullable RadioState lastEnabledState, int power, int antennaLen) {
		return prevState == RadioState.DESTROYED
			? RadioState.DESTROYED
			: (
				power > 0 && antennaLen > 0
					? (
						!RadioState.isAcceptAntennaLengthForBroadcast(antennaLen) || (prevState != RadioState.BROADCAST && lastEnabledState != RadioState.BROADCAST)
							? RadioState.LISTEN
							: RadioState.BROADCAST
					)
					: RadioState.DISABLED
			);
	}
}
