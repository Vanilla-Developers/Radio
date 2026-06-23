package com.damir00109.item;

import com.damir00109.RadioState;
import com.damir00109.item.ModItems.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class RadioItem extends BlockItem {

    private static final Component DESTROYED_TEXT =
            Component.translatable("block.vpl.radio.destroyed");

    public RadioItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.get(DataComponents.RADIO_STATE) == RadioState.DESTROYED) {
            return DESTROYED_TEXT;
        }
        return Component.translatable(this.getBlock().getDescriptionId());
    }
}
