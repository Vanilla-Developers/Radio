package com.damir00109;

import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.audiolistener.AudioListener;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.Position;

import java.util.UUID;

public class RadioListener implements AudioListener, Runnable {
	private final int num;
	private final LocationalAudioChannel static_channel;
	private final UUID uuid;
	private boolean active = true;

	public RadioListener(int index, VoicechatServerApi api, ServerLevel level, int x, int y, int z) {
		VanillaDamir00109.LOGGER.debug("Created Listener for index {}", index);
		uuid = UUID.randomUUID();
		num = index;
		Position position = api.createPosition(x,y,z);
		static_channel = api.createLocationalAudioChannel(uuid, level, position);
		static_channel.setDistance(15);
		static_channel.setCategory("Radio-"+index);
	}

	public void playAudio(byte[] audio) {
		VanillaDamir00109.LOGGER.debug("Listener {} active={}", num, active);
		if (isActive()) static_channel.send(audio);
	}

	public void setActive(boolean bool) { active = bool; }
	public boolean isActive() { return active; }

	@Override
	public UUID getListenerId() {
		return uuid;
	}

	@Override
	public void run() {}
}
