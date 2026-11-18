package com.luckyowlstudios.codex;

import com.luckyowlstudios.codex.brewing.BrewingRecipe;
import com.luckyowlstudios.codex.brewing.BrewingRecipes;
import com.luckyowlstudios.codex.client.BrewingFill;
import com.luckyowlstudios.codex.client.BrewingRecipeBook;
import com.luckyowlstudios.codex.client.RecipeLookup;
import com.luckyowlstudios.codex.mixin.AbstractContainerScreenAccessor;
import com.luckyowlstudios.codex.mixin.RecipeBookComponentAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Draws pinned recipes beside the inventory. Never drawn on the in-game HUD:
 * this only runs from screen render events, and only for the inventory and crafting table screens.
 */
public final class PinnedRecipeOverlay {
    private static final ResourceLocation ARROW_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    private static final int SLOT = 18;
    private static final int PADDING = 5;
    private static final int TITLE_HEIGHT = 12;
    private static final int ARROW_WIDTH = 24;
    private static final int GAP = 4;
    /** Wide panel: grid, arrow and result side by side. */
    private static final int WIDE_WIDTH = PADDING + 3 * SLOT + GAP + ARROW_WIDTH + GAP + SLOT + PADDING;
    /** Compact panel for small screens: the result sits under the grid. */
    private static final int COMPACT_WIDTH = PADDING + 3 * SLOT + PADDING;
    private static final int DOWN_ARROW_HEIGHT = 7;
    /** The recipe book's category tabs stick out this far to the left of the book. */
    private static final int BOOK_TAB_OVERHANG = 32;
    private static final int PANEL_SPACING = 3;
    private static final int SCREEN_MARGIN = 4;

    private static final int COLOR_BACKGROUND = 0xE0101018;
    private static final int COLOR_BORDER = 0xFF5A4A8A;
    private static final int COLOR_BORDER_FILLABLE = 0xFFE0D070;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_SLOT_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_SLOT_FILL = 0xFF8B8B8B;
    private static final int COLOR_MISSING = 0x90FF3030;
    private static final int COLOR_HOVER = 0x80FFFFFF;

    /** Hit boxes of the last frame's unpin buttons, so clicks can be matched to a recipe. */
    private static final List<UnpinButton> UNPIN_BUTTONS = new ArrayList<>();
    private static boolean unpinHoveredThisFrame;
    /** Hit boxes of the last frame's panels, for click-to-fill and to keep clicks off the screen behind. */
    private static final List<PanelHit> PANELS = new ArrayList<>();
    /** Ingredient slots the player is missing items for: clicking one pins how to make it. */
    private static final List<MissingSlot> MISSING_SLOTS = new ArrayList<>();

