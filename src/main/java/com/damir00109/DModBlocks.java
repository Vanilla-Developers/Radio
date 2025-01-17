package com.damir00109;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.item.ItemPlacementContext;
import org.jetbrains.annotations.Nullable;

public class DModBlocks {

    public static final IntProperty POWER = IntProperty.of("power", 0, 15);
    public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class);
    public static final BooleanProperty LISTEN = BooleanProperty.of("listen"); // Изменяем тип на BooleanProperty

    public static final Block RADIO = registerBlock("radio", new RadioBlock(Block.Settings.create()
            .mapColor(MapColor.STONE_GRAY)
            .strength(1.5f)
            .pistonBehavior(PistonBehavior.BLOCK)
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(VanillaDamir00109.MOD_ID, "radio")))
    ));

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
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.add(RADIO);
        });

        // Регистрируем обработчик события размещения блока
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (state.isOf(Blocks.LIGHTNING_ROD)) {
                BlockPos belowPos = pos.down();
                BlockState belowState = world.getBlockState(belowPos);

                if (belowState.isOf(RADIO)) {
                    // Проверяем, что громоотвод не появился с одной из сторон блока
                    boolean isValidPlacement = true;
                    for (Direction direction : Direction.values()) {
                        if (direction != Direction.UP && direction != Direction.DOWN) {
                            BlockPos sidePos = belowPos.offset(direction);
                            BlockState sideState = world.getBlockState(sidePos);
                            if (sideState.isOf(Blocks.LIGHTNING_ROD)) {
                                isValidPlacement = false;
                                break;
                            }
                        }
                    }

                    if (isValidPlacement) {
                        world.setBlockState(belowPos, belowState.with(POWER, 15).with(LISTEN, true), 2); // Устанавливаем listen в true
                    }
                }
            }

            return ActionResult.PASS;
        });

        // Регистрируем обработчик события разрушения блока
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (state.isOf(Blocks.LIGHTNING_ROD)) {
                BlockPos belowPos = pos.down();
                BlockState belowState = world.getBlockState(belowPos);

                if (belowState.isOf(RADIO)) {
                    world.setBlockState(belowPos, belowState.with(POWER, 0).with(LISTEN, false), 2); // Устанавливаем listen в false
                }
            }

            return true;
        });
    }

    public static class RadioBlock extends Block {
        public RadioBlock(Settings settings) {
            super(settings);
            this.setDefaultState(this.stateManager.getDefaultState()
                    .with(POWER, 0)
                    .with(FACING, Direction.EAST)
                    .with(LISTEN, false)); // Устанавливаем значение по умолчанию для параметра listen
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
            builder.add(POWER, FACING, LISTEN); // Добавляем параметр listen
        }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return this.getDefaultState()
                    .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                    .with(POWER, ctx.getWorld().getReceivedRedstonePower(ctx.getBlockPos()))
                    .with(LISTEN, false); // Устанавливаем значение по умолчанию для параметра listen
        }

        @Override
        public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
            if (!world.isClient) {
                // Проверяем, был ли установлен lightning_rod сверху
                BlockPos abovePos = pos.up();  // Позиция сверху

                BlockState aboveState = world.getBlockState(abovePos);
                if (aboveState.isOf(Blocks.LIGHTNING_ROD)) {
                    // Проверяем, что громоотвод был установлен на блоке радио
                    if (world.getBlockState(pos).getBlock() == RADIO) {
                        // Отправляем сообщение всем игрокам в мире
                        world.getPlayers().forEach(player -> player.sendMessage(
                                Text.literal("На блоке Radio был установлен Lightning Rod сверху!"),
                                false
                        ));
                    }
                }

                // Обновляем мощность, если сигнал редстоуна изменился
                int currentPower = state.get(POWER);
                int newPower = world.getReceivedRedstonePower(pos);

                if (currentPower != newPower) {
                    world.setBlockState(pos, state.with(POWER, newPower), 2);
                }
            }
        }
    }
}