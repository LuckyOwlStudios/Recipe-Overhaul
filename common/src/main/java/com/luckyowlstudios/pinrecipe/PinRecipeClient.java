package com.luckyowlstudios.pinrecipe;

import com.luckyowlstudios.pinrecipe.client.BrewingRecipeBook;
import com.luckyowlstudios.pinrecipe.mixin.RecipeBookComponentAccessor;
import com.luckyowlstudios.pinrecipe.mixin.RecipeBookPageAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Loader-independent client logic. Each loader forwards its screen events here.
 */
public final class PinRecipeClient {
    public static final String KEY_CATEGORY = "key.categories." + Constants.MOD_ID;

    /** Press while hovering a recipe in the recipe book to pin or unpin it. */
    public static final KeyMapping PIN_KEY = new KeyMapping(
            "key." + Constants.MOD_ID + ".pin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KEY_CATEGORY
    );

    @Nullable
    private static Screen lastInitScreen;

    private PinRecipeClient() {
    }

    /** Called after a screen renders. Draws the pinned recipes on the inventory screen only. */
    public static void onScreenRender(Screen screen, net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        PinnedRecipeOverlay.render(screen, graphics, mouseX, mouseY);
        BrewingRecipeBook.renderForeground(screen, graphics, mouseX, mouseY);
    }

    /** Called after a screen is initialised, which happens when it opens and again on every resize. */
    public static void onScreenInit(Screen screen) {
        if (screen != lastInitScreen) {
            lastInitScreen = screen;
            BrewingRecipeBook.onScreenOpened();
        }
        BrewingRecipeBook.onScreenInit(screen);
    }

    /** Returns true if the release was used and the screen should not see it. */
    public static boolean onMouseReleased(Screen screen, double mouseX, double mouseY, int button) {
        return BrewingRecipeBook.mouseReleased(screen, mouseX, mouseY, button);
    }

    /** Returns true if the key press was used and the screen should not see it. */
    public static boolean onKeyPressed(Screen screen, int keyCode, int scanCode) {
        if (PIN_KEY.isUnbound() || !PIN_KEY.matches(keyCode, scanCode)) {
            return false;
        }
        return tryTogglePin(screen);
    }

    /** Returns true if the click was used and the screen should not see it. */
    public static boolean onMouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (PinnedRecipeOverlay.mouseClicked(screen, mouseX, mouseY, button)) {
            return true;
        }
        if (BrewingRecipeBook.mouseClicked(screen, mouseX, mouseY, button)) {
            return true;
        }
        if (!PIN_KEY.isUnbound() && PIN_KEY.matchesMouse(button)) {
            return tryTogglePin(screen);
        }
        return false;
    }

    private static boolean tryTogglePin(Screen screen) {
        RecipeHolder<?> recipe = getHoveredRecipe(screen);
        if (recipe == null) {
            return false;
        }
        PinnedRecipes.toggle(recipe.id());
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    @Nullable
    private static RecipeHolder<?> getHoveredRecipe(Screen screen) {
        if (!(screen instanceof RecipeUpdateListener listener)) {
            return null;
        }
        RecipeBookComponent book = listener.getRecipeBookComponent();
        if (book == null || !book.isVisible()) {
            return null;
        }
        RecipeBookComponentAccessor bookAccess = (RecipeBookComponentAccessor) book;
        EditBox search = bookAccess.pinrecipe$getSearchBox();
        if (search != null && search.isFocused()) {
            // The player is typing in the search box; let the key through.
            return null;
        }
        RecipeButton hovered = ((RecipeBookPageAccessor) bookAccess.pinrecipe$getRecipeBookPage()).pinrecipe$getHoveredButton();
        if (hovered == null) {
            return null;
        }
        return hovered.getRecipe();
    }
}
