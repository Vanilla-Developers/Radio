package ru.dimaskama.radio;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.random.RandomSource;

public class RadioAudioEffect {
	private static final float SAMPLE_RATE = 48000.0F;
	private static final int FRAME_SIZE = 960;
	private static final float MAX_SHORT = 32767.0F;
	private static final float LN_2 = (float)Math.log(2.0);
	private final RandomSource random = RandomSource.create();
	private final float normalizedCenterFrequency;
	private final float normalizedBandwidth;
	private final float severity;
	private final float[] floatBuffer = new float[FRAME_SIZE];
	private float lastInputSample1;
	private float lastInputSample2;
	private float lastOutputSample1;
	private float lastOutputSample2;

	public RadioAudioEffect(float centerFrequency, float bandwidth, float severity) {
		this.normalizedCenterFrequency = 2.0F * centerFrequency / SAMPLE_RATE;
		this.normalizedBandwidth = 2.0F * bandwidth / SAMPLE_RATE;
		this.severity = severity;
	}

	public RadioAudioEffect() {
		this(750.0F, 4000.0F, 0.05F);
	}

	public void apply(short[] data) {
		if (data.length == FRAME_SIZE) {
			for (int i = 0; i < FRAME_SIZE; i++) {
				if (this.random.nextFloat() < this.severity) {
					data[i] = 0;
				}
			}

			float[] floats = this.floatBuffer;

			for (int ix = 0; ix < FRAME_SIZE; ix++) {
				floats[ix] = data[ix] / MAX_SHORT;
			}

			float maxValue = MAX_SHORT;

			for (int ix = 0; ix < FRAME_SIZE; ix++) {
				float sample = floats[ix] = this.bandpassFilter(floats[ix]) * MAX_SHORT;
				float abs = Math.abs(sample);
				if (abs > maxValue) {
					maxValue = abs;
				}
			}

			float factor = MAX_SHORT / maxValue;

			for (int ixx = 0; ixx < FRAME_SIZE; ixx++) {
				data[ixx] = (short)(floats[ixx] * factor);
			}
		}
	}

	private float bandpassFilter(float inputSample) {
		float bandwidth = this.normalizedBandwidth * (1.0F - this.severity * 0.1F);
		float w0 = (float) (Math.PI * 2) * this.normalizedCenterFrequency;
		float sin = MathHelper.sin(w0);
		float alpha = sin * (float)Math.sinh(LN_2 * 0.5F * bandwidth * w0 / sin);
		float a0 = 1.0F + alpha;
		float cos = MathHelper.cos(w0);
		float b0 = (1.0F - cos) * 0.5F;
		float b1 = 1.0F - cos;
		float a1 = -2.0F * cos;
		float a2 = 1.0F - alpha;
		float filteredSample = (
				b0 * inputSample + b1 * this.lastInputSample1 + b0 * this.lastInputSample2 - a1 * this.lastOutputSample1 - a2 * this.lastOutputSample2
			)
			/ a0;
		this.lastInputSample2 = this.lastInputSample1;
		this.lastInputSample1 = inputSample;
		this.lastOutputSample2 = this.lastOutputSample1;
		this.lastOutputSample1 = filteredSample;
		return filteredSample;
	}
}
