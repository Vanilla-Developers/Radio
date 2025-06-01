package com.damir00109.audio;

import com.damir00109.vpl;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.audiochannel.*;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Listener {
	private final int num;
	private final Channel channel;
	private final LocationalAudioChannel static_channel;
	private final List<short[]> packetBuffer = new ArrayList<>();
	private final BlockPos pos;
	private final VoicechatServerApi api;
	private final ServerLevel level;
	private final OpusDecoder decoder;
	private final UUID uuid;
	private AudioPlayer audioPlayer;
	private LocationalAudioChannel audioChannel;
	private final AtomicBoolean active = new AtomicBoolean(false);

	public Listener(
			int index,
			Channel channel,
			VoicechatServerApi api,
			ServerLevel level,
			BlockPos pos
	) {
		this.num = index;
		this.api = api;
		this.level = level;
		this.pos = pos;
		this.uuid = UUID.randomUUID();
		this.channel = channel;
		this.decoder = api.createDecoder();
		Position position = api.createPosition(pos.getX(), pos.getY(), pos.getZ());
		this.audioChannel = api.createLocationalAudioChannel(this.uuid, level, position);
		if (this.audioChannel != null) {
			this.audioChannel.setDistance(64F);
			this.audioChannel.setCategory(vpl.RADIO_VOLUME_CATEGORY_ID);
		} else {
			vpl.LOGGER.error("Failed to create LocationalAudioChannel for Listener at {}", pos);
		}
		this.static_channel = null;
	}

	public void setActive(boolean active1) {
		active.set(active1);
		if (!active1 && audioPlayer != null) {
			audioPlayer.stopPlaying();
		}
	}

	public boolean getActive() {
		return active.get();
	}

	private AudioPlayer getAudioPlayer() {
		if (audioPlayer == null && audioChannel != null) {
			audioPlayer = api.createAudioPlayer(audioChannel, api.createEncoder(), this::getAudio);
			if (audioPlayer == null) {
				vpl.LOGGER.error("Failed to create AudioPlayer for Listener at {}", pos);
			}
		}
		return audioPlayer;
	}

	private short[] getAudio() {
		if (!active.get() && packetBuffer.isEmpty()) {
			if (audioPlayer != null) {
				audioPlayer.stopPlaying();
			}
			return null;
		}
		short[] audio = getCombinedAudio();
		if (audio == null) {
			return null;
		}
		return audio;
	}

	public short[] getCombinedAudio() {
		short[] result = new short[960];

		if (packetBuffer.isEmpty()) {
			if (!active.get()) return null;
			return result;
		}

		int sample;
		int[] sums = new int[result.length];
		List<short[]> currentPackets = new ArrayList<>(packetBuffer);
		packetBuffer.clear();

		for (short[] audioChunk : currentPackets) {
			for (int i = 0; i < sums.length && i < audioChunk.length; i++) {
				sums[i] += audioChunk[i];
			}
		}
		for (int i = 0; i < result.length; i++) {
			sample = sums[i];
			if (sample > Short.MAX_VALUE) sample = Short.MAX_VALUE;
			if (sample < Short.MIN_VALUE) sample = Short.MIN_VALUE;
			result[i] = (short) sample;
		}
		return result;
	}

	public void sendAudio(MicrophonePacket packet) {
		if (!active.get()) return;
		
		byte[] data = packet.getOpusEncodedData();
		if (data == null || data.length == 0) return;

		short[] decoded = decoder.decode(data);
		if (decoded == null || decoded.length == 0) return;

		packetBuffer.add(decoded);

		if (getAudioPlayer() != null && !audioPlayer.isPlaying() && !packetBuffer.isEmpty()) {
			audioPlayer.startPlaying();
		}
	}

	public int getNum() {
		return num;
	}

	public Channel getChannel() {
		return channel;
	}

	public void destroy() {
		active.set(false);
		if (audioPlayer != null) {
			audioPlayer.stopPlaying();
			audioPlayer = null;
		}
		if (audioChannel != null) {
			audioChannel = null;
		}
		if (decoder != null) {
			decoder.close();
		}
		packetBuffer.clear();
	}
}
