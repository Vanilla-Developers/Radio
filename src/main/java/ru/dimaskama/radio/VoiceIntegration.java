package ru.dimaskama.radio;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.radio.extend.ServerWorldExtend;

public class VoiceIntegration {
    public static final int SAMPLE_RATE = 48000;
    public static final int PACKET_SIZE = 960;
    public static final long PACKET_RATE_NANOS = 20000000L;

    @Nullable
    private static VoicechatServerApi voicechatServerApi;
    @Nullable
    private static VolumeCategory radios;

    @Nullable
    public static VoicechatServerApi getServerApi() {
        return voicechatServerApi;
    }

    @Nullable
    public static VolumeCategory getRadiosVolumeCategory() {
        return radios;
    }

    static void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, VoiceIntegration::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, VoiceIntegration::onServerStopped);
        registration.registerEvent(MicrophonePacketEvent.class, VoiceIntegration::onMicPacket);
    }

    private static void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = voicechatServerApi = event.getVoicechat();
        radios = api.volumeCategoryBuilder()
                .setId("radios")
                .setName("Radios")
                .setIcon(loadIcon("assets/radio/category_icon.png"))
                .build();
        api.registerVolumeCategory(radios);
    }

    private static void onServerStopped(VoicechatServerStoppedEvent event) {
        voicechatServerApi = null;
        radios = null;
    }

    // Изменено с BlockPos на Vec3d
    public static void onPluginLocationPacket(ServerWorld world, Vec3d pos, UUID id, byte[] data, float distance) {
        WorldRadioManager radioManager = ((ServerWorldExtend) world).radio_getRadioManager();
        if (radioManager != null) {
            radioManager.handlePluginLocPacket(pos, id, data, distance);
        }
    }

    private static void onMicPacket(MicrophonePacketEvent event) {
        VoicechatConnection con;
        WorldRadioManager radioManager;
        if ((con = event.getSenderConnection()) != null
                && con.getPlayer().getPlayer() instanceof ServerPlayerEntity player
                // Исправлено: getWorld() -> getServerWorld() (или serverWorld)
                && (radioManager = ((ServerWorldExtend) player.serverWorld).radio_getRadioManager()) != null) {
            radioManager.handleMicPacket(player, (MicrophonePacket) event.getPacket());
        }
    }

    private static int[][] loadIcon(String filename) {
        return RadioMod.MOD_CONTAINER.findPath(filename).map(path -> {
            try (InputStream in = Files.newInputStream(path)) {
                BufferedImage image = ImageIO.read(in);
                int[][] arr = new int[16][16];

                for (int x = 0; x < 16; x++) {
                    int imgX = x * image.getWidth() / 16;
                    for (int y = 0; y < 16; y++) {
                        int imgY = y * image.getHeight() / 16;
                        arr[x][y] = image.getRGB(imgX, imgY);
                    }
                }
                return arr;
            } catch (Exception e) {
                RadioMod.LOGGER.error("Failed to load icon {}", filename, e);
                return null;
            }
        }).orElse(null);
    }
}