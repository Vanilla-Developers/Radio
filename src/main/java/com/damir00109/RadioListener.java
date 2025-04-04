package com.damir00109;

import de.maxhenkel.voicechat.api.audiolistener.AudioListener;

import java.util.UUID;

public class RadioListener implements AudioListener {
	private RadioChannel channel;

	public RadioListener(RadioChannel channel) {
		this.channel = channel;
	}

	@Override
	public UUID getListenerId() {
		return null;
	}
}
