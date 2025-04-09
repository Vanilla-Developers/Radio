package com.damir00109;

import de.maxhenkel.voicechat.api.audiosender.AudioSender;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.ServerLevel;

public class RadioSender implements AudioSender, Runnable {
	private final int num;
	private boolean active = true;

	public RadioSender(int index, VoicechatServerApi api, ServerLevel level, int x, int y, int z) {
		VanillaDamir00109.LOGGER.debug("Created Sender for index {}", index);
		num = index;
	}

	@Override
	public AudioSender whispering(boolean b) { return this; }
	@Override
	public boolean isWhispering() { return false; }
	@Override
	public AudioSender sequenceNumber(long l) { return this; }
	@Override
	public boolean canSend() { return true; }
	@Override
	public boolean reset() { return false; }

	public void setActive(boolean bool) { active = bool; }
	public boolean isActive() { return active; }

	@Override
	public boolean send(byte[] audio) {
		VanillaDamir00109.LOGGER.debug("Sender {} active={}", num, active);
		if (!isActive()) return false;
		//channel.broadcast(audio);
		return true;
	}

	@Override
	public void run() {}
}
