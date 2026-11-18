package com.luckyowlstudios.codex;

import com.luckyowlstudios.codex.brewing.BrewingKnowledge;
import com.luckyowlstudios.codex.item.ModItems;
import com.luckyowlstudios.codex.network.SyncKnownBrewsPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/** Common (client and server) setup: the Potion Scroll, unlock syncing and the /brewbook command. */
public class CodexFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Item scroll = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, ModItems.POTION_SCROLL_NAME),
                ModItems.createPotionScroll());
        ModItems.POTION_SCROLL = () -> scroll;
        Item tradedScroll = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, ModItems.TRADED_POTION_SCROLL_NAME),
                ModItems.createTradedPotionScroll());
        ModItems.TRADED_POTION_SCROLL = () -> tradedScroll;
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            entries.accept(scroll);
            entries.accept(tradedScroll);
        });
        // Level 1 is the wandering trader's common pool (level 2 is its single rare slot).
        TradeOfferHelper.registerWanderingTraderOffers(1, offers -> offers.add((trader, random) -> ModItems.wanderingTraderOffer()));

        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (source.isBuiltin() && key.equals(EntityType.WITCH.getDefaultLootTable())) {
                tableBuilder.withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(NestedLootTable.lootTableReference(ModItems.WITCH_SCROLL_LOOT)));
            }
        });

        PayloadTypeRegistry.playS2C().register(SyncKnownBrewsPayload.TYPE, SyncKnownBrewsPayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> BrewingKnowledge.sync(handler.player));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> BrewBookCommand.register(dispatcher));
    }
}
