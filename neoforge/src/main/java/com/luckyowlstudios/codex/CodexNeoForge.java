package com.luckyowlstudios.codex;

import com.luckyowlstudios.codex.brewing.BrewingKnowledge;
import com.luckyowlstudios.codex.brewing.ClientBrewingKnowledge;
import com.luckyowlstudios.codex.item.ModItems;
import com.luckyowlstudios.codex.network.SyncKnownBrewsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Common (client and server) setup: the Potion Scroll, unlock syncing and the /brewbook command. */
@Mod(Constants.MOD_ID)
public class CodexNeoForge {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final DeferredItem<Item> POTION_SCROLL = ITEMS.register(ModItems.POTION_SCROLL_NAME, ModItems::createPotionScroll);
    private static final DeferredItem<Item> TRADED_POTION_SCROLL =
            ITEMS.register(ModItems.TRADED_POTION_SCROLL_NAME, ModItems::createTradedPotionScroll);

    public CodexNeoForge(IEventBus modBus) {
        ITEMS.register(modBus);
        ModItems.POTION_SCROLL = POTION_SCROLL;
        ModItems.TRADED_POTION_SCROLL = TRADED_POTION_SCROLL;

        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
                event.accept(POTION_SCROLL);
                event.accept(TRADED_POTION_SCROLL);
            }
        });

        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToClient(SyncKnownBrewsPayload.TYPE, SyncKnownBrewsPayload.STREAM_CODEC,
                        (payload, context) -> ClientBrewingKnowledge.handle(payload)));

        NeoForge.EVENT_BUS.addListener(WandererTradesEvent.class,
                event -> event.getGenericTrades().add((trader, random) -> ModItems.wanderingTraderOffer()));

        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                BrewingKnowledge.sync(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> BrewBookCommand.register(event.getDispatcher()));
    }
}
