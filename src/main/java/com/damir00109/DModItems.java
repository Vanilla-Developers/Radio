package com.damir00109;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;


public class DModItems {
	public static final Item GLOWING_BRUSH = registerItem("glowing_brush",
			new BrushItem(new Item.Settings().maxDamage(64).registryKey(
					RegistryKey.of(RegistryKeys.ITEM, Identifier.of(VanillaDamir00109.MOD_ID, "glowing_brush"))
			)));

	private static Item registerItem(String name, Item item) {
		return Registry.register(Registries.ITEM, Identifier.of(VanillaDamir00109.MOD_ID, name), item);
	}

	public static void registerModItems() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			entries.addAfter(Items.BRUSH, GLOWING_BRUSH);
		});
	}

	public static class BrushItem extends Item {
		public BrushItem(Settings settings) {
			super(settings);
		}

		@Override
		public ActionResult useOnBlock(ItemUsageContext context) {
			PlayerEntity player = context.getPlayer();
			World world = context.getWorld();
			BlockHitResult hitResult = new BlockHitResult(
					context.getHitPos(),
					context.getSide(),
					context.getBlockPos(),
					context.hitsInsideBlock()
			);

			if (player != null && world.getBlockEntity(hitResult.getBlockPos()) instanceof BrushableBlockEntity) {
				player.setCurrentHand(context.getHand());
				return ActionResult.CONSUME;
			}
			return ActionResult.PASS;
		}

		@Override
		public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
			if (!(world instanceof ServerWorld serverWorld) || !(user instanceof PlayerEntity)) return;

			HitResult hitResult = user.raycast(10.0, 0.0f, false);
			if (hitResult.getType() != HitResult.Type.BLOCK) return;

			BlockHitResult blockHit = (BlockHitResult) hitResult;
			if (serverWorld.getBlockEntity(blockHit.getBlockPos()) instanceof BrushableBlockEntity brushable) {
				if (remainingUseTicks == this.getMaxUseTime(stack, user) - 1) {
					user.playSound(SoundEvents.ITEM_BRUSH_BRUSHING_SAND, 1.0f, 1.0f);
				}

				if (brushable.brush(serverWorld.getTime(), serverWorld, user, blockHit.getSide(), stack)) {
					EquipmentSlot slot = user.getActiveHand() == Hand.MAIN_HAND
							? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

					stack.damage(1, user, slot);
				}
			}
		}

		@Override
		public int getMaxUseTime(ItemStack stack, LivingEntity user) {
			return 225; // Время использования как у ванильной кисти
		}

		@Override
		public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
			if (user instanceof PlayerEntity player) {
				player.getItemCooldownManager().set(stack, 10);
			}
			return false;
		}
	}


}
