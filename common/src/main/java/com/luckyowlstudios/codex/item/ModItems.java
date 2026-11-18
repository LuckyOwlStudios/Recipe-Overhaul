package com.luckyowlstudios.codex.item;

import com.luckyowlstudios.codex.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

/** Items added by the mod. Each loader registers them and fills in the suppliers. */
public final class ModItems {
    public static final String POTION_SCROLL_NAME = "potion_scroll";

    /**
     * Extra witch drops: a Potion Scroll, 10% when killed by a player, 15/20/25% with Looting I/II/III.
     * Fabric adds it to the witch's loot table; NeoForge uses a global loot modifier.
     */
    public static final ResourceKey<LootTable> WITCH_SCROLL_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "entities/witch_potion_scroll"));

    public static Supplier<Item> POTION_SCROLL = () -> {
        throw new IllegalStateException("Potion Scroll used before registration");
    };

    private ModItems() {
    }

    public static Item createPotionScroll() {
        return new PotionScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }
}
