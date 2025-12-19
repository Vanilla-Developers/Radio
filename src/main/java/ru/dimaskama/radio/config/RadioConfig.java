package ru.dimaskama.radio.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RadioConfig(double recordMaxDist, double whisperingRecordMaxDist, double comparatorMaxDistance, float playerSoundDistance) {
	public static final Codec<RadioConfig> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				JsonConfig.defaultedField(Codec.DOUBLE, "radio_record_max_dist", () -> 10.0).forGetter(RadioConfig::recordMaxDist),
				JsonConfig.defaultedField(Codec.DOUBLE, "radio_whispering_record_max_dist", () -> 4.0).forGetter(RadioConfig::whisperingRecordMaxDist),
				JsonConfig.defaultedField(Codec.DOUBLE, "radio_comparator_max_distance", () -> 4000.0).forGetter(RadioConfig::comparatorMaxDistance),
				JsonConfig.defaultedField(Codec.FLOAT, "radio_player_sound_distance", () -> 32.0F).forGetter(RadioConfig::playerSoundDistance)
			)
			.apply(instance, RadioConfig::new)
	);

	public RadioConfig() {
		this(10.0, 4.0, 4000.0, 32.0F);
	}
}
