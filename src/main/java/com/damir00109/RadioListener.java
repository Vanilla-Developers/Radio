package com.damir00109;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.audiolistener.AudioListener;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

import java.util.UUID;

public class RadioListener implements AudioListener {
	private final int num;
	private final RadioChannel channel;
	private final LocationalAudioChannel static_channel;

	public RadioListener(int index, RadioChannel channel, VoicechatServerApi api, ServerLevel level, int x, int y, int z) {
		this.num = index;
		this.channel = channel;
		Position position = api.createPosition(x,y,z);
		this.static_channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(level), position);
		assert this.static_channel != null;
		this.static_channel.setDistance(15);
	}
	public RadioChannel getChannel() {
		return this.channel;
	}

	public void playAudio(byte[] audio) {
		this.static_channel.send(audio);
	}

	@Override
	public UUID getListenerId() {
		return null;
	}

	public int getIndex() {
		return num;
	}
}
