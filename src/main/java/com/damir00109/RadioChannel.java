package com.damir00109;

import java.util.ArrayList;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

public class RadioChannel {
	private ServerLevel level;
	private int channelNum;
	private ArrayList<RadioSender> senders;
	private ArrayList<RadioListener> listeners;
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
		RadioSender sender = new RadioSender(this, this.api, this.level, this.x, this.y, this.z);
		this.senders.add(sender);
		return sender;
	}
	public RadioListener newListener() {
		RadioListener listener = new RadioListener(this, this.api, this.level, this.x, this.y, this.z);
		this.listeners.add(listener);
		return  listener;
	}

	public int getId() {
		return this.channelNum;
	}
}
