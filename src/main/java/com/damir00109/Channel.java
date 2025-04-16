package com.damir00109;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public class Channel {
	private final VoicechatServerApi api;
	private final int num;
	private ArrayList<Listener> listeners;
	private ArrayList<Sender> senders;
	private int lastListenerIndex;
	private int lastSenderIndex;

	public Channel(int index, VoicechatServerApi api) {
		this.api = api;
		this.num = index;
	}

	public Listener getOrCreateListener(ServerLevel level, BlockPos pos) {
		return getOrCreateListener(-1, level, pos);
	}
	public Listener getOrCreateListener(int index) {
		return getOrCreateListener(index, null, null);
	}

	public Listener getOrCreateListener(int index, ServerLevel level, BlockPos pos) {
		Listener listener;
		if (index == -1) {
			listener = newListener(level, pos);
		} else {
			listener = getListener(index);
		}
		return listener;
	}

	private Sender getSender(int index) {
		return senders.get(index);
	}

	private Sender newSender(ServerLevel level, BlockPos pos) {
		Sender sender = new Sender(lastSenderIndex+1, this, api, level, pos);
		senders.add(sender);
		lastSenderIndex += 1;
		return sender;
	}
	public Sender getOrCreateSender(ServerLevel level, BlockPos pos) {
		return getOrCreateSender(-1, level, pos);
	}
	public Sender getOrCreateSender(int index) {
		return getOrCreateSender(index, null, null);
	}

	public Sender getOrCreateSender(int index, ServerLevel level, BlockPos pos) {
		Sender sender;
		if (index == -1) {
			sender = newSender(level, pos);
		} else {
			sender = getSender(index);
		}
		return sender;
	}

	private Listener getListener(int index) {
		return listeners.get(index);
	}

	private Listener newListener(ServerLevel level, BlockPos pos) {
		Listener listener = new Listener(lastListenerIndex+1, this, api, level, pos);
		listeners.add(listener);
		lastListenerIndex += 1;
		return listener;
	}

	public void broadcast(MicrophonePacket packet) {
		for (Listener listener : listeners) {
			listener.sendAudio(packet);
		}
	}

	public int getIndex() {
		return num;
	}
}
