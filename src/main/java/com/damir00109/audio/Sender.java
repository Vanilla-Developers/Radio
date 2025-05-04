package com.damir00109.audio;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.util.math.BlockPos;

public class Sender {
	private final Channel channel;
	private int num;
	private boolean active;

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
	public void setActive(boolean active1) {active = active1;}
	public boolean getActive() {return active;}

	public void send(MicrophonePacket packet) {
		if (!active) return;
		if (packet.isWhispering()) return;
		//VanillaDamir00109.LOGGER.info("Sender {} active={}", num, active);
		channel.broadcast(packet);
	}

	public int getNum() {
		return num;
	}
}
