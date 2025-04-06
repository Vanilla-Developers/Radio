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
	private final int x;
	private final int y;
	private final int z;

	public RadioChannel(int id, VoicechatServerApi api, ServerLevel level, int x, int y, int z) {
		this.channelNum = id;
		this.api = api;
		this.level = level;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public ArrayList<RadioSender> getSenders() {
		return this.senders;
	}
	public ArrayList<RadioListener> getListeners() {
		return this.listeners;
	}

	public RadioSender newSender() {
		int index = lastSenderIndex;
		RadioSender sender = new RadioSender(index, this, this.api, this.level, this.x, this.y, this.z);
		this.senders.add(sender);
		lastSenderIndex += 1;
		return sender;
	}
	public RadioListener newListener() {
		int index = lastListenerIndex;
		RadioListener listener = new RadioListener(index, this, this.api, this.level, this.x, this.y, this.z);
		this.listeners.add(listener);
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
		for (int i = 0; i < this.getListeners().size(); i++) {
			RadioListener listener = this.getListener(i);
			listener.playAudio(audio);
		}
	}
}
