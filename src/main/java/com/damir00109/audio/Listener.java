package com.damir00109.audio;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.audiochannel.*;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.util.math.BlockPos;

import java.util.*;

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
	private boolean active;

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
	public void setActive(boolean active1) {active = active1;}
	public boolean getActive() {return active;}

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
		if (audio == null) {
			audioPlayer.stopPlaying();
			audioPlayer = null;
			return null;
		}
		return audio;
	}

	public short[] getCombinedAudio() {
		short[] result = new short[960];

		if (packetBuffer.isEmpty()) {
			return result;
		}

		int sample;
		int[] sums = new int[result.length];
		List<short[]> uniquePackets = new ArrayList<>(new HashSet<>(packetBuffer));
		for (short[] audio : uniquePackets) {
			for (int i = 0; i < sums.length; i++) {
				sums[i] += audio[i];
			}
		}
		for (int i = 0; i < result.length; i++) {
			sample = sums[i];
			result[i] = (short) sample;
		}
		packetBuffer.clear();
		//VanillaDamir00109.LOGGER.info(Arrays.toString(result));
		return result;
	}

	public void sendAudio(MicrophonePacket packet) {
		if (!active) return;
		byte[] data = packet.getOpusEncodedData();
		short[] decoded = decoder.decode(data);
		if (packetBuffer.size() > 960) packetBuffer.clear();
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
