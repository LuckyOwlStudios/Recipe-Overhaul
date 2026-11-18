package com.luckyowlstudios.pinrecipe.client;

import com.luckyowlstudios.pinrecipe.brewing.BrewingRecipe;
import com.luckyowlstudios.pinrecipe.brewing.BrewingRecipes;
import com.luckyowlstudios.pinrecipe.brewing.ClientBrewingKnowledge;
import com.luckyowlstudios.pinrecipe.mixin.AbstractContainerScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe book for the brewing stand, laid out like the vanilla one: a toggle button on the brewing
 * stand, and a 5x4 grid of the recipes the player has unlocked with Potion Scrolls. Hovering a recipe
 * shows what it needs; clicking one shows its ingredients as ghost items in the brewing stand's slots.
 */
public final class BrewingRecipeBook {
    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("recipe_book/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/button_highlighted");
    private static final ResourceLocation SLOT_CRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_craftable");
    private static final ResourceLocation SLOT_UNCRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_uncraftable");
    private static final ResourceLocation PAGE_FORWARD = ResourceLocation.withDefaultNamespace("recipe_book/page_forward");
    private static final ResourceLocation PAGE_FORWARD_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/page_forward_highlighted");
    private static final ResourceLocation PAGE_BACKWARD = ResourceLocation.withDefaultNamespace("recipe_book/page_backward");
    private static final ResourceLocation PAGE_BACKWARD_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted");

    private static final int BOOK_WIDTH = 147;
    private static final int BOOK_HEIGHT = 166;
    /** Same threshold vanilla uses to decide the book should cover the screen instead of sitting beside it. */
    private static final int NARROW_WIDTH = 379;
    private static final int BUTTON_X = 148;
    private static final int BUTTON_Y = 51;
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 18;
    private static final int COLUMNS = 5;
    private static final int ROWS = 4;
    private static final int PER_PAGE = COLUMNS * ROWS;
    private static final int CELL = 25;
    private static final int BOTTLE_SLOTS = 3;
    private static final int INGREDIENT_SLOT = 3;

    private static boolean open;
    private static int page;
    @Nullable
    private static BrewingRecipe selected;
    @Nullable
    private static BrewingRecipe hovered;

    private BrewingRecipeBook() {
    }

    // ---- Layout ----------------------------------------------------------------------------------

    private static AbstractContainerScreenAccessor access(BrewingStandScreen screen) {
        return (AbstractContainerScreenAccessor) screen;
    }

    private static boolean isNarrow(Screen screen) {
        return screen.width < NARROW_WIDTH;
    }

    private static int defaultLeft(BrewingStandScreen screen) {
        return (screen.width - access(screen).pinrecipe$getImageWidth()) / 2;
    }

    /** How far the brewing stand is pushed right to make room for the book. */
    public static int backgroundShift(BrewingStandScreen screen) {
        return access(screen).pinrecipe$getLeftPos() - defaultLeft(screen);
    }

    private static int bookX(Screen screen) {
        return isNarrow(screen) ? (screen.width - BOOK_WIDTH) / 2 : (screen.width - BOOK_WIDTH) / 2 - 86;
    }

    private static int bookY(Screen screen) {
        return (screen.height - BOOK_HEIGHT) / 2;
    }

    private static int buttonX(BrewingStandScreen screen) {
        return access(screen).pinrecipe$getLeftPos() + BUTTON_X;
    }

    private static int buttonY(BrewingStandScreen screen) {
        return access(screen).pinrecipe$getTopPos() + BUTTON_Y;
    }

    /** Called after the screen is (re)initialised, and when the book is toggled. */
    public static void onScreenInit(Screen screen) {
        if (!(screen instanceof BrewingStandScreen brewing)) {
            return;
        }
        int left = defaultLeft(brewing);
        if (open && !isNarrow(screen)) {
            left += 77;
        }
        access(brewing).pinrecipe$setLeftPos(left);
    }

