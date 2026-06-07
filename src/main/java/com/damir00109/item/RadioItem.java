package com.damir00109.item;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.text.Text;

import com.damir00109.RadioState;
import com.damir00109.item.ModItems.DataComponents;

public class RadioItem extends BlockItem {

    private static final Text DESTROYED_TEXT =
            Text.translatable("block.vpl.radio.destroyed");

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
