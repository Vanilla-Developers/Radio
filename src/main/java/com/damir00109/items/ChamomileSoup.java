package com.damir00109.items;

import com.damir00109.vpl;
import com.damir00109.zona.CustomBorderManager;
import com.damir00109.zona.PlayerState;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ChamomileSoup {

    public static final Item CHAMOMILE_SOUP_ITEM = registerChamomileSoup();

    private static Item registerChamomileSoup() {
        Item.Settings settings = new Item.Settings()
            .maxCount(1)
            .food(new FoodComponent.Builder().nutrition(6).saturationModifier(0.6f).build());
        // Установка RegistryKey в настройки перед созданием предмета
        RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(vpl.MOD_ID, "chamomile_soup"));
        settings.registryKey(registryKey);
        
        Item item = new ChamomileSoupItemClass(settings);
        return Registry.register(Registries.ITEM, registryKey.getValue(), item);
    }

    public static void addChamomileSoupToItemGroup() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(content -> {
            content.add(CHAMOMILE_SOUP_ITEM);
        });
    }

    private static class ChamomileSoupItemClass extends Item {
        public ChamomileSoupItemClass(Settings settings) {
            super(settings);
        }

        @Override
        public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
            super.finishUsing(stack, world, user);
            if (user instanceof ServerPlayerEntity) {
                ServerPlayerEntity playerEntity = (ServerPlayerEntity) user;
                if (!world.isClient) {
                    playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 5 * 20, 0));

                    PlayerState state = CustomBorderManager.getPlayerState(playerEntity.getUuid());
                    if (state != null) {
                        state.setConsciousness(Math.min(100, state.getConsciousness() + 10));
                        state.setAteChamomileSoupRecently(true);
                        state.setTimeWhenSoupWasEaten(world.getTime());
                        if (playerEntity.getServer() != null) {
                            CustomBorderManager.saveConsciousnessData(playerEntity.getServer(), CustomBorderManager.getAllPlayerStates());
                        }
                        vpl.LOGGER.info("Player {} ate Chamomile Soup. Consciousness: {}, AteRecently: true, TimeEaten: {}",
                            playerEntity.getName().getString(), state.getConsciousness(), state.getTimeWhenSoupWasEaten());
                    } else {
                        vpl.LOGGER.warn("PlayerState not found for {} after eating soup.", playerEntity.getName().getString());
                    }
                }
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