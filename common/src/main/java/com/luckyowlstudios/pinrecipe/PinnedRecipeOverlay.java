package com.luckyowlstudios.pinrecipe;

import com.luckyowlstudios.pinrecipe.mixin.AbstractContainerScreenAccessor;
import com.luckyowlstudios.pinrecipe.mixin.RecipeBookComponentAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_SLOT_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_SLOT_FILL = 0xFF8B8B8B;
    private static final int COLOR_MISSING = 0x90FF3030;
    private static final int COLOR_HOVER = 0x80FFFFFF;

    /** Hit boxes of the last frame's unpin buttons, so clicks can be matched to a recipe. */
    private static final List<UnpinButton> UNPIN_BUTTONS = new ArrayList<>();
    private static boolean unpinHoveredThisFrame;

    private record UnpinButton(ResourceLocation id, int x, int y, int size) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + size && my >= y && my < y + size;
        }
    }

    private record Layout(RecipeHolder<?> holder, List<Ingredient> ingredients, int gridWidth, int gridHeight, ItemStack result) {
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

    public static boolean shouldShow(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CraftingScreen;
    }

    public static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (button != 0 || !shouldShow(screen)) {
            return false;
        }
        for (UnpinButton unpin : UNPIN_BUTTONS) {
            if (unpin.contains(mouseX, mouseY)) {
                PinnedRecipes.unpin(unpin.id());
                UNPIN_BUTTONS.clear();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return false;
    }

    public static void render(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        UNPIN_BUTTONS.clear();
        if (!shouldShow(screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }

        List<Layout> layouts = new ArrayList<>();
        for (ResourceLocation id : PinnedRecipes.get()) {
            Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(id);
            holder.map(h -> layout(h, level)).ifPresent(layouts::add);
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
        for (Layout layout : layouts) {
            if (y + layout.height(compact) > screen.height) {
                break; // No room for more pins on this screen size.
            }
            ItemStack hovered = drawPanel(graphics, font, layout, mc.player.getInventory(), x, y, compact, mouseX, mouseY);
            if (!hovered.isEmpty()) {
                tooltip = hovered;
            }
            y += layout.height(compact) + PANEL_SPACING;
        }
        if (!tooltip.isEmpty()) {
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        } else if (unpinHoveredThisFrame) {
            graphics.renderTooltip(font, Component.translatable("pinrecipe.overlay.unpin").withStyle(ChatFormatting.GRAY), mouseX, mouseY);
        }
    }

    private static Layout layout(RecipeHolder<?> holder, Level level) {
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
        return new Layout(holder, ingredients, width, height, result);
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
            contentLeft = access.pinrecipe$getLeftPos();
            contentRight = contentLeft + access.pinrecipe$getImageWidth();
            top = access.pinrecipe$getTopPos();
        }
        if (screen instanceof RecipeUpdateListener listener) {
            RecipeBookComponent book = listener.getRecipeBookComponent();
            if (book != null && book.isVisible()) {
                bookOpen = true;
                int bookLeft = (screen.width - 147) / 2 - ((RecipeBookComponentAccessor) book).pinrecipe$getXOffset();
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
                                       int x, int y, boolean compact, int mouseX, int mouseY) {
        int width = compact ? COMPACT_WIDTH : WIDE_WIDTH;
        int height = layout.height(compact);
        graphics.fill(x, y, x + width, y + height, COLOR_BACKGROUND);
        graphics.renderOutline(x, y, width, height, COLOR_BORDER);

        // Title: the result's name, with an unpin button on the right.
        int unpinSize = 9;
        int unpinX = x + width - PADDING - unpinSize + 2;
        int unpinY = y + PADDING - 2;
        boolean unpinHovered = mouseX >= unpinX && mouseX < unpinX + unpinSize && mouseY >= unpinY && mouseY < unpinY + unpinSize;
        Component title = layout.result().getHoverName();
        String titleText = font.plainSubstrByWidth(title.getString(), width - 2 * PADDING - unpinSize - 2);
        graphics.drawString(font, titleText, x + PADDING, y + PADDING, 0xFFFFFF, true);
        graphics.drawString(font, "x", unpinX + 2, unpinY, unpinHovered ? 0xFF5555 : 0xAAAAAA, true);
        UNPIN_BUTTONS.add(new UnpinButton(layout.holder().id(), unpinX, unpinY, unpinSize));

        ItemStack hoveredStack = ItemStack.EMPTY;
        if (unpinHovered) {
            unpinHoveredThisFrame = true;
        }

        // Ingredient grid.
        int gridX = x + PADDING;
        int gridY = y + PADDING + TITLE_HEIGHT;
        boolean[] missing = findMissing(layout.ingredients(), inventory);
        for (int row = 0; row < layout.gridHeight(); row++) {
            for (int col = 0; col < layout.gridWidth(); col++) {
                int index = row * layout.gridWidth() + col;
                int sx = gridX + col * SLOT;
                int sy = gridY + row * SLOT;
                drawSlot(graphics, sx, sy);
                if (index >= layout.ingredients().size()) {
                    continue;
                }
                Ingredient ingredient = layout.ingredients().get(index);
                ItemStack stack = cycle(ingredient);
                if (stack.isEmpty()) {
                    continue;
                }
                if (missing[index]) {
                    graphics.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, COLOR_MISSING);
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

    /** Shows each matching item in turn, once per second, like the vanilla recipe book does. */
    private static ItemStack cycle(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) {
            return ItemStack.EMPTY;
        }
        return items[(int) ((Util.getMillis() / 1000L) % items.length)];
    }

    /**
     * Marks the ingredients the player can't cover from their inventory. Each inventory item
     * is only counted once, so a recipe needing 3 planks with only 2 in hand marks one slot.
     */
    private static boolean[] findMissing(List<Ingredient> ingredients, Inventory inventory) {
        int size = inventory.items.size();
        int[] remaining = new int[size];
        for (int i = 0; i < size; i++) {
            remaining[i] = inventory.items.get(i).getCount();
        }
        boolean[] missing = new boolean[ingredients.size()];
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient.isEmpty()) {
                continue;
            }
            int found = findSlot(ingredient, inventory, remaining);
            if (found >= 0) {
                remaining[found]--;
            } else {
                missing[i] = true;
            }
        }
        return missing;
    }

    private static int findSlot(Ingredient ingredient, Inventory inventory, int[] remaining) {
        for (int slot = 0; slot < remaining.length; slot++) {
            if (remaining[slot] > 0 && ingredient.test(inventory.items.get(slot))) {
                return slot;
            }
        }
        return -1;
    }
}
