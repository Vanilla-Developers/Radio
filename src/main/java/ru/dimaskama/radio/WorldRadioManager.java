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

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;

public class WorldRadioManager implements AutoCloseable {

    private final ServerWorld world;
    private final Int2ObjectMap<RadioChannel> channels =
            Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());
    private final Set<UUID> radioAudioChannels = Sets.newConcurrentHashSet();
    private final Set<PlayingSound> fakeSounds = new HashSet<>();

    public WorldRadioManager(ServerWorld world) {
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

    public void registerRadioAudioListener(int channel, BlockPos pos) {
        this.runForChannelOrCreate(channel, ch -> ch.registerListener(pos));
    }

    public void registerRadioAudioPlayer(int channel, BlockPos pos) {
        this.runForChannelOrCreate(channel, ch -> ch.registerPlayer(pos));
    }

    public void unregisterRadio(int channel, BlockPos pos) {
        this.runForChannel(channel, ch -> {
            ch.unregisterListener(pos);
            ch.unregisterPlayer(pos);
        });
    }

    private void runForChannelOrCreate(int channel, Consumer<RadioChannel> consumer) {
        VoicechatServerApi api = VoiceIntegration.getServerApi();
        if (api != null) {
            synchronized (this.channels) {
                consumer.accept(
                        this.channels.computeIfAbsent(channel, i -> this.createChannel(api))
                );
            }
        }
    }

    private void runForChannel(int channel, Consumer<RadioChannel> consumer) {
        synchronized (this.channels) {
            RadioChannel ch = this.channels.get(channel);
            if (ch != null) {
                consumer.accept(ch);
            }
        }
    }

    private void runForChannelsOrCreate(IntSet channelIds, Consumer<RadioChannel> consumer) {
        VoicechatServerApi api = VoiceIntegration.getServerApi();
        if (api != null) {
            synchronized (this.channels) {
                IntIterator it = channelIds.iterator();
                while (it.hasNext()) {
                    int id = it.nextInt();
                    consumer.accept(
                            this.channels.computeIfAbsent(id, i -> this.createChannel(api))
                    );
                }
            }
        }
    }

    private void runForChannels(IntSet channelIds, Consumer<RadioChannel> consumer) {
        synchronized (this.channels) {
            IntIterator it = channelIds.iterator();
            while (it.hasNext()) {
                int id = it.nextInt();
                RadioChannel ch = this.channels.get(id);
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
        PlayingSound sound =
                new PlayingSound(channels, path, uuid, lock, leftIndicator,
                        sender -> this.runForChannels(channels, sender));
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

    public void handlePluginLocPacket(Vec3d pos, UUID id, byte[] data, float distance) {
        if (!this.radioAudioChannels.contains(id)) {
            double maxDistSq = distance * distance;
            this.channels.forEach((index, channel) ->
                    channel.handleAudioPacket(id, pos, maxDistSq, data)
            );
        }
    }

    public void handleMicPacket(ServerPlayerEntity player, MicrophonePacket packet) {
        double maxDistSq = MathHelper.square(
                packet.isWhispering()
                        ? RadioMod.CONFIG.getData().whisperingRecordMaxDist()
                        : RadioMod.CONFIG.getData().recordMaxDist()
        );
        this.channels.forEach((index, channel) ->
                channel.handleAudioPacket(
                        player.getUuid(),
                        player.getPos(),
                        maxDistSq,
                        packet.getOpusEncodedData()
                )
        );
    }

    @Override
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
