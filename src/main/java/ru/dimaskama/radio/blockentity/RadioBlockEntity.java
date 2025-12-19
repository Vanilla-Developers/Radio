package ru.dimaskama.radio.blockentity;

import java.util.Set;
import net.minecraft.class_11368;
import net.minecraft.class_11372;
import net.minecraft.class_1269;
import net.minecraft.class_1936;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_3218;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_3481;
import net.minecraft.class_3532;
import net.minecraft.class_2338.class_2339;
import net.minecraft.class_2902.class_2903;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.radio.RadioListener;
import ru.dimaskama.radio.RadioMod;
import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.WorldRadioManager;
import ru.dimaskama.radio.block.ModBlocks.Properties;
import ru.dimaskama.radio.extend.ServerWorldExtend;

public class RadioBlockEntity extends class_2586 {
	@Nullable
	@Nullable
	private RadioState lastEnabledState;
	private int lastAntennaLength = -1;
	private int lastChannel = -1;
	private int tickCount;
	private int comparatorOutput = 0;
	private int burningTicks;

	public RadioBlockEntity(class_2338 pos, class_2680 state) {
		super(ModBlockEntities.RADIO_TYPE, pos, state);
	}

	protected void method_11007(class_11372 view) {
		super.method_11007(view);
		if (this.lastEnabledState != null) {
			view.method_71468("LastEnabledState", RadioState.CODEC, this.lastEnabledState);
		}
	}

	protected void method_11014(class_11368 view) {
		super.method_11014(view);
		this.lastEnabledState = (RadioState)view.method_71426("LastEnabledState", RadioState.CODEC).orElse(null);
	}

	public void tick(class_1937 world, class_2338 pos, class_2680 state) {
		if (world instanceof class_3218 serverWorld) {
			if (this.lastAntennaLength < 0 || this.tickCount++ % 40 == 0) {
				class_2680 newState = this.updateState(pos, state, serverWorld, true);
				if (newState != null) {
					world.method_8652(pos, newState, 2);
				}
			}

			if (this.burningTicks > 0) {
				if (++this.burningTicks <= 30) {
					if ((this.burningTicks & 1) == 0) {
						world.method_20290(3002, pos, -1);
					}
				} else {
					this.burningTicks = 0;
				}
			}
		}
	}

	public void method_11012() {
		super.method_11012();
		if (this.field_11863 instanceof class_3218 serverWorld) {
			this.unregister(serverWorld, this.method_11016());
		}
	}

	public void method_10996() {
		super.method_10996();
		if (this.field_11863 instanceof class_3218 serverWorld) {
			this.register(serverWorld, this.method_11016(), (RadioState)this.method_11010().method_11654(Properties.RADIO_STATE));
		}
	}

