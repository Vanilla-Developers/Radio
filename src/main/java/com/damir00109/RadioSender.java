package com.damir00109;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiosender.AudioSender;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

public class RadioSender implements AudioSender {
	private int num;
	private RadioChannel channel;

	public RadioSender(int index, RadioChannel channel, VoicechatServerApi api, ServerLevel level, int x, int y, int z) {
		this.num = index;
		this.channel = channel;
	}
	public RadioChannel getChannel() {
		return this.channel;
	}


	@Override
	public AudioSender whispering(boolean b) { return null; }
	@Override
	public boolean isWhispering() { return false; }
	@Override
	public AudioSender sequenceNumber(long l) { return null; }
	@Override
	public boolean canSend() { return true; }
	@Override
	public boolean reset() { return false; }

	@Override
	public boolean send(byte[] audio) {
		channel.broadcast(audio);
		return true;
	}

	public int getIndex() {
		return num;
	}
}
