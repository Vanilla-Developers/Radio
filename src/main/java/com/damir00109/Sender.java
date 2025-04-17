package com.damir00109;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.util.math.BlockPos;

public class Sender {
	private final Channel channel;
	private int num;

	public Sender(
			int index,
			Channel channel,
			VoicechatServerApi api,
			ServerLevel level,
			BlockPos pos
	) {
		this.channel = channel;
		num = index;
	}
	public void send(MicrophonePacket packet) {
		//VanillaDamir00109.LOGGER.info("Sender {} active={}", num, active);
		channel.broadcast(packet);
	}

	public int getNum() {
		return num;
	}
}
