package ru.dimaskama.radio;

import com.google.common.collect.Sets;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_3218;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.radio.blockentity.RadioBlockEntity;

public class RadioChannel implements AutoCloseable {
	private static final byte[] FLUSH_PACKET = new byte[0];
	private final Map<class_2338, RadioListener> radioListeners = new ConcurrentHashMap();
	private final Map<class_2338, RadioPlayer> radioPlayers = new ConcurrentHashMap();
	private final Map<UUID, RadioChannel.AudioProcessor> audioProcessors = new ConcurrentHashMap();
	private final Set<RadioListener> activeListeners = Sets.newConcurrentHashSet();
	private final Set<PlayingSound> fakeSounds = Sets.newConcurrentHashSet();
	private final Set<UUID> radioAudioChannels;
	private final VoicechatServerApi api;
	private final class_3218 world;
	private boolean lastLeftIndicator;

	public RadioChannel(VoicechatServerApi api, class_3218 world, Set<UUID> radioAudioChannels) {
		this.api = api;
		this.world = world;
		this.radioAudioChannels = radioAudioChannels;
	}

	public boolean tickRemove() {
		if (this.radioListeners.isEmpty() && this.radioPlayers.isEmpty() && this.fakeSounds.isEmpty()) {
			this.close();
			return true;
		} else {
			boolean activesUpdated = this.activeListeners.removeIf(radioListener -> radioListener.idleTicks.incrementAndGet() >= 3);
			boolean hasLeftIndicator = this.hasLeftIndicator();
			boolean leftIndicatorUpdated = this.lastLeftIndicator != (this.lastLeftIndicator = hasLeftIndicator);
			this.updatePlayerBlockEntities(activesUpdated || leftIndicatorUpdated, leftIndicatorUpdated ? hasLeftIndicator : null);
			this.updateListenerBlockEntities();
			this.audioProcessors.values().removeIf(audioProcessor -> {
				if (audioProcessor.idleTicks.incrementAndGet() >= 3) {
					audioProcessor.close();
					return true;
				} else {
					return false;
				}
			});
			return false;
		}
	}

	public void registerListener(class_2338 pos) {
		this.unregisterPlayer(pos);
		this.radioListeners.computeIfAbsent(pos, RadioListener::new);
	}

	public void unregisterListener(class_2338 pos) {
		this.radioListeners.remove(pos);
	}

	public void registerPlayer(class_2338 pos) {
		this.unregisterListener(pos);
		this.radioPlayers.computeIfAbsent(pos, p -> new RadioPlayer(this.api, this.world, p, this.radioAudioChannels));
	}

	public void unregisterPlayer(class_2338 pos) {
		RadioPlayer radioPlayer = (RadioPlayer)this.radioPlayers.remove(pos);
		if (radioPlayer != null) {
			radioPlayer.close();
		}
	}

	public Set<PlayingSound> getFakeSounds() {
		return this.fakeSounds;
	}

	public boolean isLocked() {
		return !this.fakeSounds.isEmpty() && this.fakeSounds.stream().anyMatch(PlayingSound::isLock);
	}

	public boolean hasLeftIndicator() {
		return !this.fakeSounds.isEmpty() && this.fakeSounds.stream().anyMatch(PlayingSound::hasLeftIndicator);
	}

	public void handleAudioPacket(UUID id, class_243 pos, double maxDistSquared, byte[] encoded) {
		if (encoded.length != 0 && !this.isLocked()) {
			RadioListener closestListener = null;
			double minSqDist = Double.MAX_VALUE;

			for (RadioListener listener : this.radioListeners.values()) {
				double d = listener.pos.method_1025(pos);
				if (minSqDist > d) {
					closestListener = listener;
					minSqDist = d;
				}
			}

			if (closestListener != null) {
				float volume = 1.0F - (float)(minSqDist / maxDistSquared);
				if (!(volume <= 0.0F)) {
					closestListener.idleTicks.set(0);
					boolean activesUpdated = this.activeListeners.add(closestListener);
					this.updatePlayerBlockEntities(activesUpdated, null);
					RadioChannel.AudioProcessor audioProcessor = this.getOrCreateAudioProcessor(id);
					audioProcessor.idleTicks.set(0);
					short[] audio = audioProcessor.getDecoder().decode(encoded);

					for (int i = 0; i < audio.length; i++) {
						audio[i] = (short)(audio[i] * volume);
					}

					audioProcessor.radioAudioEffect.apply(audio);
					closestListener.writeAmplitude(audio);
					byte[] newEncoded = audioProcessor.getEncoder().encode(audio);
					this.sendToPlayers(id, newEncoded);
				}
			}
		} else {
			this.sendToPlayers(id, FLUSH_PACKET);
		}
	}

