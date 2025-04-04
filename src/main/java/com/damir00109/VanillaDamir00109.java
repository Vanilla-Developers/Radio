package com.damir00109;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class VanillaDamir00109 implements ModInitializer, VoicechatPlugin {
	public static final String MOD_ID = "vpl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static VoicechatServerApi vc_api;
	private static final LocationalAudioChannel[] channels = new LocationalAudioChannel[15];

	public static LocationalAudioChannel createChannelBy(int id, Vec3d pos, World level) {
		VoicechatServerApi api = VanillaDamir00109.vc_api;
		LocationalAudioChannel lac = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(level), api.createPosition(pos.x, pos.y, pos.z));
		VanillaDamir00109.channels[id-1] = lac;
		return lac;
	}
	public static VoicechatServerApi get_VCAPI() {
		return vc_api;
	}

	public static LocationalAudioChannel[] getAllChannels() {
		return channels;
	}
	public static LocationalAudioChannel getChannelByNum(int num) {
		return channels[num-1];
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
		assert sender != null;
		PlayerEntity player = (PlayerEntity) sender.getPlayer().getPlayer();
		Block target = Radio.RADIO;

		Block near_radio = getBlockNearby(player, target, 15);
		assert near_radio != null;
		//player.sendMessage(Text.literal("Nearby Radio: "+near_radio.toString()), false);
		((Radio.RadioBlock) near_radio).onMicrophoneNearby(event.getPacket());
		((Radio.RadioBlock) near_radio).flush();

		//VanillaDamir00109.LOGGER.info("Microphone! {}", event.toString());
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

}
