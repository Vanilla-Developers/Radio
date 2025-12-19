package ru.dimaskama.radio;

import com.mojang.serialization.Codec;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.StringIdentifiable;

public enum RadioState implements StringIdentifiable {
	DISABLED("disabled"),
	LISTEN("listen"),
	BROADCAST("broadcast"),
	DESTROYED("destroyed");

	public static final Codec<RadioState> CODEC = StringIdentifiable.createCodec(RadioState::values);

	private final String key;

	private RadioState(String key) {
		this.key = key;
	}

	@Override
	public String asString() {
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

	/* Network helpers (Fabric / PacketByteBuf) for Minecraft 1.21.9:
	   Replaced the obfuscated/unknown PACKET_CODEC with explicit read/write helpers.
	   Use RadioState.write(buf, state) and RadioState.read(buf) when serializing over PacketByteBuf.
	*/
	public static void write(PacketByteBuf buf, RadioState state) {
		buf.writeEnumConstant(state);
	}

	public static RadioState read(PacketByteBuf buf) {
		return buf.readEnumConstant(RadioState.class);
	}
}