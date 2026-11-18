package com.luckyowlstudios.codex.item;

import com.luckyowlstudios.codex.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

/** Items added by the mod. Each loader registers them and fills in the suppliers. */
public final class ModItems {
    public static final String POTION_SCROLL_NAME = "potion_scroll";
    /** Same as the Potion Scroll, but sold by the wandering trader instead of crafted or dropped. */
    public static final String TRADED_POTION_SCROLL_NAME = "traded_potion_scroll";

    /** Wandering trader offer: 1 scroll for this many emeralds, up to {@link #TRADE_MAX_USES} times per trader. */
    public static final int TRADE_EMERALDS = 5;
    public static final int TRADE_MAX_USES = 4;

    /**
     * Extra witch drops: a Potion Scroll, 25% when killed by a player, 35/45/55% with Looting I/II/III.
     * Fabric adds it to the witch's loot table; NeoForge uses a global loot modifier.
     */
    public static final ResourceKey<LootTable> WITCH_SCROLL_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "entities/witch_potion_scroll"));

    public static Supplier<Item> POTION_SCROLL = () -> {
        throw new IllegalStateException("Potion Scroll used before registration");
    };

    public static Supplier<Item> TRADED_POTION_SCROLL = () -> {
        throw new IllegalStateException("Wanderer's Potion Scroll used before registration");
    };

    private ModItems() {
    }

    public static Item createPotionScroll() {
        return new PotionScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    public static Item createTradedPotionScroll() {
        return new PotionScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    }

    /** The wandering trader's offer, shared by both loaders. */
    public static MerchantOffer wanderingTraderOffer() {
        return new MerchantOffer(new ItemCost(Items.EMERALD, TRADE_EMERALDS), new ItemStack(TRADED_POTION_SCROLL.get()),
                TRADE_MAX_USES, 1, 0.05F);
    }
}
