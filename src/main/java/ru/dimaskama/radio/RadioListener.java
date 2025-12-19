package ru.dimaskama.radio;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.class_2338;
import net.minecraft.class_243;

public class RadioListener {
	public static final int AMPLITUDES_QUEUE_SIZE = 40;
	public final AtomicInteger idleTicks = new AtomicInteger();
	public final IntArrayList amplitudes = new IntArrayList();
	public final class_2338 blockPos;
	public final class_243 pos;
	public int comparatorOutput;

	public RadioListener(class_2338 blockPos) {
		this.blockPos = blockPos;
		this.pos = blockPos.method_46558();
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
		return min << 16 | max;
	}

	public static short decodeMin(int minMax) {
		return (short)(minMax >>> 16);
	}

	public static short decodeMax(int minMax) {
		return (short)(minMax & 65535);
	}
}
