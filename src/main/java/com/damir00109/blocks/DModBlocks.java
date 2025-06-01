package com.damir00109.blocks;

import com.damir00109.vpl;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.*;

public class DModBlocks {
    // Горизонтальные направления
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    // Регистрация блоков статически
    public static final Block RADIO = registerBlock(
            "radio",
            settings -> new Radio.RadioBlock(settings),
            Block.Settings.create()
                    .mapColor(MapColor.STONE_GRAY)
                    .strength(1.5f)
                    .pistonBehavior(PistonBehavior.BLOCK)
    );

    public static final Block BURNT_RADIO = registerBlock(
            "burnt_radio",
            settings -> new BurntRadioBlock(settings),
            Block.Settings.create()
                    .mapColor(MapColor.ORANGE)
                    .strength(3.0f)
                    .pistonBehavior(PistonBehavior.BLOCK)
    );

    // Универсальный метод регистрации, устанавливает registryKey до создания блока и Item
    private static Block registerBlock(
            String name,
            java.util.function.Function<Block.Settings, Block> factory,
            Block.Settings baseSettings
    ) {
        Identifier id = Identifier.of(vpl.MOD_ID, name);
        // Регистрируем блок со своим registryKey
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
        baseSettings = baseSettings.registryKey(blockKey);
        Block block = factory.apply(baseSettings);
        Registry.register(Registries.BLOCK, id, block);

        // Регистрируем Item для блока с корректным registryKey
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings itemSettings = new Item.Settings().registryKey(itemKey);
        Registry.register(
                Registries.ITEM,
                id,
                new BlockItem(block, itemSettings)
        );

        return block;
    }

    public static void registerModBlocks() {
        vpl.LOGGER.info("Registering Mod Blocks for " + vpl.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE)
                .register(entries -> {
                    entries.add(BURNT_RADIO);
                    entries.add(RADIO);
                });
    }

    // Класс блока BurntRadio
    public static class BurntRadioBlock extends Block {
        public BurntRadioBlock(Settings settings) {
            super(settings);
            setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, net.minecraft.block.BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public net.minecraft.block.BlockState getPlacementState(ItemPlacementContext ctx) {
            return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
        }
    }
}
