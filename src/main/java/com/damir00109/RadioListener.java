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
	private final UUID uuid;

	public RadioListener(int index, RadioChannel channel, VoicechatServerApi api, ServerLevel level, int x, int y, int z) {
		VanillaDamir00109.LOGGER.info("Created Listener for index {}", index);
		uuid = UUID.randomUUID();
		num = index;
		this.channel = channel;
		Position position = api.createPosition(x,y,z);
		static_channel = api.createLocationalAudioChannel(UUID.randomUUID(), level, position);
		assert static_channel != null;
		static_channel.setDistance(15);
		static_channel.setCategory("Radio-"+index);
	}
	public RadioChannel getChannel() {
		return this.channel;
	}

	public void playAudio(byte[] audio) {
		static_channel.send(audio);
	}

	public int getIndex() {
		return num;
	}

	@Override
	public UUID getListenerId() {
		return uuid;
	}
}
