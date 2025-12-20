package com.damir00109.item;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item.Settings;

import com.damir00109.RadioMod;
import com.damir00109.RadioState;
import com.damir00109.block.ModBlocks;

public final class ModItems {

    private static final List<Pair<Identifier, Item>> ITEMS_TO_REGISTER = new ArrayList<>();

    public static final RadioItem RADIO = registerOnInit(
            "radio",
            new Settings()
                    .maxCount(64)
                    .component(ModItems.DataComponents.RADIO_STATE, RadioState.DISABLED),
            settings -> new RadioItem(ModBlocks.RADIO, settings)
    );


    public static void init() {
        ModItems.DataComponents.init();

        ITEMS_TO_REGISTER.forEach(pair ->
                Registry.register(
                        Registries.ITEM,
                        pair.getFirst(),
                        pair.getSecond()
                )
        );
        ITEMS_TO_REGISTER.clear();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register((ModifyEntries) entries ->
                        entries.addAfter(Items.COMPASS, RADIO)
                );
    }

    private static <T extends Item> T registerOnInit(
            String radioModId,
            Settings settings,
            Function<Settings, T> factory
    ) {
        Identifier id = RadioMod.id(radioModId);

        settings.registryKey(RegistryKey.of(Registries.ITEM.getKey(), id));

        T item = factory.apply(settings);
        ITEMS_TO_REGISTER.add(new Pair<>(id, item));
        return item;
    }

    public static final class DataComponents {

        public static final ComponentType<RadioState> RADIO_STATE =
                ComponentType.<RadioState>builder()
                        .codec(RadioState.CODEC)
                        .packetCodec(RadioState.PACKET_CODEC)
                        .build();

        private static void init() {
            Registry.register(
                    Registries.DATA_COMPONENT_TYPE,
                    RadioMod.id("radio_state"),
                    RADIO_STATE
            );
        }
    }
}
