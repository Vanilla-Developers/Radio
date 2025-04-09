package com.damir00109;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

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
}