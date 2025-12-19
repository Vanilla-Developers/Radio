package ru.dimaskama.radio.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import ru.dimaskama.radio.RadioMod;

public class JsonConfig<D> {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final transient String path;
	private final Codec<D> codec;
	private final Supplier<D> defaultSupplier;
	private D data;

	public JsonConfig(String path, Codec<D> codec, Supplier<D> defaultSupplier) {
		this.path = path;
		this.codec = codec;
		this.defaultSupplier = defaultSupplier;
	}

	public String getPath() {
		return this.path;
	}

	public D getData() {
		if (this.data == null) {
			this.reset();
		}

		return this.data;
	}

	public void loadOrCreate() {
		File file = new File(this.getPath());
		if (!file.exists()) {
			File parent = file.getParentFile();
			if (!parent.exists() && !parent.mkdirs()) {
				RadioMod.LOGGER.warn("Can't create config: {}", file.getAbsolutePath());
				return;
			}

			try {
				this.saveWithoutCatch();
			} catch (IOException var4) {
				RadioMod.LOGGER.warn("Exception occurred while writing new config. ", var4);
			}
		} else {
			this.load(file);
		}
	}

	private void load(File file) {
		try {
			FileReader f = new FileReader(file);

			try {
				this.deserialize(JsonParser.parseReader(f));
			} catch (Throwable var6) {
				try {
					f.close();
				} catch (Throwable var5) {
					var6.addSuppressed(var5);
				}

				throw var6;
			}

			f.close();
		} catch (Exception var7) {
			RadioMod.LOGGER.warn("Exception occurred while reading config. ", var7);
		}
	}

	protected void deserialize(JsonElement element) {
		this.data = (D)((Pair)this.codec.decode(JsonOps.INSTANCE, element).getOrThrow()).getFirst();
	}

	public void save() {
		this.save(true);
	}

	public void save(boolean log) {
		try {
			this.saveWithoutCatch();
			if (log) {
				RadioMod.LOGGER.info("Config saved: {}", this.getPath());
			}
		} catch (IOException var3) {
			RadioMod.LOGGER.warn("Exception occurred while saving config. ", var3);
		}
	}

	public void saveWithoutCatch() throws IOException {
		JsonElement json = this.serialize();
		FileWriter w = new FileWriter(this.getPath());

		try {
			GSON.toJson(json, w);
		} catch (Throwable var6) {
			try {
				w.close();
			} catch (Throwable var5) {
				var6.addSuppressed(var5);
			}

			throw var6;
		}

		w.close();
	}

	protected JsonElement serialize() {
		return (JsonElement)this.codec.encode(this.getData(), JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow();
	}

	public void reset() {
		this.data = (D)this.defaultSupplier.get();
	}

	static <T> MapCodec<T> defaultedField(Codec<T> codec, String fieldName, Supplier<T> defaultSupplier) {
		final MapCodec<T> delegate = codec.fieldOf(fieldName);
		return new MapCodec<T>() {
			public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
				return delegate.keys(ops);
			}

			public <T1> DataResult<T> decode(DynamicOps<T1> ops, MapLike<T1> input) {
				return input.get(fieldName) != null ? delegate.decode(ops, input) : DataResult.success(defaultSupplier.get());
			}

			public <T1> RecordBuilder<T1> encode(T input, DynamicOps<T1> ops, RecordBuilder<T1> prefix) {
				return delegate.encode(input, ops, prefix);
			}

			public String toString() {
				return "Defaulted[" + delegate + "]";
			}
		};
	}
}
