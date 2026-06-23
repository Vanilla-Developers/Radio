package com.damir00109;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;

public class RadioPlayer implements AutoCloseable {
	public final AtomicBoolean isNew = new AtomicBoolean(true);
	private final Map<UUID, LocationalAudioChannel> shadowChannels = new ConcurrentHashMap<>();
	private final VoicechatServerApi api;
	private final ServerLevel world;
	public final BlockPos pos;
	private final Set<UUID> radioAudioChannels;

	public RadioPlayer(VoicechatServerApi api, ServerLevel world, BlockHitResult hitResult, Set<UUID> radioAudioChannels) {
		this.api = api;
		this.world = world;
		this.pos = hitResult.getBlockPos();
		this.radioAudioChannels = radioAudioChannels;
	}

	public void packet(UUID uuid, byte[] data) {
		LocationalAudioChannel channel = data.length != 0
			? this.shadowChannels.computeIfAbsent(
					uuid,
					u -> {
						UUID shadowUuid = UUID.randomUUID();
						LocationalAudioChannel ch = this.api.createLocationalAudioChannel(
							shadowUuid,
							this.api.fromServerLevel(this.world),
							this.api.createPosition(this.pos.getX(), this.pos.getY(), this.pos.getZ())
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
			: this.shadowChannels.get(uuid);

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

	@Override
	public void close() {
		this.shadowChannels.keySet().forEach(uuid -> {
			LocationalAudioChannel ch = this.shadowChannels.remove(uuid);
			if (ch != null) {
				this.radioAudioChannels.remove(ch.getId());
				ch.flush();
			}
		});
	}
}
