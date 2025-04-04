package com.damir00109;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;

public class DModBlocks {
    public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class);

    public static final Block BURNT_RADIO = registerBlock(
            "burnt_radio",
            new BurntRadioBlock(Block.Settings.create()
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .mapColor(MapColor.ORANGE)
                    .strength(3.0f)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK,
                            Identifier.of(VanillaDamir00109.MOD_ID, "burnt_radio")))
            )
    );

    private static Block registerBlock(String name, Block block) {
        Identifier id = Identifier.of(VanillaDamir00109.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
        ));
        return block;
    }

    public static void registerModBlocks() {
        VanillaDamir00109.LOGGER.info("Registering Mod Blocks for " + VanillaDamir00109.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(BURNT_RADIO);
        });
    }

    public static class BurntRadioBlock extends Block {
        public BurntRadioBlock(Settings settings) {
            super(settings);
            this.setDefaultState(this.stateManager.getDefaultState()
                    .with(FACING, Direction.NORTH));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return this.getDefaultState()
                    .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
        }
    }
}