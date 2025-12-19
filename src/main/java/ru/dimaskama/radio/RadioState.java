package ru.dimaskama.radio;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.class_3542;
import net.minecraft.class_9135;
import net.minecraft.class_9139;

public enum RadioState implements class_3542 {
	DISABLED("disabled"),
	LISTEN("listen"),
	BROADCAST("broadcast"),
	DESTROYED("destroyed");

	public static final Codec<RadioState> CODEC = class_3542.method_53955(RadioState::values);
	public static final class_9139<ByteBuf, RadioState> PACKET_CODEC = class_9139.method_56434(class_9135.field_48550, Enum::ordinal, i -> values()[i]);
	private final String key;

	private RadioState(String key) {
		this.key = key;
	}

	public String method_15434() {
		return this.key;
	}

	public RadioState getSwitched(int antennaLength) {
		return this == DISABLED
			? DISABLED
			: (this == DESTROYED ? DESTROYED : (this == LISTEN && isAcceptAntennaLengthForBroadcast(antennaLength) ? BROADCAST : LISTEN));
	}

	public boolean isEnabled() {
		return this == LISTEN || this == BROADCAST;
	}

	public static boolean isAcceptAntennaLengthForBroadcast(int len) {
		return len >= 5;
	}
}