    private static List<BrewingRecipe> knownRecipes() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return List.of();
        }
        List<BrewingRecipe> known = new ArrayList<>();
        for (BrewingRecipe recipe : BrewingRecipes.all(mc.level)) {
            if (ClientBrewingKnowledge.isKnown(recipe.key())) {
                known.add(recipe);
            }
        }
        return known;
    }

    private static int totalCount() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : BrewingRecipes.all(mc.level).size();
    }

    private static int pageCount(int recipes) {
        return Math.max(1, (recipes + PER_PAGE - 1) / PER_PAGE);
    }

    // ---- Rendering -------------------------------------------------------------------------------

    /** Drawn from the brewing stand's background pass, so slots and the carried item draw on top. */
    public static void renderBackground(BrewingStandScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        renderToggleButton(screen, graphics, mouseX, mouseY);
        renderGhostItems(screen, graphics);
        if (open && !isNarrow(screen)) {
            renderBook(screen, graphics, mouseX, mouseY);
        }
    }

    /** Drawn after the whole screen: the narrow-screen book (which covers the stand) and tooltips. */
    public static void renderForeground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (!(screen instanceof BrewingStandScreen brewing)) {
            return;
        }
        if (open && isNarrow(screen)) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            renderBook(brewing, graphics, mouseX, mouseY);
            // The book covers the stand here, so keep the toggle button visible on top to close it again.
            renderToggleButton(brewing, graphics, mouseX, mouseY);
            graphics.pose().popPose();
        }
        if (open && hovered != null) {
            Font font = Minecraft.getInstance().font;
            List<Component> lines = new ArrayList<>();
            ItemStack output = hovered.currentOutput();
            lines.add(hovered.outputName().copy().withStyle(output.getRarity().color()));
            // The potion's effects and durations, as on the potion's own tooltip.
            PotionContents contents = output.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                contents.addPotionTooltip(lines::add, 1.0F, Minecraft.getInstance().level.tickRateManager().tickrate());
            }
            lines.add(Component.translatable("pinrecipe.brewing.base", hovered.inputName()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("pinrecipe.brewing.ingredient", hovered.ingredient().getHoverName()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(hovered == selected ? "pinrecipe.brewing.click_hide" : "pinrecipe.brewing.click_show")
                    .withStyle(ChatFormatting.DARK_GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private static void renderToggleButton(BrewingStandScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        int x = buttonX(screen);
        int y = buttonY(screen);
        boolean hover = inside(mouseX, mouseY, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        graphics.blitSprite(hover ? BUTTON_HIGHLIGHTED : BUTTON, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private static void renderBook(BrewingStandScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int x = bookX(screen);
        int y = bookY(screen);
        hovered = null;

        graphics.blit(BOOK_TEXTURE, x, y, 1, 1, BOOK_WIDTH, BOOK_HEIGHT);
        // Cover the search icon baked into the vanilla texture; this book has a title instead.
        graphics.fill(x + 9, y + 10, x + 26, y + 28, 0xFF373737);

        List<BrewingRecipe> recipes = knownRecipes();
        graphics.drawString(font, Component.translatable("pinrecipe.brewing.title"), x + 11, y + 15, 0xFFFFFF, false);
        String count = recipes.size() + "/" + totalCount();
        graphics.drawString(font, count, x + BOOK_WIDTH - 11 - font.width(count), y + 15, 0xA0A0A0, false);

        if (recipes.isEmpty()) {
            List<FormattedCharSequence> lines = font.split(FormattedText.of(Component.translatable("pinrecipe.brewing.empty").getString()), BOOK_WIDTH - 30);
            int lineY = y + 70;
            for (FormattedCharSequence line : lines) {
                graphics.drawString(font, line, x + (BOOK_WIDTH - font.width(line)) / 2, lineY, 0xC0C0C0, false);
                lineY += 10;
            }
            return;
        }

        int pages = pageCount(recipes.size());
        page = Math.min(page, pages - 1);
        List<Slot> slots = screen.getMenu().slots;
        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= recipes.size()) {
                break;
            }
            BrewingRecipe recipe = recipes.get(index);
            int cx = x + 11 + CELL * (i % COLUMNS);
            int cy = y + 31 + CELL * (i / COLUMNS);
            graphics.blitSprite(canBrew(recipe, slots) ? SLOT_CRAFTABLE : SLOT_UNCRAFTABLE, cx, cy, CELL, CELL);
            graphics.renderItem(recipe.currentOutput(), cx + 4, cy + 4);
            if (recipe == selected) {
                graphics.renderOutline(cx, cy, CELL, CELL, 0xFFFFD700);
            }
            if (inside(mouseX, mouseY, cx, cy, CELL, CELL)) {
                hovered = recipe;
                graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0x30FFFFFF);
            }
        }

        if (pages > 1) {
            if (page > 0) {
                boolean hover = inside(mouseX, mouseY, x + 38, y + 137, 12, 17);
                graphics.blitSprite(hover ? PAGE_BACKWARD_HIGHLIGHTED : PAGE_BACKWARD, x + 38, y + 137, 12, 17);
            }
            if (page < pages - 1) {
                boolean hover = inside(mouseX, mouseY, x + 93, y + 137, 12, 17);
                graphics.blitSprite(hover ? PAGE_FORWARD_HIGHLIGHTED : PAGE_FORWARD, x + 93, y + 137, 12, 17);
            }
            String text = (page + 1) + "/" + pages;
            graphics.drawString(font, text, x + 73 - font.width(text) / 2, y + 141, 0xFFFFFF, false);
        }
    }

    /** Shows the selected recipe's bottles and ingredient faintly in the brewing stand's empty slots. */
    private static void renderGhostItems(BrewingStandScreen screen, GuiGraphics graphics) {
        if (selected == null) {
            return;
        }
        List<Slot> slots = screen.getMenu().slots;
        int left = access(screen).pinrecipe$getLeftPos();
        int top = access(screen).pinrecipe$getTopPos();
        for (int i = 0; i <= INGREDIENT_SLOT && i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot.hasItem()) {
                continue;
            }
            ItemStack ghost = i == INGREDIENT_SLOT ? selected.ingredient() : selected.currentInput();
            int sx = left + slot.x;
            int sy = top + slot.y;
            graphics.renderFakeItem(ghost, sx, sy);
            // Wash the item out so it doesn't look like a real one.
            graphics.fill(RenderType.guiGhostRecipeOverlay(), sx, sy, sx + 16, sy + 16, 0x908B8B8B);
        }
    }

    /** Whether the player has a matching bottle and the ingredient, in their inventory or the stand. */
    private static boolean canBrew(BrewingRecipe recipe, List<Slot> slots) {
        boolean hasInput = false;
        boolean hasIngredient = false;
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            hasInput |= recipe.inputTest().test(stack);
            hasIngredient |= recipe.ingredientMatches(stack);
            if (hasInput && hasIngredient) {
                return true;
            }
        }
        return false;
    }

    // ---- Input -----------------------------------------------------------------------------------

    /** Returns true if the click was used by the book (so the brewing stand shouldn't see it). */
    public static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (!(screen instanceof BrewingStandScreen brewing)) {
            return false;
        }
        if (button == 0 && inside(mouseX, mouseY, buttonX(brewing), buttonY(brewing), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            open = !open;
            hovered = null;
            onScreenInit(screen);
            playClick();
            return true;
        }
        if (!open || !insideBook(screen, mouseX, mouseY)) {
            return false;
        }
        if (button == 0) {
            int x = bookX(screen);
            int y = bookY(screen);
            List<BrewingRecipe> recipes = knownRecipes();
            int pages = pageCount(recipes.size());
            if (pages > 1 && page > 0 && inside(mouseX, mouseY, x + 38, y + 137, 12, 17)) {
                page--;
                playClick();
            } else if (pages > 1 && page < pages - 1 && inside(mouseX, mouseY, x + 93, y + 137, 12, 17)) {
                page++;
                playClick();
            } else if (hovered != null) {
                selected = hovered == selected ? null : hovered;
                playClick();
            }
        }
        return true; // Never let a click on the book count as a click outside the brewing stand.
    }

    public static boolean mouseReleased(Screen screen, double mouseX, double mouseY, int button) {
        if (!(screen instanceof BrewingStandScreen brewing)) {
            return false;
        }
        return inside(mouseX, mouseY, buttonX(brewing), buttonY(brewing), BUTTON_WIDTH, BUTTON_HEIGHT)
                || (open && insideBook(screen, mouseX, mouseY));
    }

    /** Clears the ghost recipe when a different brewing stand is opened. */
    public static void onScreenOpened() {
        selected = null;
        hovered = null;
    }

    private static boolean insideBook(Screen screen, double mouseX, double mouseY) {
        return inside(mouseX, mouseY, bookX(screen), bookY(screen), BOOK_WIDTH, BOOK_HEIGHT);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
