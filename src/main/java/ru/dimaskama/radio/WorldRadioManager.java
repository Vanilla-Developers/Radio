package ru.dimaskama.radio;

import com.google.common.collect.Sets;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3532;

public class WorldRadioManager implements AutoCloseable {
	private final class_3218 world;
	private final Int2ObjectMap<RadioChannel> channels = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap());
	private final Set<UUID> radioAudioChannels = Sets.newConcurrentHashSet();
	private final Set<PlayingSound> fakeSounds = new HashSet();

	public WorldRadioManager(class_3218 world) {
		this.world = world;
	}

	public void tick() {
		this.fakeSounds.removeIf(sound -> {
			if (sound.isEnded()) {
				this.channels.values().forEach(ch -> ch.getFakeSounds().remove(sound));
				return true;
			} else {
				return false;
			}
		});
		this.channels.values().removeIf(RadioChannel::tickRemove);
	}

	public void registerRadioAudioListener(int channel, class_2338 pos) {
		this.runForChannelOrCreate(channel, ch -> ch.registerListener(pos));
	}

	public void registerRadioAudioPlayer(int channel, class_2338 pos) {
		this.runForChannelOrCreate(channel, ch -> ch.registerPlayer(pos));
	}

	public void unregisterRadio(int channel, class_2338 pos) {
		this.runForChannel(channel, ch -> {
			ch.unregisterListener(pos);
			ch.unregisterPlayer(pos);
		});
	}

	private void runForChannelOrCreate(int channel, Consumer<RadioChannel> consumer) {
		VoicechatServerApi api = VoiceIntegration.getServerApi();
		if (api != null) {
			synchronized (this.channels) {
				consumer.accept((RadioChannel)this.channels.computeIfAbsent(channel, i -> this.createChannel(api)));
			}
		}
	}

	private void runForChannel(int channel, Consumer<RadioChannel> consumer) {
		synchronized (this.channels) {
			RadioChannel ch = (RadioChannel)this.channels.get(channel);
			if (ch != null) {
				consumer.accept(ch);
			}
		}
	}

	private void runForChannelsOrCreate(IntSet channelIds, Consumer<RadioChannel> consumer) {
		VoicechatServerApi api = VoiceIntegration.getServerApi();
		if (api != null) {
			synchronized (this.channels) {
				IntIterator var5 = channelIds.iterator();

				while (var5.hasNext()) {
					int id = (Integer)var5.next();
					consumer.accept((RadioChannel)this.channels.computeIfAbsent(id, i -> this.createChannel(api)));
				}
			}
		}
	}

	private void runForChannels(IntSet channelIds, Consumer<RadioChannel> consumer) {
		synchronized (this.channels) {
			IntIterator var4 = channelIds.iterator();

			while (var4.hasNext()) {
				int id = (Integer)var4.next();
				RadioChannel ch = (RadioChannel)this.channels.get(id);
				if (ch != null) {
					consumer.accept(ch);
				}
			}
		}
	}

	private RadioChannel createChannel(VoicechatServerApi api) {
		return new RadioChannel(api, this.world, this.radioAudioChannels);
	}

	public void playFakeSound(UUID uuid, IntSet channels, Path path, boolean lock, boolean leftIndicator) {
		this.stopFakeSound(uuid);
		PlayingSound sound = new PlayingSound(channels, path, uuid, lock, leftIndicator, sender -> this.runForChannels(channels, sender));
		this.fakeSounds.add(sound);
		this.runForChannelsOrCreate(channels, ch -> ch.getFakeSounds().add(sound));
		sound.start();
	}

	public boolean stopFakeSound(UUID uuid) {
		return this.fakeSounds.removeIf(sound -> {
			if (sound.getUuid().equals(uuid)) {
				sound.interrupt();
				this.channels.values().forEach(ch -> ch.getFakeSounds().remove(sound));
				return true;
			} else {
				return false;
			}
		});
	}

	public Stream<UUID> getFakeSounds() {
		return this.fakeSounds.stream().map(PlayingSound::getUuid);
	}

	public void handlePluginLocPacket(class_243 pos, UUID id, byte[] data, float distance) {
		if (!this.radioAudioChannels.contains(id)) {
			double maxDistSq = distance * distance;
			this.channels.forEach((index, channel) -> channel.handleAudioPacket(id, pos, maxDistSq, data));
		}
	}

	public void handleMicPacket(class_3222 player, MicrophonePacket packet) {
		double maxDistSq = class_3532.method_33723(
			packet.isWhispering() ? RadioMod.CONFIG.getData().whisperingRecordMaxDist() : RadioMod.CONFIG.getData().recordMaxDist()
		);
		this.channels.forEach((index, channel) -> channel.handleAudioPacket(player.method_5667(), player.method_33571(), maxDistSq, packet.getOpusEncodedData()));
	}

	public void close() {
		this.fakeSounds.removeIf(sound -> {
			sound.interrupt();
			return true;
		});
		this.channels.values().removeIf(radioChannel -> {
			radioChannel.close();
			return true;
		});
	}
}