	@Nullable
	public class_2680 updateState(class_2338 pos, class_2680 state, class_3218 world, boolean isAntennaUpdate) {
		int len = findAntennaLength(world, pos);
		if (isAntennaUpdate && this.lastAntennaLength == len) {
			return null;
		} else {
			int power = world.method_49804(pos);
			RadioState prevRadioState = (RadioState)state.method_11654(Properties.RADIO_STATE);
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
				state = (class_2680)state.method_11657(Properties.RADIO_STATE, newRadioState);
				if ((Boolean)state.method_11654(Properties.LEFT_INDICATOR)) {
					state = (class_2680)state.method_11657(Properties.LEFT_INDICATOR, false);
				}

				changed = true;
			}

			if ((Integer)state.method_11654(class_2741.field_12511) != power) {
				state = (class_2680)state.method_11657(class_2741.field_12511, power);
				changed = true;
			}

			return changed ? state : null;
		}
	}

	public class_1269 tryToggle(class_3218 world, class_2338 pos, class_2680 state) {
		class_1269 result = class_1269.field_5811;
		class_2680 newState = this.updateState(pos, state, world, false);
		if (newState == null) {
			newState = state;
		}

		RadioState radioState = (RadioState)newState.method_11654(Properties.RADIO_STATE);
		RadioState switched = radioState.getSwitched(this.lastAntennaLength);
		if (switched != radioState) {
			this.updateLastEnabledState(world, switched);
			newState = (class_2680)((class_2680)newState.method_11657(Properties.RADIO_STATE, switched)).method_11657(Properties.LEFT_INDICATOR, false);
			world.method_8396(null, pos, class_3417.field_14962, class_3419.field_15245, 1.0F, 1.0F);
			result = class_1269.field_52422;
			this.register(world, pos, switched);
		}

		if (newState != state) {
			world.method_8652(pos, newState, 2);
		}

		return result;
	}

	private void updateLastEnabledState(class_3218 world, RadioState newRadioState) {
		if (newRadioState.isEnabled() && this.lastEnabledState != newRadioState) {
			this.lastEnabledState = newRadioState;
			world.method_8524(this.field_11867);
		}
	}

	@Nullable
	public class_2680 newBurnedState(class_3218 world, class_2338 pos, class_2680 state) {
		if (this.burningTicks > 0 && state.method_11654(Properties.RADIO_STATE) != RadioState.DESTROYED) {
			this.unregister(world, pos);
			state = (class_2680)state.method_11657(Properties.RADIO_STATE, RadioState.DESTROYED);
			class_2680 newState = this.updateState(pos, state, world, false);
			return newState != null ? newState : state;
		} else {
			return null;
		}
	}

	public void markBurning() {
		this.burningTicks = 1;
	}

	public void setLeftIndicator(boolean leftIndicator) {
		class_2680 state = this.method_10997().method_8320(this.field_11867);
		if ((Boolean)state.method_11654(Properties.LEFT_INDICATOR) != leftIndicator) {
			this.field_11863.method_8501(this.field_11867, (class_2680)state.method_11657(Properties.LEFT_INDICATOR, leftIndicator));
		}
	}

	public void updateComparators(class_3218 world, int output) {
		if (this.comparatorOutput != (this.comparatorOutput = output)) {
			world.method_8455(this.method_11016(), this.method_11010().method_26204());
		}
	}

	public void updateComparators(class_3218 world, Set<RadioListener> activeBroadcasters) {
		int output;
		if (activeBroadcasters.isEmpty()) {
			output = 0;
		} else {
			class_243 thisPos = this.method_11016().method_46558();
			double minDistSquared = Double.MAX_VALUE;

			for (RadioListener radioListener : activeBroadcasters) {
				class_243 pos = radioListener.pos;
				double distSq = thisPos.method_1025(pos);
				if (distSq < minDistSquared) {
					minDistSquared = distSq;
				}
			}

			output = class_3532.method_15340(15 - (int)(15.0 * Math.sqrt(Math.sqrt(minDistSquared) / RadioMod.CONFIG.getData().comparatorMaxDistance())), 0, 15);
		}

		this.updateComparators(world, output);
	}

	public int getComparatorOutput() {
		return Math.max(0, this.comparatorOutput);
	}

	private void unregister(class_3218 world, class_2338 pos) {
		WorldRadioManager manager = ((ServerWorldExtend)world).radio_getRadioManager();
		if (manager != null) {
			manager.unregisterRadio(this.lastChannel, pos);
		}
	}

	private void register(class_3218 world, class_2338 pos, RadioState state) {
		WorldRadioManager manager = ((ServerWorldExtend)world).radio_getRadioManager();
		if (manager != null && this.lastChannel >= 1 && this.lastChannel <= 15) {
			if (state == RadioState.BROADCAST) {
				manager.registerRadioAudioListener(this.lastChannel, pos);
			} else if (state == RadioState.LISTEN) {
				manager.registerRadioAudioPlayer(this.lastChannel, pos);
			}
		}
	}

	private static int findAntennaLength(class_1936 world, class_2338 radioPos) {
		int radioY = radioPos.method_10264();
		class_2339 mutable = radioPos.method_25503();
		int antennaCount = 0;

		for (int y = world.method_8624(class_2903.field_13203, mutable.method_10263(), mutable.method_10260()); y > radioY; y--) {
			mutable.method_33098(y);
			class_2680 state = world.method_8320(mutable);
			if (state.method_26164(class_3481.field_61207) && state.method_61767(class_2741.field_12525, class_2350.field_11036) == class_2350.field_11036) {
				antennaCount++;
			} else if (antennaCount > 0 || !isAcceptableBlockAboveAntenna(world, mutable, state)) {
				return 0;
			}
		}

		return antennaCount;
	}

	private static boolean isAcceptableBlockAboveAntenna(class_1936 world, class_2338 pos, class_2680 state) {
		return state.method_26215() || !state.method_26216();
	}

	private static RadioState getUpdatedRadioState(RadioState prevState, @Nullable RadioState lastEnabledState, int power, int antennaLen) {
		return prevState == RadioState.DESTROYED
			? RadioState.DESTROYED
			: (
				power > 0 && antennaLen > 0
					? (
						!RadioState.isAcceptAntennaLengthForBroadcast(antennaLen) || prevState != RadioState.BROADCAST && lastEnabledState != RadioState.BROADCAST
							? RadioState.LISTEN
							: RadioState.BROADCAST
					)
					: RadioState.DISABLED
			);
	}
}