	private void updatePlayerBlockEntities(boolean all, Boolean leftIndicatorToSet) {
		if (all) {
			this.world.method_8503().execute(() -> this.radioPlayers.forEach((pos, radioPlayer) -> {
				radioPlayer.isNew.set(false);
				this.updatePlayerBlockEntity(pos, leftIndicatorToSet);
			}));
		} else {
			this.radioPlayers.forEach((pos, radioPlayer) -> {
				if (radioPlayer.isNew.compareAndSet(true, false)) {
					this.world.method_8503().execute(() -> this.updatePlayerBlockEntity(pos, this.lastLeftIndicator));
				}
			});
		}
	}

	private void updatePlayerBlockEntity(class_2338 pos, Boolean leftIndicatorToSet) {
		if (this.world.method_8321(pos) instanceof RadioBlockEntity radio) {
			radio.updateComparators(this.world, this.activeListeners);
			if (leftIndicatorToSet != null) {
				radio.setLeftIndicator(leftIndicatorToSet);
			}
		}
	}

	private void updateListenerBlockEntities() {
		this.radioListeners.forEach((pos, radioListener) -> {
			int output = this.activeListeners.contains(radioListener) ? this.calculateListenerComparatorOutput(radioListener) : 0;
			if (output != radioListener.comparatorOutput) {
				radioListener.comparatorOutput = output;
				if (this.world.method_8321(pos) instanceof RadioBlockEntity radioBlockEntity) {
					radioBlockEntity.updateComparators(this.world, output);
				}
			}
		});
	}

	private int calculateListenerComparatorOutput(RadioListener radioListener) {
		short min = 0;
		short max = 0;
		synchronized (radioListener.amplitudes) {
			int size = radioListener.amplitudes.size();
			if (size > 40) {
				radioListener.amplitudes.removeElements(0, size - 40);
			}

			IntListIterator var6 = radioListener.amplitudes.iterator();

			while (var6.hasNext()) {
				int ampl = (Integer)var6.next();
				short m = RadioListener.decodeMin(ampl);
				if (m < min) {
					min = m;
				} else if ((m = RadioListener.decodeMax(ampl)) > max) {
					max = m;
				}
			}
		}

		return Math.round(15.0F * (max - min) / 65535.0F);
	}

	private RadioChannel.AudioProcessor getOrCreateAudioProcessor(UUID id) {
		return (RadioChannel.AudioProcessor)this.audioProcessors
			.computeIfAbsent(id, uuid -> new RadioChannel.AudioProcessor(this.api, new RadioAudioEffect(), new AtomicInteger()));
	}

	private void sendToPlayers(UUID id, byte[] packet) {
		this.radioPlayers.values().forEach(radioPlayer -> radioPlayer.packet(id, packet));
	}

	public void handleFakeSoundPiece(UUID id, @Nullable short[] audio) {
		if (audio == null) {
			this.sendToPlayers(id, FLUSH_PACKET);
		} else {
			RadioChannel.AudioProcessor processor = this.getOrCreateAudioProcessor(id);
			processor.idleTicks.set(0);
			processor.radioAudioEffect.apply(audio);
			byte[] encoded = processor.getEncoder().encode(audio);
			this.sendToPlayers(id, encoded);
		}
	}

	public void close() {
		this.radioPlayers.values().removeIf(radioPlayer -> {
			radioPlayer.close();
			return true;
		});
		this.audioProcessors.values().removeIf(audioProcessor -> {
			audioProcessor.close();
			return true;
		});
	}

	private static final class AudioProcessor implements AutoCloseable {
		private final VoicechatServerApi api;
		private final RadioAudioEffect radioAudioEffect;
		private final AtomicInteger idleTicks;
		private OpusEncoder encoder;
		private OpusDecoder decoder;

		private AudioProcessor(VoicechatServerApi api, RadioAudioEffect radioAudioEffect, AtomicInteger idleTicks) {
			this.api = api;
			this.radioAudioEffect = radioAudioEffect;
			this.idleTicks = idleTicks;
		}

		public void close() {
			if (this.encoder != null) {
				this.encoder.close();
			}

			if (this.decoder != null) {
				this.decoder.close();
			}
		}

		public OpusEncoder getEncoder() {
			if (this.encoder == null) {
				this.encoder = this.api.createEncoder();
			}

			return this.encoder;
		}

		public OpusDecoder getDecoder() {
			if (this.decoder == null) {
				this.decoder = this.api.createDecoder();
			}

			return this.decoder;
		}
	}
}
