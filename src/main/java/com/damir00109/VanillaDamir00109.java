package com.damir00109;

import com.damir00109.audio.Channel;
import com.damir00109.blocks.DModBlocks;
import com.damir00109.blocks.Radio;
import com.damir00109.items.DModItems;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.entity.player.PlayerEntity;
import de.maxhenkel.voicechat.api.events.*;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.api.ModInitializer;
import de.maxhenkel.voicechat.api.*;
import net.minecraft.world.World;
import net.minecraft.block.*;
import org.slf4j.*;

import de.maxhenkel.voicechat.api.VolumeCategory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;

import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.block.Blocks;

public class VanillaDamir00109 implements ModInitializer, VoicechatPlugin {
	public static final String MOD_ID = "vpl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Channel[] channels = new Channel[15];
	public static final Map<BlockPos, BlockState> radios = new HashMap<>();
	public static VoicechatServerApi api;

	public static final String RADIO_VOLUME_CATEGORY_ID = "vpl_radio_vol";
	private static final String RADIO_ICON_PATH = "assets/vpl/textures/gui/radio_icon.png";

	public static Channel getChannel(int index) {
		if (index <= 0) return getChannel(index+1);
		return channels[index-1];
	}

	public static Channel createChannel(int index) {
		if (index <= 0) return createChannel(index+1);
		Channel channel = new Channel(index-1, api);
		channels[index-1] = channel;
		return channel;
	}
	public static Channel getOrCreate(int index) {
		Channel channel = getChannel(index);
		if (channel == null) channel = createChannel(index);
		return channel;
	}

	public static VoicechatServerApi getAPI() {
		return api;
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Register blocks and items...");
		DModItems.registerModItems();
		DModBlocks.registerModBlocks();
		//RadioTab.initialize();
	}

	@Override
	public String getPluginId() {
		return MOD_ID;
	}

	@Override
	public void initialize(VoicechatApi api) {
		VanillaDamir00109.api = (VoicechatServerApi) api;
		LOGGER.info("VoiceChat initialized");
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		VoicechatPlugin.super.registerEvents(registration);
		registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket);

		// Регистрируем обработчик события загрузки сущности
		ServerEntityEvents.ENTITY_LOAD.register(this::onEntityLoad);

