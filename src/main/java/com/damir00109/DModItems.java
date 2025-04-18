package com.damir00109;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class DModItems {
	public static final Item GLOWING_BRUSH = registerItem("glowing_brush",
			new BrushItem(new Item.Settings().maxDamage(64).registryKey(
					RegistryKey.of(RegistryKeys.ITEM, Identifier.of(VanillaDamir00109.MOD_ID, "glowing_brush"))
			))
	);

	private static Item registerItem(String name, Item item) {
		return Registry.register(Registries.ITEM,
				Identifier.of(VanillaDamir00109.MOD_ID, name),
				item
		);
	}

	public static void registerModItems() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
				.register(entries -> entries.addAfter(Items.BRUSH, GLOWING_BRUSH));
	}

	public static class BrushItem extends Item {
		public BrushItem(Settings settings) {
			super(settings);
		}

		@Override
		public ActionResult use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			user.setCurrentHand(hand);
			return ActionResult.CONSUME;
		}

		@Override
		public UseAction getUseAction(ItemStack stack) {
			return UseAction.BRUSH;
		}

		@Override
		public int getMaxUseTime(ItemStack stack, LivingEntity user) {
			return 225;
		}

		@Override
		public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
			if (!(world instanceof ServerWorld serverWorld) || !(user instanceof ServerPlayerEntity serverPlayer)) {
				return;
			}

			int elapsed = this.getMaxUseTime(stack, user) - remainingUseTicks;
			if (elapsed == 20) {
				HitResult hit = user.raycast(10.0, 0.0f, false);
				if (hit.getType() == HitResult.Type.BLOCK) {
					BlockHitResult blockHit = (BlockHitResult) hit;
					int x = blockHit.getBlockPos().getX();
					int y = blockHit.getBlockPos().getY();
					int z = blockHit.getBlockPos().getZ();
					Identifier blockId = Registries.BLOCK.getId(
							serverWorld.getBlockState(blockHit.getBlockPos()).getBlock()
					);
					String message = String.format(
							"Block at: x=%d, y=%d, z=%d, id=%s", x, y, z, blockId
					);
					serverPlayer.sendMessage(Text.literal(message), false);
				}
				EquipmentSlot slot = user.getActiveHand() == Hand.MAIN_HAND
						? EquipmentSlot.MAINHAND
						: EquipmentSlot.OFFHAND;
				stack.damage(1, user, slot);
			}
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
