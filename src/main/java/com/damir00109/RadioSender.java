package com.damir00109;

import de.maxhenkel.voicechat.api.audiosender.AudioSender;

public class RadioSender implements AudioSender {
	private RadioChannel channel;

	public RadioSender(RadioChannel channel) {
		this.channel = channel;
	}


	@Override
	public AudioSender whispering(boolean b) {
		return null;
	}

	@Override
	public boolean isWhispering() {
		return false;
	}

	@Override
	public AudioSender sequenceNumber(long l) {
		return null;
	}

	@Override
	public boolean canSend() {
		return false;
	}

	@Override
	public boolean send(byte[] bytes) {
		return false;
	}

	@Override
	public boolean reset() {
		return false;
	}
}