		LOGGER.info("VoiceChat register events");
	}

	private void onServerStarted(VoicechatServerStartedEvent event) {
		VoicechatServerApi vcApi = event.getVoicechat();
		if (vcApi == null) {
			LOGGER.error("Failed to get VoicechatServerApi on ServerStartedEvent");
			return;
		}

		VolumeCategory.Builder builder = vcApi.volumeCategoryBuilder()
				.setId(RADIO_VOLUME_CATEGORY_ID)
				.setName("Radio")
				.setDescription("Adjusts the volume of radio blocks.");

		int[][] icon = getIconData(RADIO_ICON_PATH);
		if (icon != null) {
			builder.setIcon(icon);
		} else {
			LOGGER.warn("Radio volume category will be created without an icon.");
		}

		VolumeCategory radioVolumeCategory = builder.build();

		vcApi.registerVolumeCategory(radioVolumeCategory);
		LOGGER.info("Attempted to register radio volume category: {} with default name 'Radio' and icon: {}", 
				RADIO_VOLUME_CATEGORY_ID, (icon != null));
	}

	private record BlockData(BlockState state, BlockPos pos) {}
	
	private void onMicPacket(MicrophonePacketEvent event) {
		VoicechatConnection connection = event.getSenderConnection();
		VoicechatServerApi api = event.getVoicechat();
		if (connection == null && api == null) return;
		if (event.getPacket().getOpusEncodedData().length == 0) return;

		this.microThread(event);
	}

	private void microThread(MicrophonePacketEvent event) {
		VoicechatConnection sender = event.getSenderConnection();
		MicrophonePacket packet = event.getPacket();
		ServerLevel level = Objects.requireNonNull(event.getSenderConnection()).getPlayer().getServerLevel();
		assert sender != null;

		PlayerEntity player = (PlayerEntity) sender.getPlayer().getPlayer();
		Block target = DModBlocks.RADIO;

		BlockData near_radio = getBlockNearby(player, target, 5);
		if (near_radio == null) {
			return;
		}
		((Radio.RadioBlock) near_radio.state().getBlock()).onMicrophoneNearby(near_radio.state(), near_radio.pos(), level, packet);
	}


	public static BlockData getBlockNearby(PlayerEntity player, Block target, int radius) {
		//World world = player.getWorld();
		BlockPos playerPos = player.getBlockPos();

		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				for (int y = -radius; y <= radius; y++) {
					BlockPos checkPos = playerPos.add(x, y, z);
					BlockState state = radios.get(checkPos);
					if (state != null)
						return new BlockData(state, checkPos);
				}
			}
		}
		return null;
	}

	public static BlockState getAnyBlockAbove(BlockPos pos, World world, int radius, List<BlockState> exception) {
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();

		for (int yOffset = 1; yOffset <= radius; yOffset++) {
			mutablePos.set(pos.getX(), pos.getY()+yOffset, pos.getZ());

			BlockState blockstate = world.getBlockState(mutablePos);
			if (blockstate.isOf(Blocks.VOID_AIR) || blockstate.isOf(Blocks.AIR)) continue;
			if (exception.contains(blockstate)) continue;
			return blockstate;
		}
		return null;
	}
	public static BlockState getAnyBlockAbove(BlockPos pos, World world, int radius) {
		return getAnyBlockAbove(pos, world, radius, new ArrayList<>());
	}

	// HELPER METHOD TO TRANSFORM RADIO
	private void transformRadio(ServerWorld world, BlockPos radioPos, BlockState radioState) {
		if (!radioState.isOf(DModBlocks.RADIO)) return; // Should not happen if logic is correct

		LOGGER.info("Lightning struck antenna of radio at {}. Transforming to burnt radio.", radioPos);
		int power = radioState.get(Radio.POWER);

		world.setBlockState(radioPos, DModBlocks.BURNT_RADIO.getDefaultState(), 3);

		Channel channel = getChannel(power);
		if (channel != null) {
			channel.removeRadio(radioPos);
		}
		radios.remove(radioPos);
	}

	private int[][] getIconData(String path) {
		try (InputStream stream = VanillaDamir00109.class.getClassLoader().getResourceAsStream(path)) {
			if (stream == null) {
				LOGGER.warn("Failed to find icon resource: {}", path);
				return null;
			}
			BufferedImage image = ImageIO.read(stream);
			if (image == null) {
				LOGGER.warn("Failed to decode icon resource: {}", path);
				return null;
			}

			int width = image.getWidth();
			int height = image.getHeight();
			int[][] iconData = new int[width][height];
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					iconData[x][y] = image.getRGB(x, y);
				}
			}
			return iconData;
		} catch (IOException e) {
			LOGGER.error("Failed to load icon: {}", path, e);
			return null;
		}
	}

	private void onEntityLoad(Entity entity, ServerWorld world) {
		if (entity instanceof LightningEntity) {
			BlockPos strikePos = entity.getBlockPos();
			BlockPos currentPos;
			BlockState initialStrikeState = world.getBlockState(strikePos);

			// Если молния ударила в воздух, начнем проверку с блока ниже
			if (initialStrikeState.isAir()) {
				currentPos = strikePos.down();
			} else {
				currentPos = strikePos;
			}

			// Iterate downwards from the strike position
			// Max antenna height check to prevent infinite loops in weird scenarios (e.g., world edit mistakes)
			for (int i = 0; i < 256; i++) { // Check up to 256 blocks down
				if (currentPos.getY() < world.getBottomY()) {
					return; // Reached bottom of the world
				}

				BlockState blockStateAtCurrentPos = world.getBlockState(currentPos);

				// Case 1: Lightning struck a lightning rod
				if (blockStateAtCurrentPos.isOf(Blocks.LIGHTNING_ROD)) {
					BlockPos posBelowRod = currentPos.down();
					BlockState stateBelowRod = world.getBlockState(posBelowRod);

					if (stateBelowRod.isOf(DModBlocks.RADIO)) {
						// This lightning rod is directly above a radio block. Transform the radio.
						transformRadio(world, posBelowRod, stateBelowRod);
						return; // Radio transformed, task done.
					} else if (stateBelowRod.isOf(Blocks.LIGHTNING_ROD)) {
						// It's another segment of the antenna, continue checking downwards.
						currentPos = posBelowRod;
						// Continue to the next iteration of the for loop.
					} else {
						// Lightning rod is not above a radio or another rod segment.
						// This means it's either the base of an antenna not on a radio, or a standalone rod.
						return;
					}
				}
				// Case 2: Lightning struck the radio block directly
				else if (blockStateAtCurrentPos.isOf(DModBlocks.RADIO)) {
					transformRadio(world, currentPos, blockStateAtCurrentPos);
					return; // Radio transformed, task done.
				}
				// Case 3: Lightning struck something else (or air) at the current check level.
				else {
					// If the initial strike wasn't a rod or radio, or if our downward trace hit non-antenna block.
					return;
				}
			}
		}
	}
}
