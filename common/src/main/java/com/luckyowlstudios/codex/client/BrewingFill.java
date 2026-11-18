package com.luckyowlstudios.codex.client;

import com.luckyowlstudios.codex.brewing.BrewingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Moves a brewing recipe's bottles and ingredient from the player's inventory into the brewing stand.
 * The brewing stand has no vanilla "place recipe" request, so this uses ordinary inventory clicks:
 * exactly what a player would do by hand, checked by the server like any other click.
 * Anything the player is missing is shown as a ghost item instead.
 */
public final class BrewingFill {
    private static final int BOTTLE_SLOTS = 3;
    private static final int INGREDIENT_SLOT = 3;
    /** Bottles 0-2, ingredient 3, blaze powder 4, then the player's inventory. */
    private static final int FIRST_PLAYER_SLOT = 5;

    private BrewingFill() {
    }

    public static void fill(BrewingStandScreen screen, BrewingRecipe recipe) {
        BrewingRecipeBook.showGhost(recipe);
        Minecraft mc = Minecraft.getInstance();
        MultiPlayerGameMode gameMode = mc.gameMode;
        Player player = mc.player;
        BrewingStandMenu menu = screen.getMenu();
        if (gameMode == null || player == null || !menu.getCarried().isEmpty()) {
            return; // Don't touch anything while the player is holding an item on the cursor.
        }

        fillSlot(gameMode, player, menu, INGREDIENT_SLOT, recipe::ingredientMatches);
        for (int slot = 0; slot < BOTTLE_SLOTS; slot++) {
            fillSlot(gameMode, player, menu, slot, recipe.inputTest());
        }
    }

    /** Makes the slot hold one matching item: clears out a wrong item, then places one from the inventory. */
    private static void fillSlot(MultiPlayerGameMode gameMode, Player player, BrewingStandMenu menu, int target,
                                 Predicate<ItemStack> matches) {
        Slot slot = menu.getSlot(target);
        if (slot.hasItem() && matches.test(slot.getItem())) {
            return;
        }
        if (slot.hasItem()) {
            click(gameMode, player, menu, target, 0, ClickType.QUICK_MOVE);
            if (slot.hasItem()) {
                return; // Inventory full: leave it.
            }
        }
        int source = findInInventory(menu, matches);
        if (source < 0) {
            return;
        }
        click(gameMode, player, menu, source, 0, ClickType.PICKUP);   // pick up the stack
        click(gameMode, player, menu, target, 1, ClickType.PICKUP);   // right-click: place one
        if (!menu.getCarried().isEmpty()) {
            click(gameMode, player, menu, source, 0, ClickType.PICKUP); // put the rest back
        }
    }

    private static int findInInventory(BrewingStandMenu menu, Predicate<ItemStack> matches) {
        for (int i = FIRST_PLAYER_SLOT; i < menu.slots.size(); i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty() && matches.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    private static void click(MultiPlayerGameMode gameMode, Player player, BrewingStandMenu menu, int slot, int button, ClickType type) {
        gameMode.handleInventoryMouseClick(menu.containerId, slot, button, type, player);
    }
}
