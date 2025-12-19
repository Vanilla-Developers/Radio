package ru.dimaskama.radio.blockentity;

import java.util.Set;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
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

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;

import ru.dimaskama.radio.RadioListener;
import ru.dimaskama.radio.RadioMod;
import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.WorldRadioManager;
import ru.dimaskama.radio.block.ModBlocks;
import ru.dimaskama.radio.blockentity.ModBlockEntities;
import ru.dimaskama.radio.extend.ServerWorldExtend;

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

    protected void writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
        if (this.lastEnabledState != null) {
            tag.putString("LastEnabledState", this.lastEnabledState.name());
        }
    }

    protected void readNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
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

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            this.unregister(serverWorld, this.getPos());
        }
    }

    public void onLoad() {
        assert this.getWorld() != null;
        if (!this.getWorld().isClient() && this.getWorld() instanceof ServerWorld serverWorld) {
            BlockState bs = this.getCachedState();
            RadioState state = bs.get(ModBlocks.Properties.RADIO_STATE);
            this.register(serverWorld, this.getPos(), state);
        }
    }

    @Nullable
    public BlockState updateState(BlockPos pos, BlockState state, ServerWorld world, boolean isAntennaUpdate) {
        int len = findAntennaLength(world, pos);
        if (isAntennaUpdate && this.lastAntennaLength == len) {
            return null;
        }

        int power = world.getReceivedRedstonePower(pos);
        RadioState prevRadioState = state.get(ModBlocks.Properties.RADIO_STATE);
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
            state = state.with(ModBlocks.Properties.RADIO_STATE, newRadioState);
            if (state.get(ModBlocks.Properties.LEFT_INDICATOR)) {
                state = state.with(ModBlocks.Properties.LEFT_INDICATOR, false);
            }
            changed = true;
        }

        if (state.get(Properties.POWER) != power) {
            state = state.with(Properties.POWER, power);
            changed = true;
        }

        return changed ? state : null;
    }

    public ActionResult tryToggle(ServerWorld world, BlockPos pos, BlockState state) {
        ActionResult result = ActionResult.PASS;
        BlockState newState = this.updateState(pos, state, world, false);
        if (newState == null) newState = state;

        RadioState radioState = newState.get(ModBlocks.Properties.RADIO_STATE);
        RadioState switched = radioState.getSwitched(this.lastAntennaLength);
        if (switched != radioState) {
            this.updateLastEnabledState(world, switched);
            newState = newState.with(ModBlocks.Properties.RADIO_STATE, switched).with(ModBlocks.Properties.LEFT_INDICATOR, false);
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
            world.updateNeighborsAlways(this.getPos(), this.getCachedState().getBlock(), null);
        }
    }

    @Nullable
    public BlockState newBurnedState(ServerWorld world, BlockPos pos, BlockState state) {
        if (this.burningTicks > 0 && state.get(ModBlocks.Properties.RADIO_STATE) != RadioState.DESTROYED) {
            this.unregister(world, pos);
            state = state.with(ModBlocks.Properties.RADIO_STATE, RadioState.DESTROYED);
            BlockState newState = this.updateState(pos, state, world, false);
            return newState != null ? newState : state;
        }
        return null;
    }

    public void markBurning() {
        this.burningTicks = 1;
    }

    public void setLeftIndicator(boolean leftIndicator) {
        BlockState state = this.getWorld().getBlockState(this.getPos());
        if (state.get(ModBlocks.Properties.LEFT_INDICATOR) != leftIndicator) {
            this.getWorld().setBlockState(this.getPos(), state.with(ModBlocks.Properties.LEFT_INDICATOR, leftIndicator), 3);
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
                if (distSq < minDistSquared) minDistSquared = distSq;
            }
            output = MathHelper.clamp(15 - (int)(15.0 * Math.sqrt(Math.sqrt(minDistSquared) / RadioMod.CONFIG.getData().comparatorMaxDistance())), 0, 15);
        }
        this.updateComparators(world, output);
    }

    public int getComparatorOutput() {
        return Math.max(0, this.comparatorOutput);
    }

    private void unregister(ServerWorld world, BlockPos pos) {
        WorldRadioManager manager = ((ServerWorldExtend) world).radio_getRadioManager();
        if (manager != null) manager.unregisterRadio(this.lastChannel, pos);
    }

    private void register(ServerWorld world, BlockPos pos, RadioState state) {
        WorldRadioManager manager = ((ServerWorldExtend) world).radio_getRadioManager();
        if (manager != null && this.lastChannel >= 1 && this.lastChannel <= 15) {
            if (state == RadioState.BROADCAST) manager.registerRadioAudioListener(this.lastChannel, pos);
            else if (state == RadioState.LISTEN) manager.registerRadioAudioPlayer(this.lastChannel, pos);
        }
    }

    private static int findAntennaLength(World world, BlockPos radioPos) {
        int radioY = radioPos.getY();
        BlockPos.Mutable mutable = radioPos.mutableCopy();
        int antennaCount = 0;

        for (int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, mutable.getX(), mutable.getZ()); y > radioY; y--) {
            mutable.setY(y);
            BlockState state = world.getBlockState(mutable);
            if (state.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of("radio", "antenna_segments")))
                    && state.get(Properties.AXIS) == net.minecraft.util.math.Direction.Axis.Y) {
                antennaCount++;
            } else if (antennaCount > 0 || !isAcceptableBlockAboveAntenna(world, mutable, state)) {
                return 0;
            }
        }

        return antennaCount;
    }

    private static boolean isAcceptableBlockAboveAntenna(World world, BlockPos pos, BlockState state) {
        return state.isAir() || !state.isSolidBlock(world, pos);
    }

    private static RadioState getUpdatedRadioState(RadioState prevState, @Nullable RadioState lastEnabledState, int power, int antennaLen) {
        if (prevState == RadioState.DESTROYED) return RadioState.DESTROYED;
        if (power > 0 && antennaLen > 0) {
            if (!RadioState.isAcceptAntennaLengthForBroadcast(antennaLen) || (prevState != RadioState.BROADCAST && lastEnabledState != RadioState.BROADCAST)) {
                return RadioState.LISTEN;
            } else {
                return RadioState.BROADCAST;
            }
        }
        return RadioState.DISABLED;
    }

    // Метод tick, который вызывается из тикера
    public void tick(World world, BlockPos pos, BlockState state) {
        // Обработка горения
        if (this.burningTicks > 0) {
            this.burningTicks++;
            if (this.burningTicks > 20) {
                this.burningTicks = 0;
            }
        }
    }
}