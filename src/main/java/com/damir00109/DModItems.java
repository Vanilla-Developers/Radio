package com.damir00109;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;    // для креативной вкладки
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;                          // фабричный of(...)
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.function.Function;
import java.util.function.Predicate;

public final class DModItems {
	private DModItems() { }

	// 1) Регистрируем предмет, передавая фабрику + настройки
	public static final Item GLOWING_BRUSH = register(
			"glowing_brush",
			BrushItem::new,
			new Item.Settings().maxDamage(64)
	);

	private static <T extends Item> T register(
			String path,
			Function<Item.Settings, T> factory,
			Item.Settings settings
	) {
		// 2) Создаём идентификатор и ключ реестра
		Identifier id = Identifier.of(VanillaDamir00109.MOD_ID, path);
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

		// 3) Фиксируем ключ в настройках
		settings.registryKey(key);  // обязательно перед созданием Item :contentReference[oaicite:4]{index=4}

		// 4) Создаём и регистрируем предмет
		T item = factory.apply(settings);
		Registry.register(Registries.ITEM, id, item);
		return item;
	}

	public static void registerModItems() {
		// Добавляем в креативную вкладку «Инструменты» сразу после Items.BRUSH
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
				.register(entries -> entries.addAfter(Items.BRUSH, GLOWING_BRUSH));
	}

	public static class BrushItem extends Item {
		public BrushItem(Settings settings) {
			super(settings);
		}

		@Override
		public ActionResult use(World world, PlayerEntity user, Hand hand) {
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
			int elapsed = getMaxUseTime(stack, user) - remainingUseTicks;
			if (elapsed != 20) return;

			Vec3d start = user.getCameraPosVec(0);
			Vec3d look  = user.getRotationVec(0);
			Vec3d end   = start.add(look.multiply(10.0));

			Box box = user.getBoundingBox()
					.expand(look.x * 10, look.y * 10, look.z * 10)
					.expand(1.0);
			Predicate<net.minecraft.entity.Entity> filter =
					e -> e instanceof ServerPlayerEntity && e != user;
			EntityHitResult playerHit =
					ProjectileUtil.raycast(user, start, end, box, filter, 10.0);

			if (playerHit != null) {
				ServerPlayerEntity target = (ServerPlayerEntity) playerHit.getEntity();
				String msg = String.format("Player: uuid=%s, name=%s",
						target.getUuidAsString(),
						target.getGameProfile().getName()
				);
				serverPlayer.sendMessage(Text.literal(msg), false);
			} else {
				BlockHitResult blockHit = serverWorld.raycast(new RaycastContext(
						start, end,
						RaycastContext.ShapeType.OUTLINE,
						RaycastContext.FluidHandling.NONE,
						user
				));
				if (blockHit.getType() == HitResult.Type.BLOCK) {
					var pos = blockHit.getBlockPos();
					Identifier blockId = Registries.BLOCK.getId(
							serverWorld.getBlockState(pos).getBlock()
					);
					String msg = String.format("Block at: x=%d, y=%d, z=%d, id=%s",
							pos.getX(), pos.getY(), pos.getZ(), blockId
					);
					serverPlayer.sendMessage(Text.literal(msg), false);
				}
			}

			EquipmentSlot slot = (user.getActiveHand() == Hand.MAIN_HAND)
					? EquipmentSlot.MAINHAND
					: EquipmentSlot.OFFHAND;
			stack.damage(1, user, slot);
		}

		@Override
		public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
			if (user instanceof PlayerEntity p) {
				p.getItemCooldownManager().set(stack, 10);
			}
			return super.onStoppedUsing(stack, world, user, remainingUseTicks);
		}
	}
}
