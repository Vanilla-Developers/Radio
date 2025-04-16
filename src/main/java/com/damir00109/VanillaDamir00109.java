package com.damir00109;

import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.entity.player.PlayerEntity;
import de.maxhenkel.voicechat.api.events.*;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.api.ModInitializer;
import de.maxhenkel.voicechat.api.*;
import net.minecraft.world.World;
import net.minecraft.block.*;
import org.slf4j.*;

public class VanillaDamir00109 implements ModInitializer, VoicechatPlugin {
	public static final String MOD_ID = "vpl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Channel[] channels = new Channel[15];
	public static VoicechatServerApi api;

	public static Channel getChannel(int index) {
		return channels[index];
	}

	public static Channel createChannel(int index) {
		Channel channel = new Channel(index, api);
		channels[index] = channel;
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
		//DModBlocks.registerModBlocks();
	}

	@Override
	public String getPluginId() {
		return MOD_ID;
	}

	@Override
	public void initialize(VoicechatApi api) {
		LOGGER.info("VoiceChat initialized");
		this.api = (VoicechatServerApi) api;
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		VoicechatPlugin.super.registerEvents(registration);
		registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket);
		LOGGER.info("VoiceChat register events");
	}
	private void onServerStarted(VoicechatServerStartedEvent event) {}

	private boolean vc_on() {
		return false;

	}
	
	private void onMicPacket(MicrophonePacketEvent event) {
		if (vc_on()) return;
		VoicechatConnection sender = event.getSenderConnection();
		MicrophonePacket packet = event.getPacket();
		assert sender != null;
		PlayerEntity player = (PlayerEntity) sender.getPlayer().getPlayer();
		Block target = DModBlocks.RADIO;

		BlockState near_radio = getBlockNearby(player, target, 15);
		assert near_radio != null;
		if (!(near_radio.isOf(target))) return;
		((Radio.RadioBlock) near_radio.getBlock()).onMicrophoneNearby(near_radio, packet);
	}

	public static BlockState getBlockNearby(PlayerEntity player, Block target, int radius) {
		World world = player.getWorld();
		BlockPos playerPos = player.getBlockPos();

		for(int x = -radius; x <= radius; x++) {
			for(int y = -radius; y <= radius; y++) {
				for(int z = -radius; z <= radius; z++) {
					BlockPos checkPos = playerPos.add(x, y, z);
					BlockState check_block = world.getBlockState(checkPos);
					if(check_block.isOf(target)) {
						return check_block;
					}
				}
			}
		}
		return null;
	}
	public static BlockState getAnyBlockAbove(BlockPos pos, World world, int radius) {
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();

		for (int yOffset = 1; yOffset <= radius; yOffset++) {
			mutablePos.set(pos.getX(), pos.getY()+yOffset, pos.getZ());

			BlockState blockstate = world.getBlockState(mutablePos);
			if (blockstate.isOf(Blocks.VOID_AIR) || blockstate.isOf(Blocks.AIR)) continue;
			return blockstate;
		}
		return null;
	}
}
