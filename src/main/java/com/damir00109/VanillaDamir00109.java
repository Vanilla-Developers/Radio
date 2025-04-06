package com.damir00109;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaDamir00109 implements ModInitializer, VoicechatPlugin {
	public static final String MOD_ID = "vpl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static VoicechatServerApi vc_api;


	private static RadioChannel[] channels = new RadioChannel[15];


	public static VoicechatServerApi get_VCAPI() {
		return vc_api;
	}

	@Override
	public void onInitialize() {
		DModItems.registerModItems();   // Регистрация предметов
		Radio.registerModBlocks(); // Регистрация радио
		DModBlocks.registerModBlocks();
	}

	@Override
	public String getPluginId() {
		return MOD_ID;
	}

	@Override
	public void initialize(VoicechatApi api) {
		VoicechatPlugin.super.initialize(api);
		VanillaDamir00109.LOGGER.info("VoiceChat initialized");
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		VoicechatPlugin.super.registerEvents(registration);
		registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket);
		VanillaDamir00109.LOGGER.info("VoiceChat register events");
	}
	private void onServerStarted(VoicechatServerStartedEvent event) {
		VanillaDamir00109.vc_api = event.getVoicechat();
	}
	
	private void onMicPacket(MicrophonePacketEvent event) {
		VoicechatConnection sender = event.getSenderConnection();
		MicrophonePacket packet = event.getPacket();
		assert sender != null;
		PlayerEntity player = (PlayerEntity) sender.getPlayer().getPlayer();
		Block target = Radio.RADIO;

		Radio.RadioBlock near_radio = (Radio.RadioBlock) getBlockNearby(player, target, 15);
		if (!(near_radio instanceof Radio.RadioBlock)) return;
		near_radio.onMicrophoneNearby(packet);
		VanillaDamir00109.LOGGER.info("event has cancelled: {}", event.isCancelled());
	}

	public static Block getBlockNearby(PlayerEntity player, Block target, int radius) {
		World world = player.getWorld();
		BlockPos playerPos = player.getBlockPos();

		for(int x = -radius; x <= radius; x++) {
			for(int y = -radius; y <= radius; y++) {
				for(int z = -radius; z <= radius; z++) {
					BlockPos checkPos = playerPos.add(x, y, z);
					BlockState check_block = world.getBlockState(checkPos);
					if(check_block.isOf(target)) {
						return check_block.getBlock();
					}
				}
			}
		}
		return null;
	}

	public static RadioChannel[] getChannels() {
		return channels;
	}
	public static RadioChannel getChannelBy(int num) {
		if (num < 1) return null;
		return channels[num-1];
	}
	public static RadioChannel createChannel(int num) {
		if (num < 1) return null;
		RadioChannel channel = new RadioChannel(num-1, vc_api);
		channels[num-1] = channel;
		return channel;
	}
}
