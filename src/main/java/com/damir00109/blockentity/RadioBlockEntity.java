package com.damir00109.blockentity;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.damir00109.RadioListener;
import com.damir00109.RadioMod;
import com.damir00109.RadioState;
import com.damir00109.WorldRadioManager;
import com.damir00109.block.ModBlocks;
import com.damir00109.blockentity.ModBlockEntities;
import com.damir00109.extend.ServerWorldExtend;

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

    protected void writeNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registryLookup) {
        if (this.lastEnabledState != null) {
            tag.putString("LastEnabledState", this.lastEnabledState.name());
        }
    }

    protected void readNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registryLookup) {
        if (tag.contains("LastEnabledState")) {
            try {
                this.lastEnabledState = RadioState.valueOf(String.valueOf(tag.getString("LastEnabledState")));
            } catch (IllegalArgumentException e) {
                this.lastEnabledState = null;
            }
        } else {
            this.lastEnabledState = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.getLevel() instanceof ServerLevel serverWorld) {
            this.unregister(serverWorld, this.getBlockPos());
        }
    }

    public void onLoad() {
        assert this.getLevel() != null;
        if (!this.getLevel().isClientSide() && this.getLevel() instanceof ServerLevel serverWorld) {
            BlockState bs = this.getBlockState();
            RadioState state = bs.getValue(ModBlocks.Properties.RADIO_STATE);
            this.register(serverWorld, this.getBlockPos(), state);
        }
    }

    @Nullable
    public BlockState updateState(BlockPos pos, BlockState state, ServerLevel world, boolean isAntennaUpdate) {
        int len = findAntennaLength(world, pos);
        if (isAntennaUpdate && this.lastAntennaLength == len) {
            return null;
        }

        int power = world.getBestNeighborSignal(pos);
        RadioState prevRadioState = state.getValue(ModBlocks.Properties.RADIO_STATE);
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
            state = state.setValue(ModBlocks.Properties.RADIO_STATE, newRadioState);
            if (state.getValue(ModBlocks.Properties.LEFT_INDICATOR)) {
                state = state.setValue(ModBlocks.Properties.LEFT_INDICATOR, false);
            }
            changed = true;
        }

        if (state.getValue(BlockStateProperties.POWER) != power) {
            state = state.setValue(BlockStateProperties.POWER, power);
            changed = true;
        }

        return changed ? state : null;
    }

    public InteractionResult tryToggle(ServerLevel world, BlockPos pos, BlockState state) {
        InteractionResult result = InteractionResult.PASS;
        BlockState newState = this.updateState(pos, state, world, false);
        if (newState == null) newState = state;

        RadioState radioState = newState.getValue(ModBlocks.Properties.RADIO_STATE);
        RadioState switched = radioState.getSwitched(this.lastAntennaLength);
        if (switched != radioState) {
            this.updateLastEnabledState(world, switched);
            newState = newState.setValue(ModBlocks.Properties.RADIO_STATE, switched).setValue(ModBlocks.Properties.LEFT_INDICATOR, false);
            world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 1.0F);
            result = InteractionResult.SUCCESS;
            this.register(world, pos, switched);
        }

        if (newState != state) {
            world.setBlock(pos, newState, 2);
        }

        return result;
    }

    private void updateLastEnabledState(ServerLevel world, RadioState newRadioState) {
        if (newRadioState.isEnabled() && this.lastEnabledState != newRadioState) {
            this.lastEnabledState = newRadioState;
            world.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock(), null);
        }
    }

    @Nullable
    public BlockState newBurnedState(ServerLevel world, BlockPos pos, BlockState state) {
        if (this.burningTicks > 0 && state.getValue(ModBlocks.Properties.RADIO_STATE) != RadioState.DESTROYED) {
            this.unregister(world, pos);
            state = state.setValue(ModBlocks.Properties.RADIO_STATE, RadioState.DESTROYED);
            BlockState newState = this.updateState(pos, state, world, false);
            return newState != null ? newState : state;
        }
        return null;
    }

    public void markBurning() {
        this.burningTicks = 1;
    }

    public void setLeftIndicator(boolean leftIndicator) {
        BlockState state = this.getLevel().getBlockState(this.getBlockPos());
        if (state.getValue(ModBlocks.Properties.LEFT_INDICATOR) != leftIndicator) {
            this.getLevel().setBlock(this.getBlockPos(), state.setValue(ModBlocks.Properties.LEFT_INDICATOR, leftIndicator), 3);
        }
    }

    public void updateComparators(ServerLevel world, int output) {
        if (this.comparatorOutput != output) {
            this.comparatorOutput = output;
            world.updateNeighbourForOutputSignal(this.getBlockPos(), this.getBlockState().getBlock());
        }
    }

    public void updateComparators(ServerLevel world, Set<RadioListener> activeBroadcasters) {
        int output;
        if (activeBroadcasters.isEmpty()) {
            output = 0;
        } else {
            Vec3 thisPos = Vec3.atCenterOf(this.getBlockPos());
            double minDistSquared = Double.MAX_VALUE;
            for (RadioListener radioListener : activeBroadcasters) {
                Vec3 listenerPos = radioListener.pos;
                double distSq = thisPos.distanceToSqr(listenerPos);
                if (distSq < minDistSquared) minDistSquared = distSq;
            }
            output = Mth.clamp(15 - (int)(15.0 * Math.sqrt(Math.sqrt(minDistSquared) / RadioMod.CONFIG.getData().comparatorMaxDistance())), 0, 15);
        }
        this.updateComparators(world, output);
    }

    public int getComparatorOutput() {
        return Math.max(0, this.comparatorOutput);
    }

    private void unregister(ServerLevel world, BlockPos pos) {
        WorldRadioManager manager = ((ServerWorldExtend) world).radio_getRadioManager();
        if (manager != null) manager.unregisterRadio(this.lastChannel, pos);
    }

    private void register(ServerLevel world, BlockPos pos, RadioState state) {
        WorldRadioManager manager = ((ServerWorldExtend) world).radio_getRadioManager();
        if (manager != null && this.lastChannel >= 1 && this.lastChannel <= 15) {
            if (state == RadioState.BROADCAST) manager.registerRadioAudioListener(this.lastChannel, pos);
            else if (state == RadioState.LISTEN) manager.registerRadioAudioPlayer(this.lastChannel, pos);
        }
    }

    private static int findAntennaLength(Level world, BlockPos radioPos) {
        int radioY = radioPos.getY();
        BlockPos.MutableBlockPos mutable = radioPos.mutable();
        int antennaCount = 0;

        for (int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, mutable.getX(), mutable.getZ()); y > radioY; y--) {
            mutable.setY(y);
            BlockState state = world.getBlockState(mutable);
            if (state.is(TagKey.create(Registries.BLOCK, RadioMod.id("antenna_segments"))) && isVerticalAntennaSegment(state)) {
                antennaCount++;
            } else if (antennaCount > 0 || !isAcceptableBlockAboveAntenna(world, mutable, state)) {
                return 0;
            }
        }

        return antennaCount;
    }

    private static boolean isVerticalAntennaSegment(BlockState state) {
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING).getAxis() == Direction.Axis.Y;
        }
        return true;
    }

    private static boolean isAcceptableBlockAboveAntenna(Level world, BlockPos pos, BlockState state) {
        return state.isAir() || !state.isRedstoneConductor(world, pos);
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
    public void tick(Level world, BlockPos pos, BlockState state) {
        // Обработка горения
        if (this.burningTicks > 0) {
            this.burningTicks++;
            if (this.burningTicks > 20) {
                this.burningTicks = 0;
            }
        }
    }
}