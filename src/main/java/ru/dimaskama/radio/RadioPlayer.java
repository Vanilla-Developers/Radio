package ru.dimaskama.radio;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_3218;

public class RadioPlayer implements AutoCloseable {
	public final AtomicBoolean isNew = new AtomicBoolean(true);
	private final Map<UUID, LocationalAudioChannel> shadowChannels = new ConcurrentHashMap();
	private final VoicechatServerApi api;
	private final class_3218 world;
	public final class_243 pos;
	private final Set<UUID> radioAudioChannels;

	public RadioPlayer(VoicechatServerApi api, class_3218 world, class_2338 pos, Set<UUID> radioAudioChannels) {
		this.api = api;
		this.world = world;
		this.pos = pos.method_46558();
		this.radioAudioChannels = radioAudioChannels;
	}

	public void packet(UUID uuid, byte[] data) {
		LocationalAudioChannel channel = data.length != 0
			? (LocationalAudioChannel)this.shadowChannels
				.computeIfAbsent(
					uuid,
					u -> {
						UUID shadowUuid = UUID.randomUUID();
						LocationalAudioChannel ch = this.api
							.createLocationalAudioChannel(
								shadowUuid, this.api.fromServerLevel(this.world), this.api.createPosition(this.pos.field_1352, this.pos.field_1351, this.pos.field_1350)
							);
						if (ch == null) {
							throw new IllegalStateException("Can't create audio channel");
						} else {
							ch.setDistance(RadioMod.CONFIG.getData().playerSoundDistance());
							VolumeCategory volumeCategory = VoiceIntegration.getRadiosVolumeCategory();
							if (volumeCategory != null) {
								ch.setCategory(volumeCategory.getId());
							}

							this.radioAudioChannels.add(shadowUuid);
							return ch;
						}
					}
				)
			: (LocationalAudioChannel)this.shadowChannels.get(uuid);
		if (channel != null) {
			if (data.length != 0) {
				channel.send(data);
			} else {
				this.shadowChannels.remove(uuid);
				this.radioAudioChannels.remove(channel.getId());
				channel.flush();
			}
		}
	}

	public void close() {
		this.shadowChannels.keySet().forEach(uuid -> {
			LocationalAudioChannel ch = (LocationalAudioChannel)this.shadowChannels.remove(uuid);
			this.radioAudioChannels.remove(ch.getId());
			ch.flush();
		});
	}
}
