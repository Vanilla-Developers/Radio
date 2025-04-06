package com.damir00109;

import java.util.ArrayList;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

public class RadioChannel {
	private int channelNum;
	private ArrayList<RadioSender> senders;
	private int lastSenderIndex = -1;
	private ArrayList<RadioListener> listeners;
	private int lastListenerIndex = -1;
	private VoicechatServerApi api;

	public RadioChannel(int id, VoicechatServerApi api) {
		channelNum = id;
		senders = new ArrayList<>();
		listeners = new ArrayList<>();
		this.api = api;
	}

	public ArrayList<RadioSender> getSenders() {
		return this.senders;
	}
	public ArrayList<RadioListener> getListeners() {
		return this.listeners;
	}

	public RadioSender newSenderWith(int index, ServerLevel level, int x, int y, int z) {
		RadioSender sender = new RadioSender(index, this, api, level, x, y, z);
		senders.add(index, sender);
		return sender;
	}
	public RadioListener newListenerWith(int index, ServerLevel level, int x, int y, int z) {
		RadioListener listener = new RadioListener(index, this, api, level, x, y, z);
		listeners.add(index, listener);
		return listener;
	}

	public RadioSender newSender(ServerLevel level, int x, int y, int z) {
		int index = lastSenderIndex+1;
		RadioSender sender = newSenderWith(index, level, x, y, z);
		lastSenderIndex = index;
		return sender;
	}
	public RadioListener newListener(ServerLevel level, int x, int y, int z) {
		int index = lastListenerIndex+1;
		RadioListener listener = newListenerWith(index, level, x, y, z);
		lastListenerIndex = index;
		return listener;
	}

	public RadioSender getSender(int index) {
		if (index >= senders.size()) return null;
		return this.senders.get(index);
	}
	public RadioListener getListener(int index) {
		if (index >= listeners.size()) return null;
		return this.listeners.get(index);
	}

	public void broadcast(byte[] audio) {
		VanillaDamir00109.LOGGER.info("broadcasting for byte["+audio.length+"]");
		for (int i = 0; i < this.getListeners().size(); i++) {
			this.getListener(i).playAudio(audio);
		}
	}
}
