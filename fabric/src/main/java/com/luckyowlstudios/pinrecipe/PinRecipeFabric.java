package com.luckyowlstudios.pinrecipe;

import com.luckyowlstudios.pinrecipe.brewing.BrewingKnowledge;
import com.luckyowlstudios.pinrecipe.item.ModItems;
import com.luckyowlstudios.pinrecipe.network.SyncKnownBrewsPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Common (client and server) setup: the Potion Scroll, unlock syncing and the /brewbook command. */
public class PinRecipeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Item scroll = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, ModItems.POTION_SCROLL_NAME),
                ModItems.createPotionScroll());
        ModItems.POTION_SCROLL = () -> scroll;
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> entries.accept(scroll));

        PayloadTypeRegistry.playS2C().register(SyncKnownBrewsPayload.TYPE, SyncKnownBrewsPayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> BrewingKnowledge.sync(handler.player));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> BrewBookCommand.register(dispatcher));
    }
}
