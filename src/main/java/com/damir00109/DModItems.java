package com.damir00109;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.component.ComponentType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.damir00109.VanillaDamir00109.MOD_ID;

public final class DModItems {
	private DModItems() {}

	public static Item GLOWING_BRUSH;
	public static ComponentType<GlowingBrushData> GLOWING_BRUSH_DATA;

	/** Фасадный метод, вызываемый из MainMod */
	public static void registerModItems() {
		registerComponent();
		registerItem();
		registerEvents();
	}

	// 1) Регистрируем Data Component вместо NBT
	private static void registerComponent() {
		GLOWING_BRUSH_DATA = Registry.register(
				Registries.DATA_COMPONENT_TYPE,
				Identifier.of(MOD_ID, "glowing_brush_data"),
				ComponentType.<GlowingBrushData>builder()
						.codec(GlowingBrushData.CODEC)
						.build()
		);
	}

	// 2) Регистрируем сам предмет с дефолтным значением компонента
	private static void registerItem() {
		GLOWING_BRUSH = register(
				"glowing_brush",
				settings -> new BrushItem(
						settings.component(GLOWING_BRUSH_DATA,
								new GlowingBrushData("","",0,0,0))
				),
				new Item.Settings().maxDamage(64)
		);
	}

	// 3) Добавляем в креативную вкладку
	private static void registerEvents() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
				.register(e -> e.addAfter(Items.BRUSH, GLOWING_BRUSH));
	}

	// Утилитарный метод для регистрации в Registry.ITEM
	private static <T extends Item> T register(
			String path,
			Function<Item.Settings, T> factory,
			Item.Settings settings
	) {
		Identifier id = Identifier.of(MOD_ID, path);
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
		settings.registryKey(key);
		T item = factory.apply(settings);
		Registry.register(Registries.ITEM, id, item);
		return item;
	}

	/**
	 * Record для хранения данных кисти (последний блок, сущность и координаты)
	 */
	public static record GlowingBrushData(
			String lastBlock,
			String lastType,
			int x, int y, int z
	) {
		public static final Codec<GlowingBrushData> CODEC = RecordCodecBuilder.create(instance ->
				instance.group(
						Codec.STRING.fieldOf("lastBlock").forGetter(GlowingBrushData::lastBlock),
						Codec.STRING.fieldOf("lastType").forGetter(GlowingBrushData::lastType),
						Codec.INT.fieldOf("x").forGetter(GlowingBrushData::x),
						Codec.INT.fieldOf("y").forGetter(GlowingBrushData::y),
						Codec.INT.fieldOf("z").forGetter(GlowingBrushData::z)
				).apply(instance, GlowingBrushData::new)
		);
	}

	/**
	 * Класс кисти: логика использования, тайминги, запись и отображение данных
	 */
	public static class BrushItem extends Item {
		public BrushItem(Settings settings) { super(settings); }

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
		public void usageTick(World world, LivingEntity user, ItemStack stack, int rem) {
			if (!(world instanceof ServerWorld srv) || !(user instanceof ServerPlayerEntity player)) return;
			if (getMaxUseTime(stack, user) - rem != 20) return;

			Vec3d start = user.getCameraPosVec(0);
			Vec3d dir   = user.getRotationVec(0);
			Vec3d end   = start.add(dir.multiply(10));
			Box box     = user.getBoundingBox().expand(dir.x*10, dir.y*10, dir.z*10).expand(1);

			// Читаем старые данные и обновляем их
			GlowingBrushData data = stack.getOrDefault(
					GLOWING_BRUSH_DATA,
					new GlowingBrushData("","",0,0,0)
			);

			EntityHitResult eHit = ProjectileUtil.raycast(
					user, start, end, box,
					e -> e instanceof LivingEntity && e != user,
					10
			);
			if (eHit != null) {
				var tgt = (LivingEntity)eHit.getEntity();
				String type = Registries.ENTITY_TYPE.getId(tgt.getType()).toString();
				player.sendMessage(Text.literal("Entity: " + type), false);
				data = new GlowingBrushData(
						"",
						type,
						tgt.getBlockPos().getX(),
						tgt.getBlockPos().getY(),
						tgt.getBlockPos().getZ()
				);
			} else {
				BlockHitResult bHit = srv.raycast(new RaycastContext(
						start, end,
						RaycastContext.ShapeType.OUTLINE,
						RaycastContext.FluidHandling.NONE,
						user
				));
				if (bHit.getType() == HitResult.Type.BLOCK) {
					var pos = bHit.getBlockPos();
					String bId = Registries.BLOCK.getId(
							srv.getBlockState(pos).getBlock()
					).toString();
					player.sendMessage(Text.literal("Block: " + bId), false);
					data = new GlowingBrushData(
							bId,
							"",
							pos.getX(),
							pos.getY(),
							pos.getZ()
					);
				}
			}

			// Сохраняем обновлённые данные в компонент
			stack.set(GLOWING_BRUSH_DATA, data);

			// Наносим урон
			stack.damage(1, user, (EquipmentSlot)null);
		}

		@Override
		public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int rem) {
			if (user instanceof PlayerEntity p) {
				p.getItemCooldownManager().set(stack, 10);
			}
			return super.onStoppedUsing(stack, world, user, rem);
		}

		@Override
		@Environment(EnvType.CLIENT)
		public void appendTooltip(
				ItemStack stack,
				Item.TooltipContext context,
				net.minecraft.component.type.TooltipDisplayComponent dc,
				Consumer<Text> consumer,
				TooltipType type
		) {
			if (stack.contains(GLOWING_BRUSH_DATA)) {
				var d = stack.get(GLOWING_BRUSH_DATA);
				if (!d.lastBlock().isEmpty()) {
					consumer.accept(Text.literal("Last block: " + d.lastBlock()));
				}
				if (!d.lastType().isEmpty()) {
					consumer.accept(Text.literal("Last entity: " + d.lastType()));
				}
			}
			super.appendTooltip(stack, context, dc, consumer, type);
		}
	}
}
