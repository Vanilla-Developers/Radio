package com.damir00109;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class RadioListener {
	public static final int AMPLITUDES_QUEUE_SIZE = 40;
	public final AtomicInteger idleTicks = new AtomicInteger();
	public final IntArrayList amplitudes = new IntArrayList();
	public final BlockPos blockPos;
	public final Vec3d pos;
	public int comparatorOutput;

	public RadioListener(BlockPos blockPos) {
		this.blockPos = blockPos;
		this.pos = blockPos.toCenterPos();
	}

	public void writeAmplitude(short[] audio) {
		short min = 0;
		short max = 0;

		for (short sample : audio) {
			if (sample > max) {
				max = sample;
			} else if (sample < min) {
				min = sample;
			}
		}

		int ampl = encodeAmplitude(min, max);
		synchronized (this.amplitudes) {
			this.amplitudes.add(ampl);
		}
	}

	public static int encodeAmplitude(short min, short max) {
		return (min & 0xFFFF) << 16 | (max & 0xFFFF);
	}

	public static short decodeMin(int minMax) {
		return (short)(minMax >>> 16);
	}

	public static short decodeMax(int minMax) {
		return (short)(minMax & 0xFFFF);
	}
}
