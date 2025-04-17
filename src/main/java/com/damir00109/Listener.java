package com.damir00109;

import com.fasterxml.jackson.databind.JsonNode;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

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
		uuid = UUID.randomUUID();
		this.channel = channel;
		this.decoder = api.createDecoder();
		Position position = api.createPosition(pos.getX(), pos.getY(), pos.getZ());
		static_channel = api.createLocationalAudioChannel(UUID.randomUUID(), level, position);
		assert static_channel != null;
		static_channel.setDistance(15);
		static_channel.setCategory("speakers");
	}

	private AudioPlayer getAudioPlayer() {
		if (audioPlayer == null) {
			VoicechatConnection connection = api.getConnectionOf(uuid);
			if (connection == null) { // Speaker
				LocationalAudioChannel locationalAudioChannel = api.createLocationalAudioChannel(uuid,
						level,
						api.createPosition(pos.getX(), pos.getY(), pos.getZ()));
				assert locationalAudioChannel != null;
				locationalAudioChannel.setDistance(15);
				audioChannel = locationalAudioChannel;
				audioChannel.setCategory("speakers");
			} else { // Player
				audioChannel = (LocationalAudioChannel) api.createEntityAudioChannel(uuid, connection.getPlayer());
			}

			audioPlayer = api.createAudioPlayer(audioChannel, api.createEncoder(), this::getAudio);
		}

		return audioPlayer;
	}

	private short[] getAudio() {
		short[] audio = getCombinedAudio();
		int volume;
		if (audio == null) {
			volume = 0;
			audioPlayer.stopPlaying();
			audioPlayer = null;
			return null;
		}
		volume = getVolume(audio);
		return audio;
	}

	private short getVolume(short[] audio) {
		short max = 0;
		for (short num : audio) {
			if (num > max) {
				max = num; // Mise à jour du maximum
			}
		}
		return max;
	}

	public short[] getCombinedAudio() {
		if (packetBuffer.isEmpty()) {
			return null;
		}

		short[] result = new short[960];
		int sample;
		for (int i = 0; i < result.length; i++) {
			sample = 0;
			for (short[] audio : new HashSet<>(packetBuffer)) {
				sample += audio[i];
			}
			if (sample > Short.MAX_VALUE) {
				result[i] = Short.MAX_VALUE;
			} else if (sample < Short.MIN_VALUE) {
				result[i] = Short.MIN_VALUE;
			} else {
				result[i] = (short) sample;
			}
		}
		packetBuffer.clear();
		return result;
	}

	public void sendAudio(MicrophonePacket packet) {
		byte[] data = packet.getOpusEncodedData();
		short[] decoded = decoder.decode(data);
		if (packetBuffer.size() > 10000) packetBuffer.clear();
		packetBuffer.add(decoded);

		if (getAudioPlayer() != null)
			audioPlayer.startPlaying();
	}

	public int getNum() {
		return num;
	}

	public Channel getChannel() {
		return channel;
	}
}
