package com.damir00109.item;

import com.damir00109.VanillaDamir00109;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import java.util.function.Function;
import net.minecraft.component.type.FoodComponent;
import com.damir00109.zona.PlayerState;
import com.damir00109.zona.CustomBorderManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

public class ModItems {

    public static final Item CHAMOMILE_SOUP = registerItem("chamomile_soup",
        ChamomileSoupItem::new,
        new Item.Settings()
            .maxCount(1)
            .food(new FoodComponent.Builder().nutrition(6).saturationModifier(0.6f).build())
    );

    private static <T extends Item> T registerItem(String path, Function<Item.Settings, T> itemFactory, Item.Settings settings) {
        RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(VanillaDamir00109.MOD_ID, path));
        settings.registryKey(registryKey); // Устанавливаем RegistryKey в настройки ПЕРЕД созданием предмета
        T item = itemFactory.apply(settings); // Создаем предмет, используя фабрику и обновленные настройки
        return Registry.register(Registries.ITEM, registryKey.getValue(), item); // Регистрируем предмет
    }

    public static void registerModItems() {
        VanillaDamir00109.LOGGER.info("Registering Mod Items for " + VanillaDamir00109.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(content -> {
            content.add(CHAMOMILE_SOUP);
        });
    }

    public static class ChamomileSoupItem extends Item {

        public ChamomileSoupItem(Settings settings) {
            super(settings);
        }

        @Override
        public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
            super.finishUsing(stack, world, user);
            if (user instanceof ServerPlayerEntity) {
                ServerPlayerEntity playerEntity = (ServerPlayerEntity) user;
                // Добавляем эффект регенерации
                if (!world.isClient) {
                    playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 5 * 20, 0)); // 5 секунд, усилитель 0 (Регенерация I)

                    // Логика ментального здоровья
                    PlayerState state = CustomBorderManager.getPlayerState(playerEntity.getUuid());
                    if (state != null) {
                        state.setConsciousness(Math.min(100, state.getConsciousness() + 10));
                        state.setAteChamomileSoupRecently(true);
                        state.setTimeWhenSoupWasEaten(world.getTime()); // Сохраняем текущий мировой тик
                        CustomBorderManager.saveConsciousnessData(playerEntity.getServer(), CustomBorderManager.getAllPlayerStates()); // Сохраняем все состояния
                        VanillaDamir00109.LOGGER.info("Player {} ate Chamomile Soup. Consciousness: {}, AteRecently: true, TimeEaten: {}",
                            playerEntity.getName().getString(), state.getConsciousness(), state.getTimeWhenSoupWasEaten());
                    } else {
                        VanillaDamir00109.LOGGER.warn("PlayerState not found for {} after eating soup.", playerEntity.getName().getString());
                    }
                }
                // Возвращаем пустую миску, если это игрок
                if (!playerEntity.getAbilities().creativeMode) {
                    if (stack.isEmpty()) {
                        return new ItemStack(Items.BOWL);
                    }
                    playerEntity.getInventory().insertStack(new ItemStack(Items.BOWL));
                }
            }
            return stack;
        }
    }
} 