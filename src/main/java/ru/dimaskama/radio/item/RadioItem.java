package ru.dimaskama.radio.item;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.text.Text;

import ru.dimaskama.radio.RadioState;
import ru.dimaskama.radio.item.ModItems.DataComponents;

public class RadioItem extends BlockItem {

    private static final Text DESTROYED_TEXT =
            Text.translatable("block.radio.radio.destroyed");

    public RadioItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        if (stack.get(DataComponents.RADIO_STATE) == RadioState.DESTROYED) {
            return DESTROYED_TEXT;
        }
        return Text.translatable(this.getBlock().getTranslationKey());
    }
}
