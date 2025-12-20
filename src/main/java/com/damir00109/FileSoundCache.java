package com.damir00109;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.mp3.Mp3Decoder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;
import org.jetbrains.annotations.Nullable;

public class FileSoundCache {
	public static AudioFormat FORMAT = new AudioFormat(Encoding.PCM_SIGNED, 48000.0F, 16, 1, 2, 48000.0F, false);
	private static final short[] EMPTY_RESULT = new short[0];
	private static final Map<Path, CompletableFuture<short[]>> CACHE = new ConcurrentHashMap();

	public static CompletableFuture<short[]> getFuture(Path path) {
		return (CompletableFuture<short[]>)CACHE.computeIfAbsent(path, p -> {
			VoicechatApi api = VoiceIntegration.getServerApi();
			if (api == null) {
				RadioMod.LOGGER.error("Unable to download sound {} Cannot access voicechat api", p);
				return CompletableFuture.completedFuture(EMPTY_RESULT);
			} else {
				FileSoundCache.FileType fileType = FileSoundCache.FileType.get(p);
				if (fileType == null) {
					RadioMod.LOGGER.error("Unable to download sound {} Unknown file type", p);
					return CompletableFuture.completedFuture(EMPTY_RESULT);
				} else {
					return CompletableFuture.supplyAsync(() -> {
						try {
							return fileType.convert(api, path.toFile());
						} catch (IOException var5) {
							RadioMod.LOGGER.error("Failed to download or convert sound {}", p, var5);
							return EMPTY_RESULT;
						}
					});
				}
			}
		});
	}

	public static short[] get(Path path) {
		return (short[])getFuture(path).join();
	}

	public static void remove(Path path) {
		CompletableFuture<short[]> future = (CompletableFuture<short[]>)CACHE.remove(path);
		if (future != null) {
			future.cancel(true);
		}
	}

	public static void clear() {
		CACHE.values().removeIf(future -> {
			future.cancel(true);
			return true;
		});
	}

	private static short[] convertToVoicechatFormat(VoicechatApi api, AudioInputStream audioIn) throws IOException {
		AudioFormat format = audioIn.getFormat();
		AudioFormat convertFormat = new AudioFormat(
			Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false
		);
		AudioInputStream str1 = AudioSystem.getAudioInputStream(convertFormat, audioIn);

		short[] var6;
		try {
			AudioInputStream str2 = AudioSystem.getAudioInputStream(FORMAT, str1);

			try {
				var6 = api.getAudioConverter().bytesToShorts(str2.readAllBytes());
			} catch (Throwable var10) {
				if (str2 != null) {
					try {
						str2.close();
					} catch (Throwable var9) {
						var10.addSuppressed(var9);
					}
				}

				throw var10;
			}

			if (str2 != null) {
				str2.close();
			}
		} catch (Throwable var11) {
			if (str1 != null) {
				try {
					str1.close();
				} catch (Throwable var8) {
					var11.addSuppressed(var8);
				}
			}

			throw var11;
		}

		if (str1 != null) {
			str1.close();
		}

		return var6;
	}

	private static enum FileType {
		WAV(Pattern.compile("^.*\\.wav$")) {
			@Override
			protected short[] convert(VoicechatApi api, File file) throws IOException {
				try {
					AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);

					short[] var4;
					try {
						var4 = FileSoundCache.convertToVoicechatFormat(api, audioIn);
					} catch (Throwable var7) {
						if (audioIn != null) {
							try {
								audioIn.close();
							} catch (Throwable var6) {
								var7.addSuppressed(var6);
							}
						}

						throw var7;
					}

					if (audioIn != null) {
						audioIn.close();
					}

					return var4;
				} catch (UnsupportedAudioFileException var8) {
					throw new IOException(var8);
				}
			}
		},
		MP3(Pattern.compile("^.*\\.mp3$")) {
			@Override
			protected short[] convert(VoicechatApi api, File file) throws IOException {
				InputStream in = new FileInputStream(file);

				short[] var7;
				try {
					Mp3Decoder mp3Decoder = api.createMp3Decoder(in);
					if (mp3Decoder == null) {
						throw new IOException("Error creating Mp3 decoder");
					}

					byte[] data = api.getAudioConverter().shortsToBytes(mp3Decoder.decode());
					AudioFormat format = mp3Decoder.getAudioFormat();
					var7 = FileSoundCache.convertToVoicechatFormat(api, new AudioInputStream(new ByteArrayInputStream(data), format, data.length / format.getFrameSize()));
				} catch (Throwable var9) {
					try {
						in.close();
					} catch (Throwable var8) {
						var9.addSuppressed(var8);
					}

					throw var9;
				}

				in.close();
				return var7;
			}
		};

		private final Pattern pattern;

		private FileType(Pattern pattern) {
			this.pattern = pattern;
		}

		protected short[] convert(VoicechatApi api, File file) throws IOException {
			throw new AssertionError("Not implemented");
		}

		@Nullable
		private static FileSoundCache.FileType get(Path path) {
			return get(path.getFileName().toString());
		}

		@Nullable
		private static FileSoundCache.FileType get(String filename) {
			for (FileSoundCache.FileType type : values()) {
				if (type.pattern.matcher(filename).matches()) {
					return type;
				}
			}

			return null;
		}
	}
}
