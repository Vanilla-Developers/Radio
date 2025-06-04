package com.damir00109.audio;

import com.damir00109.vpl;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.audiochannel.*;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Listener {
	private final int num;
	private final Channel channel;
	private final LocationalAudioChannel static_channel;
	private final Queue<short[]> packetBuffer = new ConcurrentLinkedQueue<>();
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

	public void setActive(boolean activeState) {
		boolean oldActive = active.getAndSet(activeState);
		vpl.LOGGER.info("[Listener {} at {}] setActive: old={}, new={}", num, pos, oldActive, activeState);
		if (!activeState && oldActive) { // Becoming inactive
			if (audioPlayer != null) {
				vpl.LOGGER.info("[Listener {} at {}] setActive: Stopping player due to becoming inactive.", num, pos);
				audioPlayer.stopPlaying();
			}
			vpl.LOGGER.info("[Listener {} at {}] setActive: Clearing packet buffer due to becoming inactive. Size before: {}", num, pos, packetBuffer.size());
			packetBuffer.clear();
		}
		// If becoming active, sendAudio will handle starting the player when data arrives.
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
		// vpl.LOGGER.info("[Listener {} at {}] getAudio called.", num, pos); // Can be too spammy
		short[] audioFrame = packetBuffer.poll();

		if (audioFrame != null) {
			vpl.LOGGER.debug("[Listener {} at {}] getAudio: Returning audio frame. Buffer size after poll: {}", num, pos, packetBuffer.size());
			return audioFrame;
		} else {
			// Buffer is empty.
			vpl.LOGGER.debug("[Listener {} at {}] getAudio: Buffer empty.", num, pos);
			if (audioPlayer != null && audioPlayer.isPlaying()) {
				vpl.LOGGER.info("[Listener {} at {}] getAudio: Buffer empty, stopping player.", num, pos);
				audioPlayer.stopPlaying();
			}
			return null;
		}
	}

	public void sendAudio(MicrophonePacket packet) {
		if (!active.get()) {
			// vpl.LOGGER.warn("[Listener {} at {}] sendAudio: Called while listener inactive. Discarding.", num, pos); // Can be spammy if radio toggled often
			return;
		}
		vpl.LOGGER.info("[Listener {} at {}] sendAudio: Listener active, processing packet.", num, pos);
		
		byte[] data = packet.getOpusEncodedData();
		if (data == null || data.length == 0) {
			vpl.LOGGER.warn("[Listener {} at {}] sendAudio: Packet data is null or empty. Discarding.", num, pos);
			return;
		}

		short[] decoded = decoder.decode(data);
		if (decoded == null || decoded.length == 0) {
			vpl.LOGGER.warn("[Listener {} at {}] sendAudio: Decoder returned null or empty audio. Discarding.", num, pos);
			return;
		}

		vpl.LOGGER.debug("[Listener {} at {}] sendAudio: Decoded audio successfully. Offering to buffer. Buffer size before: {}", num, pos, packetBuffer.size());
		packetBuffer.offer(decoded);
		vpl.LOGGER.debug("[Listener {} at {}] sendAudio: Offered to buffer. Buffer size after: {}", num, pos, packetBuffer.size());

		AudioPlayer player = getAudioPlayer(); 
		if (player != null) {
			if (!player.isPlaying()) {
				vpl.LOGGER.info("[Listener {} at {}] sendAudio: Player was not playing. Buffer not empty (size {}). Starting player.", num, pos, packetBuffer.size());
				player.startPlaying();
			} else {
				// vpl.LOGGER.debug("[Listener {} at {}] sendAudio: Player already playing. Buffer size: {}", num, pos, packetBuffer.size());
			}
		} else {
			vpl.LOGGER.error("[Listener {} at {}] sendAudio: getAudioPlayer() returned null! Cannot start playback.", num, pos);
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
