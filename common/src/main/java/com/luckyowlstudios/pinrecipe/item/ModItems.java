package com.luckyowlstudios.pinrecipe.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

/** Items added by the mod. Each loader registers them and fills in the suppliers. */
public final class ModItems {
    public static final String POTION_SCROLL_NAME = "potion_scroll";

    public static Supplier<Item> POTION_SCROLL = () -> {
        throw new IllegalStateException("Potion Scroll used before registration");
    };

    private ModItems() {
    }

    public static Item createPotionScroll() {
        return new PotionScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }
}
