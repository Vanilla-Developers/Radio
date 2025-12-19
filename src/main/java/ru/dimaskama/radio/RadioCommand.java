package ru.dimaskama.radio;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.util.UndashedUuid;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.class_124;
import net.minecraft.class_156;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2172;
import net.minecraft.class_2561;
import net.minecraft.class_5242;
import net.minecraft.class_7157;
import net.minecraft.class_2170.class_5364;
import net.minecraft.class_2558.class_10610;
import net.minecraft.class_2568.class_10613;
import ru.dimaskama.radio.extend.ServerWorldExtend;

public class RadioCommand implements CommandRegistrationCallback {
	private static final SimpleCommandExceptionType INVALID_RADIO_CHANNELS = new SimpleCommandExceptionType(
		new LiteralMessage("Invalid radio channel set. Must be \"all\" or different ints in range 1..15, delimited with \".\"")
	);
	private static final SimpleCommandExceptionType INVALID_FILE_PATH = new SimpleCommandExceptionType(new LiteralMessage("Invalid file path"));
	private static final SimpleCommandExceptionType FAILED_TO_PLAY = new SimpleCommandExceptionType(new LiteralMessage("Failed to load sound"));
	private static final SimpleCommandExceptionType NO_SOUND_PLAYING = new SimpleCommandExceptionType(
		new LiteralMessage("Sound with such UUID is already stopped or has never played")
	);
	private static final SimpleCommandExceptionType INVALID_URL = new SimpleCommandExceptionType(new LiteralMessage("Invalid URL"));
	private static final IntSet ALL_RADIO_CHANNELS = IntSet.of(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});

	public void register(CommandDispatcher<class_2168> commandDispatcher, class_7157 commandRegistryAccess, class_5364 registrationEnvironment) {
		commandDispatcher.register(
			(LiteralArgumentBuilder)((LiteralArgumentBuilder)class_2170.method_9247("radio").requires(src -> Permissions.check(src, "radio.command", 2)))
				.then(
					((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)class_2170.method_9247("fakesound")
									.then(
										class_2170.method_9247("play")
											.then(
												class_2170.method_9244("FileName", StringArgumentType.string())
													.suggests(this::suggestFilenames)
													.then(
														class_2170.method_9244("Channels", StringArgumentType.word())
															.suggests(this::suggestRadioChannels)
															.then(
																class_2170.method_9244("Lock", BoolArgumentType.bool())
																	.then(
																		((RequiredArgumentBuilder)class_2170.method_9244("LeftIndicator", BoolArgumentType.bool())
																				.executes(ctx -> this.executeFakeSoundPlay(ctx, UUID.randomUUID())))
																			.then(
																				class_2170.method_9244("UUID", class_5242.method_27643())
																					.executes(ctx -> this.executeFakeSoundPlay(ctx, class_5242.method_27645(ctx, "UUID")))
																			)
																	)
															)
													)
											)
									))
								.then(
									class_2170.method_9247("stop")
										.then(class_2170.method_9244("UUID", class_5242.method_27643()).suggests(this::suggestFakesounds).executes(this::executeFakeSoundStop))
								))
							.then(
								class_2170.method_9247("upload_file")
									.then(
										class_2170.method_9244("FileName", StringArgumentType.string())
											.then(class_2170.method_9244("URL", StringArgumentType.greedyString()).executes(this::executeFakeSoundUploadFile))
									)
							))
						.then(class_2170.method_9247("clearCache").executes(this::executeFakeSoundClearCache))
				)
		);
	}

	private int executeFakeSoundPlay(CommandContext<class_2168> context, UUID uuid) throws CommandSyntaxException {
		WorldRadioManager worldRadioManager = ((ServerWorldExtend)((class_2168)context.getSource()).method_9225()).radio_getRadioManager();
		if (worldRadioManager == null) {
			return 0;
		} else {
			IntSet channels = parseRadioChannels(StringArgumentType.getString(context, "Channels"));
			if (channels.isEmpty()) {
				throw INVALID_RADIO_CHANNELS.create();
			} else {
				String filename = StringArgumentType.getString(context, "FileName");

				Path smallPath;
				try {
					smallPath = Path.of(filename);
				} catch (InvalidPathException var11) {
					throw INVALID_FILE_PATH.create();
				}

				Path fullPath = RadioMod.SOUNDS_DIR.resolve(smallPath);
				boolean lock = BoolArgumentType.getBool(context, "Lock");
				boolean leftIndicator = BoolArgumentType.getBool(context, "LeftIndicator");
				CompletableFuture<short[]> future = FileSoundCache.getFuture(fullPath);
				if (!future.isDone() || !future.isCompletedExceptionally() && ((short[])future.join()).length != 0) {
					worldRadioManager.playFakeSound(uuid, channels, fullPath, lock, leftIndicator);
					((class_2168)context.getSource())
						.method_9226(
							() -> class_2561.method_43470("Playing sound " + filename + " with uuid ")
								.method_10852(
									class_2561.method_43470(uuid.toString())
										.method_27694(
											s -> s.method_10977(class_124.field_1054)
												.method_10949(new class_10613(class_2561.method_43470("Click to stop")))
												.method_10958(new class_10610("/radio fakesound stop " + uuid))
										)
								),
							true
						);
					return 1;
				} else {
					FileSoundCache.remove(fullPath);
					throw FAILED_TO_PLAY.create();
				}
			}
		}
	}

	private int executeFakeSoundStop(CommandContext<class_2168> context) throws CommandSyntaxException {
		WorldRadioManager worldRadioManager = ((ServerWorldExtend)((class_2168)context.getSource()).method_9225()).radio_getRadioManager();
		if (worldRadioManager == null) {
			return 0;
		} else {
			UUID uuid = class_5242.method_27645(context, "UUID");
			if (!worldRadioManager.stopFakeSound(uuid)) {
				throw NO_SOUND_PLAYING.create();
			} else {
				((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470("Stopped sound " + UndashedUuid.toString(uuid)), true);
				return 1;
			}
		}
	}

	private int executeFakeSoundUploadFile(CommandContext<class_2168> context) throws CommandSyntaxException {
		String filename = StringArgumentType.getString(context, "FileName");
		String urlString = StringArgumentType.getString(context, "URL");

		URL url;
		try {
			url = new URI(urlString).toURL();
		} catch (Exception var6) {
			throw INVALID_URL.create();
		}

		class_2168 source = (class_2168)context.getSource();
		source.method_9226(() -> class_2561.method_43470("Downloading sound from " + urlString + "..."), true);
		class_156.method_27958().execute(() -> {
			try {
				InputStream in = url.openConnection().getInputStream();

				try {
					Path path = RadioMod.SOUNDS_DIR.resolve(filename);
					byte[] bytes = in.readAllBytes();
					Files.write(path, bytes, new OpenOption[0]);
					source.method_9211().execute(() -> {
						FileSoundCache.remove(path);
						source.method_9226(() -> class_2561.method_43470("Upload finished: " + filename), true);
					});
				} catch (Throwable var7) {
					if (in != null) {
						try {
							in.close();
						} catch (Throwable var6x) {
							var7.addSuppressed(var6x);
						}
					}

					throw var7;
				}

				if (in != null) {
					in.close();
				}
			} catch (Exception var8) {
				source.method_9211().execute(() -> source.method_9213(class_2561.method_43470("Failed to download sound: " + var8)));
				RadioMod.LOGGER.warn("Failed to download sound", var8);
			}
		});
		return 1;
	}

	private int executeFakeSoundClearCache(CommandContext<class_2168> context) {
		FileSoundCache.clear();
		return 1;
	}

	private CompletableFuture<Suggestions> suggestRadioChannels(CommandContext<class_2168> context, SuggestionsBuilder builder) throws CommandSyntaxException {
		String remain = builder.getRemainingLowerCase();
		boolean suggested = false;
		if ("all".startsWith(remain)) {
			builder.suggest("all");
			suggested = true;
		}

		IntSet channels;
		try {
			channels = parseRadioChannels(remain);
		} catch (CommandSyntaxException var7) {
			if (!suggested) {
				throw var7;
			}

			return builder.buildFuture();
		}

		if (!remain.isBlank() && !remain.endsWith(".")) {
			if (!suggested) {
				builder.suggest(remain + ".");
			}
		} else {
			for (int i = 1; i <= 15; i++) {
				if (!channels.contains(i)) {
					builder.suggest(remain + i);
				}
			}
		}

		return builder.buildFuture();
	}

	private static IntSet parseRadioChannels(String string) throws CommandSyntaxException {
		if ("all".equalsIgnoreCase(string)) {
			return ALL_RADIO_CHANNELS;
		} else {
			String[] splited = string.split("\\.");
			IntSet ints = new IntArraySet();

			for (String s : splited) {
				if (!s.isBlank()) {
					try {
						int i = Integer.parseInt(s);
						if (i < 1 || i > 15 || !ints.add(i)) {
							throw INVALID_RADIO_CHANNELS.create();
						}
					} catch (NumberFormatException var8) {
						throw INVALID_RADIO_CHANNELS.create();
					}
				}
			}

			return ints;
		}
	}

	private CompletableFuture<Suggestions> suggestFilenames(CommandContext<class_2168> context, SuggestionsBuilder builder) {
		return CompletableFuture.supplyAsync(() -> {
			String remain = builder.getRemainingLowerCase();
			File[] files = RadioMod.SOUNDS_DIR.toFile().listFiles();
			if (files != null) {
				for (File file : files) {
					String filename = file.getName();
					if (filename.toLowerCase(Locale.ROOT).startsWith(remain)) {
						builder.suggest(filename);
					}
				}
			}

			return builder.build();
		}, class_156.method_27958());
	}

	private CompletableFuture<Suggestions> suggestFakesounds(CommandContext<class_2168> context, SuggestionsBuilder builder) {
		WorldRadioManager worldRadioManager = ((ServerWorldExtend)((class_2168)context.getSource()).method_9225()).radio_getRadioManager();
		return worldRadioManager == null ? builder.buildFuture() : class_2172.method_9264(worldRadioManager.getFakeSounds().map(UUID::toString), builder);
	}
}
