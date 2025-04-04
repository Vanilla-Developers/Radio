package com.damir00109;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class DModBlocks {
    public static final Block COPPER_BLOCK = registerBlock(
            "burnt_radio",
            new Block(
                    Block.Settings.copy(Blocks.COPPER_BLOCK)
                            .mapColor(MapColor.ORANGE)
                            .strength(3.0f)
                            .pistonBehavior(PistonBehavior.BLOCK)
                            .registryKey(RegistryKey.of(
                                    RegistryKeys.BLOCK,
                                    Identifier.of(VanillaDamir00109.MOD_ID, "burnt_radio")
                            ))
            )
    );

    private static Block registerBlock(String name, Block block) {
        Identifier id = Identifier.of(VanillaDamir00109.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id,
                new BlockItem(block, new Item.Settings()
                        .registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
                )
        );
        return block;
    }

    public static void registerBlocks() {
        VanillaDamir00109.LOGGER.info("Registering Mod Blocks for " + VanillaDamir00109.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(COPPER_BLOCK);
        });
    }
}