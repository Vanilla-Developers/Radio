package com.damir00109;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.fabricmc.api.ModInitializer;
import net.minecraft.MinecraftVersion;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaDamir00109 implements ModInitializer, VoicechatPlugin {
	public static final String MOD_ID = "vpl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Register blocks and items...");
		DModItems.registerModItems();
		Radio.registerModBlocks();
		DModBlocks.registerModBlocks();
	}

	@Override
	public String getPluginId() { return MOD_ID; }

	@Override
	public void initialize(VoicechatApi api) {
		VoicechatPlugin.super.initialize(api);
		LOGGER.info("VoiceChat initialized");
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		VoicechatPlugin.super.registerEvents(registration);
		registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket);
		LOGGER.info("VoiceChat register events");
	}
	private void onServerStarted(VoicechatServerStartedEvent event) {}

	private boolean vc_on() {return false;}
	
	private void onMicPacket(MicrophonePacketEvent event) {
		if (vc_on()) return;
		VoicechatConnection sender = event.getSenderConnection();
		MicrophonePacket packet = event.getPacket();
		assert sender != null;
		PlayerEntity player = (PlayerEntity) sender.getPlayer().getPlayer();
		Block target = Radio.RADIO;

		Radio.RadioBlock near_radio = (Radio.RadioBlock) getBlockNearby(player, target, 15);
		if (!(near_radio instanceof Radio.RadioBlock)) return;
		near_radio.onMicrophoneNearby(packet);
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
