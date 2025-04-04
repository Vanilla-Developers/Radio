package com.damir00109;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiosender.AudioSender;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

public class RadioSender implements AudioSender {
	private RadioChannel channel;

	public RadioSender(RadioChannel channel, VoicechatServerApi api, ServerLevel level, int x, int y, int z) {
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
		for (int i = 0; i < this.channel.getListeners().size(); i++) {
			RadioListener listener = this.channel.getListeners().get(i);
			listener.playAudio(audio);
		}
		return true;
	}
}
