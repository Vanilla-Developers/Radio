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
import net.minecraft.class_243;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_9848;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.radio.extend.ServerWorldExtend;

public class VoiceIntegration {
	public static final int SAMPLE_RATE = 48000;
	public static final int PACKET_SIZE = 960;
	public static final long PACKET_RATE_NANOS = 20000000L;
	@Nullable
	@Nullable
	private static VoicechatServerApi voicechatServerApi;
	@Nullable
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
		radios = api.volumeCategoryBuilder().setId("radios").setName("Radios").setIcon(loadIcon("assets/radio/category_icon.png")).build();
		api.registerVolumeCategory(radios);
	}

	private static void onServerStopped(VoicechatServerStoppedEvent event) {
		voicechatServerApi = null;
		radios = null;
	}

	public static void onPluginLocationPacket(class_3218 world, class_243 pos, UUID id, byte[] data, float distance) {
		WorldRadioManager radioManager = ((ServerWorldExtend)world).radio_getRadioManager();
		if (radioManager != null) {
			radioManager.handlePluginLocPacket(pos, id, data, distance);
		}
	}

	private static void onMicPacket(MicrophonePacketEvent event) {
		VoicechatConnection con;
		WorldRadioManager radioManager;
		if ((con = event.getSenderConnection()) != null
			&& con.getPlayer().getPlayer() instanceof class_3222 player
			&& (radioManager = ((ServerWorldExtend)player.method_51469()).radio_getRadioManager()) != null) {
			radioManager.handleMicPacket(player, (MicrophonePacket)event.getPacket());
		}
	}

	private static int[][] loadIcon(String filename) {
		return (int[][])RadioMod.MOD_CONTAINER.findPath(filename).map(path -> {
			try {
				InputStream in = Files.newInputStream(path);

				int[][] var12;
				try {
					BufferedImage image = ImageIO.read(in);
					int[][] arr = new int[16][16];

					for (int x = 0; x < 16; x++) {
						int imgX = x * image.getWidth() / 16;

						for (int y = 0; y < 16; y++) {
							int imgY = y * image.getHeight() / 16;
							arr[x][y] = class_9848.method_61334(image.getRGB(imgX, imgY));
						}
					}

					var12 = arr;
				} catch (Throwable var10) {
					if (in != null) {
						try {
							in.close();
						} catch (Throwable var9) {
							var10.addSuppressed(var9);
						}
					}

					throw var10;
				}

				if (in != null) {
					in.close();
				}

				return var12;
			} catch (Exception var11) {
				RadioMod.LOGGER.error("Failed to load icon {}", filename);
				return null;
			}
		}).orElse(null);
	}
}
