package ru.dimaskama.radio;

import net.minecraft.class_3532;
import net.minecraft.class_5819;

public class RadioAudioEffect {
	private static final float SAMPLE_RATE = 48000.0F;
	private static final int FRAME_SIZE = 960;
	private static final float MAX_SHORT = 32767.0F;
	private static final float LN_2 = (float)Math.log(2.0);
	private final class_5819 random = class_5819.method_43053();
	private final float normalizedCenterFrequency;
	private final float normalizedBandwidth;
	private final float severity;
	private final float[] floatBuffer = new float[960];
	private float lastInputSample1;
	private float lastInputSample2;
	private float lastOutputSample1;
	private float lastOutputSample2;

	public RadioAudioEffect(float centerFrequency, float bandwidth, float severity) {
		this.normalizedCenterFrequency = 2.0F * centerFrequency / 48000.0F;
		this.normalizedBandwidth = 2.0F * bandwidth / 48000.0F;
		this.severity = severity;
	}

	public RadioAudioEffect() {
		this(750.0F, 4000.0F, 0.05F);
	}

	public void apply(short[] data) {
		if (data.length == 960) {
			for (int i = 0; i < 960; i++) {
				if (this.random.method_43057() < this.severity) {
					data[i] = 0;
				}
			}

			float[] floats = this.floatBuffer;

			for (int ix = 0; ix < 960; ix++) {
				floats[ix] = data[ix] / 32767.0F;
			}

			float maxValue = 32767.0F;

			for (int ix = 0; ix < 960; ix++) {
				float sample = floats[ix] = this.bandpassFilter(floats[ix]) * 32767.0F;
				float abs = Math.abs(sample);
				if (abs > maxValue) {
					maxValue = abs;
				}
			}

			float factor = 32767.0F / maxValue;

			for (int ixx = 0; ixx < 960; ixx++) {
				data[ixx] = (short)(floats[ixx] * factor);
			}
		}
	}

	private float bandpassFilter(float inputSample) {
		float bandwidth = this.normalizedBandwidth * (1.0F - this.severity * 0.1F);
		float w0 = (float) (Math.PI * 2) * this.normalizedCenterFrequency;
		float sin = class_3532.method_15374(w0);
		float alpha = sin * (float)Math.sinh(LN_2 * 0.5F * bandwidth * w0 / sin);
		float a0 = 1.0F + alpha;
		float cos = class_3532.method_15362(w0);
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
