package com.damir00109;

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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;
import net.minecraft.command.argument.UuidArgumentType;
import com.damir00109.extend.ServerWorldExtend;

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

	@Override
	public void register(CommandDispatcher<ServerCommandSource> commandDispatcher, net.minecraft.command.CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
		commandDispatcher.register(
			(LiteralArgumentBuilder<ServerCommandSource>) (CommandManager.literal("radio").requires(src -> Permissions.check(src, "radio.command", 2)))
				.then(
					CommandManager.literal("fakesound")
						.then(
							CommandManager.literal("play")
								.then(
									CommandManager.argument("FileName", StringArgumentType.string())
										.suggests(this::suggestFilenames)
										.then(
											CommandManager.argument("Channels", StringArgumentType.word())
												.suggests(this::suggestRadioChannels)
												.then(
													CommandManager.argument("Lock", BoolArgumentType.bool())
														.then(
															((RequiredArgumentBuilder) CommandManager.argument("LeftIndicator", BoolArgumentType.bool())
																	.executes(ctx -> this.executeFakeSoundPlay(ctx, UUID.randomUUID())))
																.then(
																	CommandManager.argument("UUID", UuidArgumentType.uuid())
																		.executes(ctx -> this.executeFakeSoundPlay(ctx, UuidArgumentType.getUuid(ctx, "UUID")))
																)
														)
												)
										)
								)
						)
						.then(
							CommandManager.literal("stop")
								.then(CommandManager.argument("UUID", UuidArgumentType.uuid()).suggests(this::suggestFakesounds).executes(this::executeFakeSoundStop))
						)
						.then(
							CommandManager.literal("upload_file")
								.then(
									CommandManager.argument("FileName", StringArgumentType.string())
										.then(CommandManager.argument("URL", StringArgumentType.greedyString()).executes(this::executeFakeSoundUploadFile))
								)
						)
				)
				.then(CommandManager.literal("clearCache").executes(this::executeFakeSoundClearCache))
		);
	}

	private int executeFakeSoundPlay(CommandContext<ServerCommandSource> context, UUID uuid) throws CommandSyntaxException {
		WorldRadioManager worldRadioManager = ((ServerWorldExtend) context.getSource().getWorld()).radio_getRadioManager();
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
				// keep original behavior: if future not done OR not completed exceptionally and non-empty -> play
				if (!future.isDone() || (!future.isCompletedExceptionally() && future.join().length != 0)) {
					worldRadioManager.playFakeSound(uuid, channels, fullPath, lock, leftIndicator);
					context.getSource()
						.sendFeedback(
							() -> Text.literal("Playing sound " + filename + " with uuid ")
								.append(
									Text.literal(uuid.toString())
										.styled(style -> style.withColor(Formatting.GRAY)
											.withHoverEvent(() -> null)
											.withClickEvent(() -> null)
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

	private int executeFakeSoundStop(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		WorldRadioManager worldRadioManager = ((ServerWorldExtend) context.getSource().getWorld()).radio_getRadioManager();
		if (worldRadioManager == null) {
			return 0;
		} else {
			UUID uuid = UuidArgumentType.getUuid(context, "UUID");
			if (!worldRadioManager.stopFakeSound(uuid)) {
				throw NO_SOUND_PLAYING.create();
			} else {
				context.getSource().sendFeedback(() -> Text.literal("Stopped sound " + UndashedUuid.toString(uuid)), true);
				return 1;
			}
		}
	}

	private int executeFakeSoundUploadFile(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String filename = StringArgumentType.getString(context, "FileName");
		String urlString = StringArgumentType.getString(context, "URL");

		URL url;
		try {
			url = new URI(urlString).toURL();
		} catch (Exception var6) {
			throw INVALID_URL.create();
		}

		ServerCommandSource source = context.getSource();
		source.sendFeedback(() -> Text.literal("Downloading sound from " + urlString + "..."), true);

		// Run download asynchronously on common pool (matches original intent to not block server thread)
		CompletableFuture.runAsync(() -> {
			try (InputStream in = url.openConnection().getInputStream()) {
				try {
					Path path = RadioMod.SOUNDS_DIR.resolve(filename);
					byte[] bytes = in.readAllBytes();
					Files.write(path, bytes, new OpenOption[0]);
					// schedule task back on server thread
					source.getServer().execute(() -> {
						FileSoundCache.remove(path);
						source.sendFeedback(() -> Text.literal("Upload finished: " + filename), true);
					});
				} catch (Throwable var7) {
					// ensure InputStream closed by try-with-resources and rethrow
					throw var7;
				}
			} catch (Exception var8) {
				source.getServer().execute(() -> source.sendError(Text.literal("Failed to download sound: " + var8)));
				RadioMod.LOGGER.warn("Failed to download sound", var8);
			}
		});
		return 1;
	}

	private int executeFakeSoundClearCache(CommandContext<ServerCommandSource> context) {
		FileSoundCache.clear();
		return 1;
	}

	private CompletableFuture<Suggestions> suggestRadioChannels(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
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

	private CompletableFuture<Suggestions> suggestFilenames(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
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
		});
	}

	private CompletableFuture<Suggestions> suggestFakesounds(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
		WorldRadioManager worldRadioManager = ((ServerWorldExtend) context.getSource().getWorld()).radio_getRadioManager();
		if (worldRadioManager == null) return builder.buildFuture();

		// Build suggestions from currently playing fake sounds
		return CompletableFuture.supplyAsync(() -> {
			worldRadioManager.getFakeSounds().map(UUID::toString).forEach(builder::suggest);
			return builder.build();
		});
	}
}
