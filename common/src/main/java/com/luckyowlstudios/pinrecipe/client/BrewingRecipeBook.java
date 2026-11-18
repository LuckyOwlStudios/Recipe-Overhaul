package com.luckyowlstudios.pinrecipe.client;

import com.luckyowlstudios.pinrecipe.PinRecipeClient;
import com.luckyowlstudios.pinrecipe.PinnedRecipes;
import com.luckyowlstudios.pinrecipe.brewing.BrewingRecipe;
import com.luckyowlstudios.pinrecipe.brewing.BrewingRecipes;
import com.luckyowlstudios.pinrecipe.brewing.ClientBrewingKnowledge;
import com.luckyowlstudios.pinrecipe.mixin.AbstractContainerScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private static final ResourceLocation FILTER_ON = ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled");
    private static final ResourceLocation FILTER_OFF = ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled");
    private static final ResourceLocation FILTER_ON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled_highlighted");
    private static final ResourceLocation FILTER_OFF_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled_highlighted");
    private static final int FILTER_X = 110;
    private static final int FILTER_Y = 12;
    private static final int FILTER_WIDTH = 26;
    private static final int FILTER_HEIGHT = 16;
    private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint")
            .withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

    @Nullable
    private static EditBox searchBox;
    private static boolean filterTooltipHovered;
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

        String text = searchBox == null ? "" : searchBox.getValue();
        searchBox = new EditBox(Minecraft.getInstance().font, bookX(screen) + 25, bookY(screen) + 13, 81, 14,
                Component.translatable("itemGroup.search"));
        searchBox.setMaxLength(50);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setHint(SEARCH_HINT);
        searchBox.setValue(text);
        searchBox.setResponder(value -> page = 0);
    }

    /** Typing in the search box. Returns true if the key was used (so the screen doesn't close on E). */
    public static boolean keyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
        if (!open || !(screen instanceof BrewingStandScreen) || searchBox == null || !searchBox.isFocused()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false; // Esc still closes the screen, like the vanilla book.
        }
        searchBox.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    /** Called for each typed character. Returns true if the search box took it. */
    public static boolean charTyped(char character, int modifiers) {
        if (!open || !(Minecraft.getInstance().screen instanceof BrewingStandScreen) || searchBox == null || !searchBox.isFocused()) {
            return false;
        }
        searchBox.charTyped(character, modifiers);
        return true;
    }

    /** Unlocked recipes, narrowed by the search text and the "show brewable" toggle. */
    private static List<BrewingRecipe> visibleRecipes(BrewingStandScreen screen) {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        boolean brewableOnly = PinnedRecipes.isBrewingFilterOn();
        List<Slot> slots = screen.getMenu().slots;
        List<BrewingRecipe> visible = new ArrayList<>();
        for (BrewingRecipe recipe : knownRecipes()) {
            if (brewableOnly && !canBrew(recipe, slots)) {
                continue;
            }
            if (!query.isEmpty() && !matchesSearch(recipe, query)) {
                continue;
            }
            visible.add(recipe);
        }
        return visible;
    }

    private static boolean matchesSearch(BrewingRecipe recipe, String query) {
        return recipe.outputName().getString().toLowerCase(Locale.ROOT).contains(query)
                || recipe.inputName().getString().toLowerCase(Locale.ROOT).contains(query)
                || recipe.ingredient().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
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
        if (!(open && isNarrow(screen))) {
            renderGhostTooltip(brewing, graphics, mouseX, mouseY);
        }
        if (open && isNarrow(screen)) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            renderBook(brewing, graphics, mouseX, mouseY);
            // The book covers the stand here, so keep the toggle button visible on top to close it again.
            renderToggleButton(brewing, graphics, mouseX, mouseY);
            graphics.pose().popPose();
        }
        if (open && filterTooltipHovered && hovered == null) {
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable(PinnedRecipes.isBrewingFilterOn()
                    ? "pinrecipe.brewing.showing_brewable" : "gui.recipebook.toggleRecipes.all"), mouseX, mouseY);
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
            lines.add(Component.translatable("pinrecipe.brewing.click_fill").withStyle(ChatFormatting.DARK_GRAY));
            boolean pinned = PinnedRecipes.isPinned(PinnedRecipes.brewingPin(hovered.key()));
            if (!PinRecipeClient.PIN_KEY.isUnbound()) {
                lines.add(Component.translatable(pinned ? "pinrecipe.brewing.press_unpin" : "pinrecipe.brewing.press_pin",
                        PinRecipeClient.PIN_KEY.getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_GRAY));
            }
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

        // Search box and "show all / show brewable" toggle, in the same spots as the vanilla book.
        if (searchBox != null) {
            searchBox.setX(x + 25);
            searchBox.setY(y + 13);
            searchBox.render(graphics, mouseX, mouseY, 0);
        }
        boolean filterOn = PinnedRecipes.isBrewingFilterOn();
        boolean filterHovered = inside(mouseX, mouseY, x + FILTER_X, y + FILTER_Y, FILTER_WIDTH, FILTER_HEIGHT);
        graphics.blitSprite(filterOn
                        ? (filterHovered ? FILTER_ON_HIGHLIGHTED : FILTER_ON)
                        : (filterHovered ? FILTER_OFF_HIGHLIGHTED : FILTER_OFF),
                x + FILTER_X, y + FILTER_Y, FILTER_WIDTH, FILTER_HEIGHT);
        filterTooltipHovered = filterHovered;

        List<BrewingRecipe> recipes = visibleRecipes(screen);
        if (knownRecipes().isEmpty()) {
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
            if (PinnedRecipes.isPinned(PinnedRecipes.brewingPin(recipe.key()))) {
                // Small gold pin marker in the corner of pinned recipes.
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.fill(cx + CELL - 7, cy + 3, cx + CELL - 3, cy + 7, 0xFFFFD700);
                graphics.fill(cx + CELL - 6, cy + 7, cx + CELL - 4, cy + 10, 0xFFB08800);
                graphics.pose().popPose();
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
            // Same look as vanilla's ghost recipe: red slot, item, then a faint wash over it.
            graphics.fill(sx, sy, sx + 16, sy + 16, 0x30FF0000);
            graphics.renderFakeItem(ghost, sx, sy);
            graphics.fill(RenderType.guiGhostRecipeOverlay(), sx, sy, sx + 16, sy + 16, 0x30FFFFFF);
        }
    }

    /**
     * Clicking a ghost item looks up how to make it: a brewing recipe switches the ghost to that recipe
     * (e.g. Awkward Potion shows Water Bottle + Nether Wart), and either way the recipe gets pinned.
     */
    private static boolean clickGhost(BrewingStandScreen screen, double mouseX, double mouseY) {
        if (selected == null || (open && isNarrow(screen)) || !screen.getMenu().getCarried().isEmpty()) {
            return false;
        }
        List<Slot> slots = screen.getMenu().slots;
        int left = access(screen).pinrecipe$getLeftPos();
        int top = access(screen).pinrecipe$getTopPos();
        for (int i = 0; i <= INGREDIENT_SLOT && i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot.hasItem() || !inside(mouseX, mouseY, left + slot.x, top + slot.y, 16, 16)) {
                continue;
            }
            ItemStack ghost = i == INGREDIENT_SLOT ? selected.ingredient() : selected.currentInput();
            BrewingRecipe brewing = RecipeLookup.findBrewing(ghost);
            if (RecipeLookup.pinRecipeFor(ghost) && brewing != null) {
                selected = brewing;
            }
            return true;
        }
        return false;
    }

    /** Hovering a ghost item names it, like vanilla's ghost recipes in the crafting grid. */
    private static void renderGhostTooltip(BrewingStandScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (selected == null) {
            return;
        }
        List<Slot> slots = screen.getMenu().slots;
        int left = access(screen).pinrecipe$getLeftPos();
        int top = access(screen).pinrecipe$getTopPos();
        for (int i = 0; i <= INGREDIENT_SLOT && i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (!slot.hasItem() && inside(mouseX, mouseY, left + slot.x, top + slot.y, 16, 16)) {
                ItemStack ghost = i == INGREDIENT_SLOT ? selected.ingredient() : selected.currentInput();
                List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), ghost));
                lines.add(Component.translatable("pinrecipe.lookup.hint").withStyle(ChatFormatting.DARK_GRAY));
                graphics.renderComponentTooltip(Minecraft.getInstance().font, lines, mouseX, mouseY);
                return;
            }
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
        if (button == 0 && clickGhost(brewing, mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && inside(mouseX, mouseY, buttonX(brewing), buttonY(brewing), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            open = !open;
            hovered = null;
            onScreenInit(screen);
            playClick();
            return true;
        }
        if (!open || !insideBook(screen, mouseX, mouseY)) {
            if (searchBox != null) {
                searchBox.setFocused(false);
            }
            return false;
        }
        if (button == 0) {
            int x = bookX(screen);
            int y = bookY(screen);
            List<BrewingRecipe> recipes = visibleRecipes(brewing);
            int pages = pageCount(recipes.size());
            boolean onSearch = searchBox != null && inside(mouseX, mouseY, searchBox.getX(), searchBox.getY(), searchBox.getWidth(), searchBox.getHeight());
            if (searchBox != null) {
                searchBox.setFocused(onSearch);
            }
            if (onSearch) {
                searchBox.mouseClicked(mouseX, mouseY, button);
            } else if (inside(mouseX, mouseY, x + FILTER_X, y + FILTER_Y, FILTER_WIDTH, FILTER_HEIGHT)) {
                PinnedRecipes.setBrewingFilter(!PinnedRecipes.isBrewingFilterOn());
                page = 0;
                playClick();
            } else if (pages > 1 && page > 0 && inside(mouseX, mouseY, x + 38, y + 137, 12, 17)) {
                page--;
                playClick();
            } else if (pages > 1 && page < pages - 1 && inside(mouseX, mouseY, x + 93, y + 137, 12, 17)) {
                page++;
                playClick();
            } else if (hovered != null) {
                // Like the vanilla recipe book: move the items in, and show ghosts for anything missing.
                BrewingFill.fill(brewing, hovered);
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

    /** The recipe under the mouse in the open book, if any. */
    @Nullable
    public static BrewingRecipe hoveredRecipe(Screen screen) {
        return open && screen instanceof BrewingStandScreen ? hovered : null;
    }

    /** Shows this recipe's bottles and ingredient as ghost items in the stand's empty slots. */
    public static void showGhost(BrewingRecipe recipe) {
        selected = recipe;
    }

    /** Left edge of the open book beside the stand, or -1 if it isn't open beside it (for the pin overlay). */
    public static int bookLeftEdge(Screen screen) {
        if (!open || !(screen instanceof BrewingStandScreen)) {
            return -1;
        }
        return isNarrow(screen) ? 0 : bookX(screen);
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
