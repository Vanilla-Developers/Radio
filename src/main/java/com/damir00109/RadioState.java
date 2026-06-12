package com.damir00109;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum RadioState implements StringRepresentable {
	DISABLED("disabled"),
	LISTEN("listen"),
	BROADCAST("broadcast"),
	DESTROYED("destroyed");

	public static final Codec<RadioState> CODEC = StringRepresentable.fromEnum(RadioState::values);
	public static final StreamCodec<ByteBuf, RadioState> PACKET_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Enum::ordinal, i -> values()[i]);

	private final String key;

	private RadioState(String key) {
		this.key = key;
	}

	@Override
	public String getSerializedName() {
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
	public static void write(FriendlyByteBuf buf, RadioState state) {
		buf.writeEnum(state);
	}

	public static RadioState read(FriendlyByteBuf buf) {
		return buf.readEnum(RadioState.class);
	}
}