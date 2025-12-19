package ru.dimaskama.radio;

import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class PlayingSound extends Thread {
	private static final AtomicInteger THREAD_COUNT = new AtomicInteger();
	private final IntSet channels;
	private final Path path;
	private final UUID uuid;
	private final boolean lock;
	private final boolean leftIndicator;
	private final Consumer<Consumer<RadioChannel>> sender;
	private boolean ended;

	public PlayingSound(IntSet channels, Path path, UUID uuid, boolean lock, boolean leftIndicator, Consumer<Consumer<RadioChannel>> sender) {
		this.channels = channels;
		this.path = path;
		this.uuid = uuid;
		this.lock = lock;
		this.leftIndicator = leftIndicator;
		this.sender = sender;
		this.setName("RadioSoundPlayer#" + THREAD_COUNT.incrementAndGet());
		this.setDaemon(true);
	}

	public IntSet getChannels() {
		return this.channels;
	}

	public Path getPath() {
		return this.path;
	}

	public UUID getUuid() {
		return this.uuid;
	}

	public boolean isLock() {
		return this.lock;
	}

	public boolean hasLeftIndicator() {
		return this.leftIndicator;
	}

	public boolean isEnded() {
		return this.ended;
	}

	public void run() {
		short[] audio = FileSoundCache.get(this.path);
		if (audio.length != 0) {
			short[] buf1 = new short[960];
			short[] buf2 = new short[960];
			long start = System.nanoTime();
			int packetCount = 0;

			int pos;
			while (!this.isInterrupted() && (pos = packetCount * 960) < audio.length) {
				int size = audio.length - pos;
				if (size < 960) {
					Arrays.fill(buf1, (short)0);
				}

				System.arraycopy(audio, pos, buf1, 0, Math.min(size, 960));
				this.sender.accept((Consumer)channel -> {
					System.arraycopy(buf1, 0, buf2, 0, 960);
					((RadioChannel) channel).handleFakeSoundPiece(this.uuid, buf2);
				});
				long nextPlay = start + ++packetCount * 20000000L;
				long sleep = nextPlay - System.nanoTime();
				if (sleep > 0L) {
					try {
						Thread.sleep(sleep / 1000000L, (int)(sleep % 1000000L));
					} catch (InterruptedException var14) {
						this.interrupt();
					}
				}
			}
		}

		this.sender.accept((Consumer)channel -> ((RadioChannel) channel).handleFakeSoundPiece(this.uuid, null));
		this.ended = true;
	}
}
