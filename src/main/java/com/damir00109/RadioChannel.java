package com.damir00109;

import java.util.ArrayList;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

public class RadioChannel {
	private ServerLevel level;
	private int channelNum;
	private ArrayList<RadioSender> senders;
	private int lastSenderIndex = 0;
	private ArrayList<RadioListener> listeners;
	private int lastListenerIndex = 0;
	private VoicechatServerApi api;

	public RadioChannel(int id, VoicechatServerApi api) {
		channelNum = id;
		senders = new ArrayList<RadioSender>();
		listeners = new ArrayList<RadioListener>();
		this.api = api;
	}

	public ArrayList<RadioSender> getSenders() {
		return this.senders;
	}
	public ArrayList<RadioListener> getListeners() {
		return this.listeners;
	}

	public RadioSender newSender(ServerLevel level, int x, int y, int z) {
		int index = lastSenderIndex;
		RadioSender sender = new RadioSender(index, this, api, level, x, y, z);
		senders.add(sender);
		lastSenderIndex += 1;
		return sender;
	}
	public RadioListener newListener(ServerLevel level, int x, int y, int z) {
		int index = lastListenerIndex;
		RadioListener listener = new RadioListener(index, this, api, level, x, y, z);
		listeners.add(listener);
		lastListenerIndex += 1;
		return  listener;
	}

	public int getId() {
		return this.channelNum;
	}

	public RadioSender getSender(int index) {
		return this.senders.get(index);
	}
	public RadioListener getListener(int index) {
		return this.listeners.get(index);
	}

	public void broadcast(byte[] audio) {
		VanillaDamir00109.LOGGER.info("broadcasting for byte["+audio.length+"]");
		for (int i = 0; i < this.getListeners().size(); i++) {
			this.getListener(i).playAudio(audio);
		}
	}
}
