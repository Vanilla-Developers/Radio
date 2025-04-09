package com.damir00109;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrushableBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DModItems {
	public static final Item BRUSH = registerItem("brush", new Brush(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(VanillaDamir00109.MOD_ID, "brush")))));
	public static final Item RAW_PINK_GARNET = registerItem("raw_pink_garnet", new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(VanillaDamir00109.MOD_ID, "raw_pink_garnet")))));

	private static Item registerItem(String name, Item item) {
		Identifier id = Identifier.of(VanillaDamir00109.MOD_ID, name);
		return Registry.register(Registries.ITEM, id, item);
	}

	public static void registerModItems() {
		VanillaDamir00109.LOGGER.info("Registering Mod Items for " + VanillaDamir00109.MOD_ID);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
			entries.add(BRUSH);
			entries.add(RAW_PINK_GARNET);
		});
	}
	private static class Brush extends Item {
		public static final int ANIMATION_DURATION = 10;
		private static final int MAX_BRUSH_TIME = 200;

		public Brush(Item.Settings settings) {
			super(settings);
		}

		public ActionResult useOnBlock(ItemUsageContext context) {
			PlayerEntity playerEntity = context.getPlayer();
			if (playerEntity != null && this.getHitResult(playerEntity).getType() == HitResult.Type.BLOCK) {
				playerEntity.setCurrentHand(context.getHand());
			}

			return ActionResult.CONSUME;
		}

		public UseAction getUseAction(ItemStack stack) {
			return UseAction.BRUSH;
		}

		public int getMaxUseTime(ItemStack stack, LivingEntity user) {
			return 200;
		}

		public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
			if (remainingUseTicks >= 0 && user instanceof PlayerEntity playerEntity) {
				HitResult hitResult = this.getHitResult(playerEntity);
				if (hitResult instanceof BlockHitResult blockHitResult) {
					if (hitResult.getType() == HitResult.Type.BLOCK) {
						int i = this.getMaxUseTime(stack, user) - remainingUseTicks + 1;
						boolean bl = i % 10 == 5;
						if (bl) {
							BlockPos blockPos = blockHitResult.getBlockPos();
							BlockState blockState = world.getBlockState(blockPos);
							Arm arm = user.getActiveHand() == Hand.MAIN_HAND ? playerEntity.getMainArm() : playerEntity.getMainArm().getOpposite();
							if (blockState.hasBlockBreakParticles() && blockState.getRenderType() != BlockRenderType.INVISIBLE) {
								this.addDustParticles(world, blockHitResult, blockState, user.getRotationVec(0.0F), arm);
							}

							Block var15 = blockState.getBlock();
							SoundEvent soundEvent;
							if (var15 instanceof BrushableBlock brushableBlock) {
								soundEvent = brushableBlock.getBrushingSound();
							} else {
								soundEvent = SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC;
							}

							world.playSound(playerEntity, blockPos, soundEvent, SoundCategory.BLOCKS);
							if (world instanceof ServerWorld serverWorld) {
								BlockEntity var16 = world.getBlockEntity(blockPos);
								if (var16 instanceof BrushableBlockEntity brushableBlockEntity) {
									boolean bl2 = brushableBlockEntity.brush(world.getTime(), serverWorld, playerEntity, blockHitResult.getSide(), stack);
									if (bl2) {
										EquipmentSlot equipmentSlot = stack.equals(playerEntity.getEquippedStack(EquipmentSlot.OFFHAND)) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
										stack.damage(1, playerEntity, equipmentSlot);
									}
								}
							}
						}

						return;
					}
				}

				user.stopUsingItem();
			} else {
				user.stopUsingItem();
			}
		}

		private HitResult getHitResult(PlayerEntity user) {
			return ProjectileUtil.getCollision(user, EntityPredicates.CAN_HIT, user.getBlockInteractionRange());
		}

		private void addDustParticles(World world, BlockHitResult hitResult, BlockState state, Vec3d userRotation, Arm arm) {
			double d = 3.0;
			int i = arm == Arm.RIGHT ? 1 : -1;
			int j = world.getRandom().nextBetweenExclusive(7, 12);
			BlockStateParticleEffect blockStateParticleEffect = new BlockStateParticleEffect(ParticleTypes.BLOCK, state);
			Direction direction = hitResult.getSide();
			DustParticlesOffset dustParticlesOffset = DustParticlesOffset.fromSide(userRotation, direction);
			Vec3d vec3d = hitResult.getPos();

			for(int k = 0; k < j; ++k) {
				world.addParticleClient(blockStateParticleEffect, vec3d.x - (double)(direction == Direction.WEST ? 1.0E-6F : 0.0F), vec3d.y, vec3d.z - (double)(direction == Direction.NORTH ? 1.0E-6F : 0.0F), dustParticlesOffset.xd() * (double)i * 3.0 * world.getRandom().nextDouble(), 0.0, dustParticlesOffset.zd() * (double)i * 3.0 * world.getRandom().nextDouble());
			}

		}

		private static record DustParticlesOffset(double xd, double yd, double zd) {
			private static final double field_42685 = 1.0;
			private static final double field_42686 = 0.1;

			private DustParticlesOffset(double xd, double yd, double zd) {
				this.xd = xd;
				this.yd = yd;
				this.zd = zd;
			}

			public static DustParticlesOffset fromSide(Vec3d userRotation, Direction side) {
				double d = 0.0;

				return switch (side) {
					case DOWN, UP -> new DustParticlesOffset(userRotation.getZ(), 0.0, -userRotation.getX());
					case NORTH -> new DustParticlesOffset(1.0, 0.0, -0.1);
					case SOUTH -> new DustParticlesOffset(-1.0, 0.0, 0.1);
					case WEST -> new DustParticlesOffset(-0.1, 0.0, -1.0);
					case EAST -> new DustParticlesOffset(0.1, 0.0, 1.0);
					default -> throw new MatchException((String) null, (Throwable) null);
				};
			}

			public double xd() {
				return this.xd;
			}

			public double yd() {
				return this.yd;
			}

			public double zd() {
				return this.zd;
			}
		}
	}
}