    private record MissingSlot(ItemStack stack, List<ItemStack> options, int x, int y) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + SLOT && my >= y && my < y + SLOT;
        }
    }

    record PanelHit(Layout layout, int x, int y, int width, int height) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + width && my >= y && my < y + height;
        }
    }

    private record UnpinButton(String pin, int x, int y, int size) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + size && my >= y && my < y + size;
        }
    }

    /** One slot of a pinned recipe: the items it cycles through, and which items count as having it. */
    private record SlotSpec(List<ItemStack> options, Predicate<ItemStack> test) {
        static final SlotSpec EMPTY = new SlotSpec(List.of(), stack -> false);

        static SlotSpec of(Ingredient ingredient) {
            return ingredient.isEmpty() ? EMPTY : new SlotSpec(List.of(ingredient.getItems()), ingredient);
        }

        boolean isEmpty() {
            return options.isEmpty();
        }

        ItemStack current() {
            return options.isEmpty() ? ItemStack.EMPTY : options.get((int) ((Util.getMillis() / 1000L) % options.size()));
        }
    }

    /**
     * A pinned recipe ready to draw. Exactly one of {@code holder} (crafting, smelting, ...)
     * and {@code brewing} is set.
     */
    record Layout(String pin, @Nullable RecipeHolder<?> holder, @Nullable BrewingRecipe brewing, List<SlotSpec> slots,
                  int gridWidth, int gridHeight, ItemStack fixedResult, Component title) {
        ItemStack result() {
            return brewing != null ? brewing.currentOutput() : fixedResult;
        }

        int height(boolean compact) {
            int height = PADDING + TITLE_HEIGHT + gridHeight * SLOT + PADDING;
            if (compact) {
                height += GAP + DOWN_ARROW_HEIGHT + GAP + SLOT;
            }
            return height;
        }
    }

    private record Placement(int x, int y, boolean compact) {
    }

    private PinnedRecipeOverlay() {
    }

    /** The inventory and every station with a recipe book: crafting table, furnace, blast furnace, smoker. */
    public static boolean shouldShow(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CraftingScreen
                || screen instanceof AbstractFurnaceScreen<?> || screen instanceof BrewingStandScreen;
    }

    /** Whether this screen's grid can take the recipe, e.g. a crafting recipe in a crafting table. */
    public static boolean canFill(Screen screen, Layout layout) {
        if (layout.brewing() != null) {
            return screen instanceof BrewingStandScreen;
        }
        Recipe<?> recipe = layout.holder().value();
        RecipeType<?> type = recipe.getType();
        if (screen instanceof InventoryScreen) {
            return type == RecipeType.CRAFTING && recipe.canCraftInDimensions(2, 2);
        }
        if (screen instanceof CraftingScreen) {
            return type == RecipeType.CRAFTING;
        }
        if (screen instanceof BlastFurnaceScreen) {
            return type == RecipeType.BLASTING;
        }
        if (screen instanceof SmokerScreen) {
            return type == RecipeType.SMOKING;
        }
        if (screen instanceof FurnaceScreen) {
            return type == RecipeType.SMELTING;
        }
        return false;
    }

    /** Why a pin can't be filled into this screen, for its tooltip. */
    private static Component cantFillReason(Screen screen, Layout layout) {
        if (layout.brewing() != null) {
            return Component.translatable("codex.overlay.use_station", Component.translatable("block.minecraft.brewing_stand"));
        }
        RecipeType<?> type = layout.holder().value().getType();
        if (screen instanceof InventoryScreen && type == RecipeType.CRAFTING) {
            return Component.translatable("codex.overlay.too_big");
        }
        String station = type == RecipeType.CRAFTING ? "crafting_table"
                : type == RecipeType.SMELTING ? "furnace"
                : type == RecipeType.BLASTING ? "blast_furnace"
                : type == RecipeType.SMOKING ? "smoker"
                : type == RecipeType.CAMPFIRE_COOKING ? "campfire"
                : type == RecipeType.STONECUTTING ? "stonecutter"
                : type == RecipeType.SMITHING ? "smithing_table"
                : null;
        return station == null
                ? Component.translatable("codex.overlay.cant_fill")
                : Component.translatable("codex.overlay.use_station", Component.translatable("block.minecraft." + station));
    }

    public static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (!shouldShow(screen)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (button == 0) {
            for (UnpinButton unpin : UNPIN_BUTTONS) {
                if (unpin.contains(mouseX, mouseY)) {
                    PinnedRecipes.unpin(unpin.pin());
                    UNPIN_BUTTONS.clear();
                    PANELS.clear();
                    playClick();
                    return true;
                }
            }
        }
        if (button == 0) {
            for (MissingSlot slot : MISSING_SLOTS) {
                if (slot.contains(mouseX, mouseY)) {
                    RecipeLookup.pinRecipeFor(slot.stack(), slot.options());
                    return true;
                }
            }
        }
        for (PanelHit panel : PANELS) {
            if (!panel.contains(mouseX, mouseY)) {
                continue;
            }
            Layout layout = panel.layout();
            if (button == 0 && canFill(screen, layout) && mc.gameMode != null) {
                if (layout.brewing() != null && screen instanceof BrewingStandScreen brewingScreen) {
                    BrewingFill.fill(brewingScreen, layout.brewing());
                    playClick();
                } else if (layout.holder() != null && screen instanceof AbstractContainerScreen<?> container) {
                    // Same request the vanilla recipe book sends: the server moves the items in, or
                    // shows a ghost of the recipe in the grid if the player is missing some.
                    mc.gameMode.handlePlaceRecipe(container.getMenu().containerId, layout.holder(), Screen.hasShiftDown());
                    playClick();
                }
            }
            return true; // Never let a click on a pin count as a click outside the screen (which drops the held item).
        }
        return false;
    }

    public static boolean mouseReleased(Screen screen, double mouseX, double mouseY) {
        if (!shouldShow(screen)) {
            return false;
        }
        for (PanelHit panel : PANELS) {
            if (panel.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    static List<PanelHit> panelsForTest() {
        return List.copyOf(PANELS);
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public static void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        UNPIN_BUTTONS.clear();
        PANELS.clear();
        MISSING_SLOTS.clear();
        if (!shouldShow(screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }

        List<Layout> layouts = new ArrayList<>();
        for (String pin : PinnedRecipes.get()) {
            String brewingKey = PinnedRecipes.brewingKeyOf(pin);
            if (brewingKey != null) {
                BrewingRecipes.byKey(level, brewingKey).map(r -> brewingLayout(pin, r)).ifPresent(layouts::add);
            } else {
                ResourceLocation id = ResourceLocation.tryParse(pin);
                if (id != null) {
                    level.getRecipeManager().byKey(id).map(h -> layout(pin, h, level)).ifPresent(layouts::add);
                }
            }
        }
        if (layouts.isEmpty()) {
            return;
        }

        Placement placement = findPlacement(screen, layouts);
        if (placement == null) {
            return;
        }
        boolean compact = placement.compact();
        int x = placement.x();
        int y = placement.y();

        Font font = mc.font;
        unpinHoveredThisFrame = false;
        ItemStack tooltip = ItemStack.EMPTY;
        PanelHit hoveredPanel = null;
        int width = compact ? COMPACT_WIDTH : WIDE_WIDTH;
        for (Layout layout : layouts) {
            int height = layout.height(compact);
            if (y + height > screen.height) {
                break; // No room for more pins on this screen size.
            }
            boolean fillable = canFill(screen, layout);
            ItemStack hovered = drawPanel(graphics, font, layout, mc.player.getInventory(), x, y, compact, fillable, mouseX, mouseY);
            if (!hovered.isEmpty()) {
                tooltip = hovered;
            }
            PanelHit panel = new PanelHit(layout, x, y, width, height);
            PANELS.add(panel);
            if (panel.contains(mouseX, mouseY)) {
                hoveredPanel = panel;
            }
            y += height + PANEL_SPACING;
        }
        if (!tooltip.isEmpty()) {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(mc, tooltip));
            for (MissingSlot slot : MISSING_SLOTS) {
                if (slot.contains(mouseX, mouseY)) {
                    lines.add(Component.translatable("codex.lookup.hint").withStyle(ChatFormatting.DARK_GRAY));
                    break;
                }
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (unpinHoveredThisFrame) {
            graphics.renderTooltip(font, Component.translatable("codex.overlay.unpin").withStyle(ChatFormatting.GRAY), mouseX, mouseY);
        } else if (hoveredPanel != null) {
            Layout layout = hoveredPanel.layout();
            List<Component> lines;
            if (!canFill(screen, layout)) {
                lines = List.of(cantFillReason(screen, layout).copy().withStyle(ChatFormatting.GRAY));
            } else if (layout.brewing() != null) {
                lines = List.of(Component.translatable("codex.overlay.fill_brewing").withStyle(ChatFormatting.GRAY));
            } else {
                lines = List.of(Component.translatable("codex.overlay.fill").withStyle(ChatFormatting.GRAY),
                        Component.translatable("codex.overlay.fill_all").withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    /** Brewing pins show the ingredient and the base bottle side by side. */
    private static Layout brewingLayout(String pin, BrewingRecipe recipe) {
        List<SlotSpec> slots = List.of(
                new SlotSpec(recipe.ingredients(), recipe::ingredientMatches),
                new SlotSpec(recipe.inputs(), recipe.inputTest()));
        return new Layout(pin, null, recipe, slots, 2, 1, ItemStack.EMPTY, recipe.outputName());
    }

    private static Layout layout(String pin, RecipeHolder<?> holder, Level level) {
        Recipe<?> recipe = holder.value();
        List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients());
        int width;
        int height;
        if (recipe instanceof ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
        } else {
            ingredients.removeIf(Ingredient::isEmpty);
            int count = Math.max(1, ingredients.size());
            width = Math.min(3, count);
            height = (count + 2) / 3;
        }
        width = Math.max(1, Math.min(3, width));
        height = Math.max(1, Math.min(3, height));
        ItemStack result = recipe.getResultItem(level.registryAccess());
        List<SlotSpec> slots = ingredients.stream().map(SlotSpec::of).toList();
        return new Layout(pin, holder, null, slots, width, height, result, result.getHoverName());
    }

    /**
     * Picks a spot beside the inventory (and recipe book) where the pins don't cover anything,
     * switching to the narrower compact panel when the wide one doesn't fit. Returns null when the
     * recipe book is open and there's no free space, so the pins never cover the book.
     */
    @Nullable
    private static Placement findPlacement(Screen screen, List<Layout> layouts) {
        int contentLeft = (screen.width - 176) / 2;
        int contentRight = contentLeft + 176;
        int top = SCREEN_MARGIN;
        boolean bookOpen = false;
        if (screen instanceof AbstractContainerScreen<?> container) {
            AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) container;
            contentLeft = access.codex$getLeftPos();
            contentRight = contentLeft + access.codex$getImageWidth();
            top = access.codex$getTopPos();
        }
        int brewingBookLeft = BrewingRecipeBook.bookLeftEdge(screen);
        if (brewingBookLeft >= 0) {
            bookOpen = true;
            contentLeft = Math.min(contentLeft, brewingBookLeft);
        }
        if (screen instanceof RecipeUpdateListener listener) {
            RecipeBookComponent book = listener.getRecipeBookComponent();
            if (book != null && book.isVisible()) {
                bookOpen = true;
                int bookLeft = (screen.width - 147) / 2 - ((RecipeBookComponentAccessor) book).codex$getXOffset();
                contentLeft = Math.min(contentLeft, bookLeft - BOOK_TAB_OVERHANG);
            }
        }

        int spaceLeft = contentLeft - 2 * SCREEN_MARGIN;
        int spaceRight = screen.width - contentRight - 2 * SCREEN_MARGIN;
        boolean compact = false;
        int x;
        if (spaceLeft >= WIDE_WIDTH) {
            x = contentLeft - SCREEN_MARGIN - WIDE_WIDTH;
        } else if (spaceRight >= WIDE_WIDTH) {
            x = contentRight + SCREEN_MARGIN;
        } else {
            compact = true;
            if (spaceLeft >= COMPACT_WIDTH) {
                x = contentLeft - SCREEN_MARGIN - COMPACT_WIDTH;
            } else if (spaceRight >= COMPACT_WIDTH) {
                x = contentRight + SCREEN_MARGIN;
            } else if (bookOpen) {
                return null;
            } else {
                x = SCREEN_MARGIN;
            }
        }

        int totalHeight = PANEL_SPACING * (layouts.size() - 1);
        for (Layout layout : layouts) {
            totalHeight += layout.height(compact);
        }
        int y = top;
        if (y + totalHeight > screen.height - SCREEN_MARGIN) {
            y = Math.max(SCREEN_MARGIN, screen.height - SCREEN_MARGIN - totalHeight);
        }
        return new Placement(x, y, compact);
    }

    /** Draws one pinned recipe. Returns the stack under the mouse, if any, for the tooltip. */
    private static ItemStack drawPanel(GuiGraphics graphics, Font font, Layout layout, Inventory inventory,
                                       int x, int y, boolean compact, boolean fillable, int mouseX, int mouseY) {
        int width = compact ? COMPACT_WIDTH : WIDE_WIDTH;
        int height = layout.height(compact);
        boolean panelHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        graphics.fill(x, y, x + width, y + height, COLOR_BACKGROUND);
        // A brighter border on a pin you can click to fill into this screen.
        graphics.renderOutline(x, y, width, height, fillable && panelHovered ? COLOR_BORDER_FILLABLE : COLOR_BORDER);

        // Title: the result's name, with an unpin button on the right.
        int unpinSize = 9;
        int unpinX = x + width - PADDING - unpinSize + 2;
        int unpinY = y + PADDING - 2;
        boolean unpinHovered = mouseX >= unpinX && mouseX < unpinX + unpinSize && mouseY >= unpinY && mouseY < unpinY + unpinSize;
        Component title = layout.title();
        String titleText = font.plainSubstrByWidth(title.getString(), width - 2 * PADDING - unpinSize - 2);
        graphics.drawString(font, titleText, x + PADDING, y + PADDING, 0xFFFFFF, true);
        graphics.drawString(font, "x", unpinX + 2, unpinY, unpinHovered ? 0xFF5555 : 0xAAAAAA, true);
        UNPIN_BUTTONS.add(new UnpinButton(layout.pin(), unpinX, unpinY, unpinSize));

        ItemStack hoveredStack = ItemStack.EMPTY;
        if (unpinHovered) {
            unpinHoveredThisFrame = true;
        }

        // Ingredient grid.
        int gridX = x + PADDING;
        int gridY = y + PADDING + TITLE_HEIGHT;
        boolean[] missing = findMissing(layout.slots(), inventory);
        for (int row = 0; row < layout.gridHeight(); row++) {
            for (int col = 0; col < layout.gridWidth(); col++) {
                int index = row * layout.gridWidth() + col;
                int sx = gridX + col * SLOT;
                int sy = gridY + row * SLOT;
                drawSlot(graphics, sx, sy);
                if (index >= layout.slots().size()) {
                    continue;
                }
                ItemStack stack = layout.slots().get(index).current();
                if (stack.isEmpty()) {
                    continue;
                }
                if (missing[index]) {
                    graphics.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, COLOR_MISSING);
                    MISSING_SLOTS.add(new MissingSlot(stack, layout.slots().get(index).options(), sx, sy));
                }
                graphics.renderItem(stack, sx + 1, sy + 1);
                if (isOver(mouseX, mouseY, sx, sy)) {
                    graphics.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, COLOR_HOVER);
                    hoveredStack = stack;
                }
            }
        }

        // Arrow and result: to the right of the grid, or underneath it in compact mode.
        int gridPixelHeight = layout.gridHeight() * SLOT;
        int resultX;
        int resultY;
        if (compact) {
            int centerX = gridX + (3 * SLOT) / 2;
            int arrowY = gridY + gridPixelHeight + GAP;
            drawDownArrow(graphics, centerX, arrowY);
            resultX = centerX - SLOT / 2;
            resultY = arrowY + DOWN_ARROW_HEIGHT + GAP;
        } else {
            int arrowX = gridX + 3 * SLOT + GAP;
            int arrowY = gridY + (gridPixelHeight - 16) / 2;
            graphics.blitSprite(ARROW_SPRITE, arrowX, arrowY, ARROW_WIDTH, 16);
            resultX = arrowX + ARROW_WIDTH + GAP;
            resultY = gridY + (gridPixelHeight - SLOT) / 2;
        }
        drawSlot(graphics, resultX, resultY);
        ItemStack result = layout.result();
        if (!result.isEmpty()) {
            graphics.renderItem(result, resultX + 1, resultY + 1);
            graphics.renderItemDecorations(font, result, resultX + 1, resultY + 1);
            if (isOver(mouseX, mouseY, resultX, resultY)) {
                graphics.fill(resultX + 1, resultY + 1, resultX + SLOT - 1, resultY + SLOT - 1, COLOR_HOVER);
                hoveredStack = result;
            }
        }
        return hoveredStack;
    }

    /** A small downward arrow, DOWN_ARROW_HEIGHT pixels tall, centred on centerX. */
    private static void drawDownArrow(GuiGraphics graphics, int centerX, int y) {
        int color = 0xFFC6C6C6;
        graphics.fill(centerX - 1, y, centerX + 1, y + 3, color);
        for (int i = 0; i < 4; i++) {
            int half = 4 - i;
            graphics.fill(centerX - half, y + 3 + i, centerX + half, y + 4 + i, color);
        }
    }

    private static boolean isOver(int mouseX, int mouseY, int sx, int sy) {
        return mouseX >= sx + 1 && mouseX < sx + SLOT - 1 && mouseY >= sy + 1 && mouseY < sy + SLOT - 1;
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT, y + SLOT, COLOR_SLOT_DARK);
        graphics.fill(x + 1, y + 1, x + SLOT, y + SLOT, COLOR_SLOT_LIGHT);
        graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, COLOR_SLOT_FILL);
    }

    /**
     * Marks the slots the player can't cover from their inventory. Each inventory item
     * is only counted once, so a recipe needing 3 planks with only 2 in hand marks one slot.
     */
    private static boolean[] findMissing(List<SlotSpec> slots, Inventory inventory) {
        int size = inventory.items.size();
        int[] remaining = new int[size];
        for (int i = 0; i < size; i++) {
            remaining[i] = inventory.items.get(i).getCount();
        }
        boolean[] missing = new boolean[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            SlotSpec slot = slots.get(i);
            if (slot.isEmpty()) {
                continue;
            }
            int found = findSlot(slot, inventory, remaining);
            if (found >= 0) {
                remaining[found]--;
            } else {
                missing[i] = true;
            }
        }
        return missing;
    }

    private static int findSlot(SlotSpec spec, Inventory inventory, int[] remaining) {
        for (int slot = 0; slot < remaining.length; slot++) {
            if (remaining[slot] > 0 && spec.test().test(inventory.items.get(slot))) {
                return slot;
            }
        }
        return -1;
    }
}
