package com.damir00109;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.audiolistener.AudioListener;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

import java.util.UUID;

public class RadioListener implements AudioListener {
	private RadioChannel channel;
	private LocationalAudioChannel static_channel;

	public RadioListener(RadioChannel channel, VoicechatServerApi api, ServerLevel level, int x, int y, int z) {
		this.channel = channel;
		Position position = api.createPosition(x,y,z);
		this.static_channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(level), position);
	}
	public RadioChannel getChannel() {
		return this.channel;
	}

	public void playAudio(byte[] audio) {
		this.static_channel.setDistance(15);
		this.static_channel.send(audio);
	}

	@Override
	public UUID getListenerId() {
		return null;
	}
}
