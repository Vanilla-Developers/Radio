package com.damir00109.item;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else
//import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
//? if <26.1 {
/*import net.minecraft.world.item.Items;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries;
*///?}
import com.damir00109.RadioMod;
import com.damir00109.RadioState;
import com.damir00109.block.ModBlocks;

public final class ModItems {

    //? if >=1.21.11 {
    private static final List<Pair<Identifier, Item>> ITEMS_TO_REGISTER = new ArrayList<>();
    //?} else
    //private static final List<Pair<ResourceLocation, Item>> ITEMS_TO_REGISTER = new ArrayList<>();

    public static final RadioItem RADIO = registerOnInit(
            "radio",
            new Properties()
                    .stacksTo(64)
                    .component(ModItems.DataComponents.RADIO_STATE, RadioState.DISABLED),
            settings -> new RadioItem(ModBlocks.RADIO, settings)
    );


    public static void init() {
        ModItems.DataComponents.init();

        ITEMS_TO_REGISTER.forEach(pair ->
                Registry.register(
                        BuiltInRegistries.ITEM,
                        pair.getFirst(),
                        pair.getSecond()
                )
        );
        ITEMS_TO_REGISTER.clear();

        //? if <26.1 {
        /*ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register((ModifyEntries) entries ->
                        entries.addAfter(Items.COMPASS, RADIO)
                );
        *///?}
        // ItemGroupEvents API was removed/changed in Fabric 0.145.1+26.1
        // Creative tab entry will need to be added through a client-side event or configuration
        // TODO: Implement creative tab integration for version 26.1+
    }

    private static <T extends Item> T registerOnInit(
            String radioModId,
            Properties settings,
            Function<Properties, T> factory
    ) {
        //? if >=1.21.11 {
        Identifier id = RadioMod.id(radioModId);
        //?} else
        //ResourceLocation id = RadioMod.id(radioModId);

        settings.setId(ResourceKey.create(BuiltInRegistries.ITEM.key(), id));

        T item = factory.apply(settings);
        ITEMS_TO_REGISTER.add(new Pair<>(id, item));
        return item;
    }

    public static final class DataComponents {

        public static final DataComponentType<RadioState> RADIO_STATE =
                DataComponentType.<RadioState>builder()
                        .persistent(RadioState.CODEC)
                        .networkSynchronized(RadioState.PACKET_CODEC)
                        .build();

        private static void init() {
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    RadioMod.id("radio_state"),
                    RADIO_STATE
            );
        }
    }
}
