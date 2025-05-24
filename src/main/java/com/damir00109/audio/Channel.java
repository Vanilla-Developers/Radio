package com.damir00109.audio;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class Channel {
	private final VoicechatServerApi api;
	private final int num;
	public static final HashMap<BlockPos, Listener> listeners = new HashMap<>();
	public static final HashMap<BlockPos, Sender> senders = new HashMap<>();
	private static final ExecutorService executor = Executors.newCachedThreadPool();
	private final List<BlockPos> radios = new CopyOnWriteArrayList<>();

	public Channel(int index, VoicechatServerApi api) {
		this.api = api;
		this.num = index;
	}

	public Listener getOrCreateListener(ServerLevel level, BlockPos pos) {
		Listener listener = listeners.get(pos);
		if (listener == null) {
			listener = newListener(level, pos);
		}
		return listener;
	}

	public Sender getSender(BlockPos pos) {
		return senders.get(pos);
	}

	private Sender newSender(ServerLevel level, BlockPos pos) {
		Sender sender = new Sender(senders.size(), this, api, level, pos);
		senders.put(pos, sender);
		return sender;
	}

	public Sender getOrCreateSender(ServerLevel level, BlockPos pos) {
		Sender sender = senders.get(pos);
		if (sender == null) {
			sender = newSender(level, pos);
		}
		return sender;
	}
	public Listener getListener(BlockPos pos) {
		return listeners.get(pos);
	}

	private Listener newListener(ServerLevel level, BlockPos pos) {
		Listener listener = new Listener(listeners.size(), this, api, level, pos);
		listeners.put(pos, listener);
		return listener;
	}

	public void broadcast(MicrophonePacket packet) {
		listeners.forEach((pos, listener) -> {
			executor.submit(() -> {
				listener.sendAudio(packet);
			});
		});
	}

	public int getIndex() {
		return num;
	}

	public List<BlockPos> getRadios() {
		return radios;
	}

	public void removeRadio(BlockPos pos) {
		radios.remove(pos);
	}

	public void broadcast(MicrophonePacket packet, @Nullable VoicechatConnection fromConnection, @Nullable BlockPos radioPos) {
		// ... existing code ...
	}
}
