package ru.dimaskama.radio.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.block.ModBlocks.Properties;
import ru.dimaskama.radio.blockentity.ModBlockEntities;
import ru.dimaskama.radio.blockentity.RadioBlockEntity;
import ru.dimaskama.radio.item.ModItems;
import ru.dimaskama.radio.item.ModItems.DataComponents;

public class RadioBlock extends AbstractBlock {
    private final MapCodec<RadioBlock> CODEC = createCodec(RadioBlock::new);

    protected RadioBlock(Settings settings) {
        super(settings);
        this.method_9590(
                (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.method_9595().method_11664()).method_11657(Properties.RADIO_STATE, RadioState.DISABLED))
                        .method_11657(Properties.LEFT_INDICATOR, false))
                        .method_11657(class_2741.field_12511, 0))
                        .method_11657(class_2741.field_12481, class_2350.field_11043)
        );
    }

    protected MapCodec<RadioBlock> method_53969() {
        return this.CODEC;
    }

    @Nullable
    public class_2586 method_10123(BlockPos pos, BlockState state) {
        return new RadioBlockEntity(pos, state);
    }

    @Nullable
    public <T extends class_2586> class_5558<T> method_31645(class_1937 world, BlockState state, class_2591<T> type) {
        return method_31618(type, ModBlockEntities.RADIO_TYPE, (world1, pos, state1, blockEntity) -> blockEntity.tick(world1, pos, state1));
    }

    protected void method_9615(BlockState state, class_1937 world, BlockPos pos, BlockState oldState, boolean notify) {
        super.method_9615(state, world, pos, oldState, notify);
        if (!oldState.method_27852(state.method_26204()) && world instanceof ServerWorld serverWorld) {
            this.update(pos, state, serverWorld);
        }
    }

    protected void method_9612(BlockState state, class_1937 world, BlockPos pos, class_2248 sourceBlock, @Nullable class_9904 wireOrientation, boolean notify) {
        super.method_9612(state, world, pos, sourceBlock, wireOrientation, notify);
        if (world instanceof ServerWorld serverWorld) {
            this.update(pos, state, serverWorld);
        }
    }

    private void update(BlockPos pos, BlockState state, ServerWorld world) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity blockEntity) {
            BlockState s = blockEntity.updateState(pos, state, world, false);
            if (s != null) {
                world.method_8652(pos, s, 2);
            }
        }
    }

    protected class_1269 method_55766(BlockState state, class_1937 world, BlockPos pos, class_1657 player, class_3965 hit) {
        return this.tryToggle(state, world, pos);
    }

    protected void method_55124(BlockState state, ServerWorld world, BlockPos pos, class_1927 explosion, BiConsumer<class_1799, BlockPos> stackMerger) {
        if (explosion.method_60274()) {
            this.tryToggle(state, world, pos);
        }

        super.method_55124(state, world, pos, explosion, stackMerger);
    }

    protected class_1269 tryToggle(BlockState state, class_1937 world, BlockPos pos) {
        return (class_1269)(world instanceof ServerWorld serverWorld && world.getBlockEntity(pos) instanceof RadioBlockEntity blockEntity
                ? blockEntity.tryToggle(serverWorld, pos, state)
                : class_1269.field_21466);
    }

    protected class_1269 method_55765(class_1799 stack, BlockState state, class_1937 world, BlockPos pos, class_1657 player, class_1268 hand, class_3965 hit) {
        return (class_1269)(stack.method_7909() instanceof class_1747 && new class_1750(player, hand, stack, hit).method_7716()
                ? class_1269.field_5811
                : class_1269.field_52423);
    }

    protected boolean method_9498(BlockState state) {
        return true;
    }

    protected int method_9572(BlockState state, class_1937 world, BlockPos pos, class_2350 direction) {
        return world.getBlockEntity(pos) instanceof RadioBlockEntity radioBlockEntity ? radioBlockEntity.getComparatorOutput() : 0;
    }

    @Nullable
    public BlockState method_9605(class_1750 ctx) {
        return (BlockState)((BlockState)this.method_9564().method_11657(class_2741.field_12481, ctx.method_8042().method_10153()))
                .method_11657(Properties.RADIO_STATE, (RadioState)ctx.method_8041().method_58695(DataComponents.RADIO_STATE, RadioState.DISABLED));
    }

    protected class_1799 method_9574(class_4538 world, BlockPos pos, BlockState state, boolean includeData) {
        class_1799 stack = super.method_9574(world, pos, state, includeData);
        RadioState radioState = (RadioState)state.method_11654(Properties.RADIO_STATE);
        stack.method_57379(DataComponents.RADIO_STATE, radioState == RadioState.DESTROYED ? RadioState.DESTROYED : RadioState.DISABLED);
        return stack;
    }

    protected List<class_1799> method_9560(BlockState state, class_8568 builder) {
        RadioState radioState = (RadioState)state.method_11654(Properties.RADIO_STATE);
        List<class_1799> list = super.method_9560(state, builder);

        for (class_1799 stack : list) {
            if (stack.method_31574(ModItems.RADIO)) {
                stack.method_57379(DataComponents.RADIO_STATE, radioState == RadioState.DESTROYED ? RadioState.DESTROYED : RadioState.DISABLED);
            }
        }

        return list;
    }

    protected BlockState rotate(BlockState state, class_2470 rotation) {
        return (BlockState)state.method_11657(class_2741.field_12481, rotation.method_10503((class_2350)state.method_11654(class_2741.field_12481)));
    }

    protected BlockState method_9569(BlockState state, class_2415 mirror) {
        return this.rotate(state, mirror.method_10345((class_2350)state.method_11654(class_2741.field_12481)));
    }

    protected void appendProperties(class_2690<class_2248, BlockState> builder) {
        super.appendProperties(builder);
        builder.method_11667(new class_2769[]{Properties.RADIO_STATE, Properties.LEFT_INDICATOR, class_2741.field_12511, class_2741.field_12481});
    }

    public void precipitationTick(BlockState state, class_1937 world, BlockPos pos, class_1963 precipitation) {
        super.precipitationTick(state, world, pos, precipitation);
    }

    protected void tick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radioBlockEntity) {
            state = radioBlockEntity.newBurnedState(world, pos, state);
            if (state != null) {
                world.method_8652(pos, state, 2);
                world.method_43128(
                        null, pos.method_10263() + 0.5, pos.method_10264() + 0.5, pos.method_10260() + 0.5, class_3417.field_19199, class_3419.field_15245, 0.5F, 0.7F
                );
            }
        }
    }

    public static void burnRadio(ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radioBlockEntity) {
            radioBlockEntity.markBurning();
            world.setBlockState(pos, ModBlocks.RADIO, 6);
        }
    }
}
