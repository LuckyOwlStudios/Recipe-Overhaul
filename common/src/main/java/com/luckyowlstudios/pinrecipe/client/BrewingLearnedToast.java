package com.luckyowlstudios.pinrecipe.client;

import com.luckyowlstudios.pinrecipe.brewing.BrewingRecipe;
import com.luckyowlstudios.pinrecipe.brewing.BrewingRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Like vanilla's "New Recipes Unlocked!" toast, for brewing recipes learned from a Potion Scroll.
 * One recipe shows the potion's name; several (a whole stack) show a count and cycle through the potions.
 */
public class BrewingLearnedToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/recipe");
    private static final long DISPLAY_TIME = 5000L;
    private static final long ICON_TIME = 1000L;

    private final List<ItemStack> icons = new ArrayList<>();
    private Component title;
    private Component description;
    private int count;
    private boolean changed;
    private long lastChanged;

    private BrewingLearnedToast(List<BrewingRecipe> recipes) {
        add(recipes);
    }

    /** Shows a toast for these recipe keys, merging into a toast that is still on screen. */
    public static void show(List<String> keys) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        List<BrewingRecipe> recipes = new ArrayList<>();
        for (String key : keys) {
            BrewingRecipes.byKey(mc.level, key).ifPresent(recipes::add);
        }
        if (recipes.isEmpty()) {
            return;
        }
        ToastComponent toasts = mc.getToasts();
        BrewingLearnedToast existing = toasts.getToast(BrewingLearnedToast.class, NO_TOKEN);
        if (existing != null) {
            existing.add(recipes);
        } else {
            toasts.addToast(new BrewingLearnedToast(recipes));
        }
    }

    private void add(List<BrewingRecipe> recipes) {
        for (BrewingRecipe recipe : recipes) {
            icons.add(recipe.outputs().get(0));
        }
        count += recipes.size();
        if (count == 1) {
            title = Component.translatable("pinrecipe.toast.learned.title");
            description = recipes.get(0).outputName();
        } else {
            title = Component.translatable("pinrecipe.toast.learned_many.title", count);
            description = Component.translatable("pinrecipe.toast.learned_many.description");
        }
        changed = true;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        if (changed) {
            lastChanged = timeSinceLastVisible;
            changed = false;
        }
        Minecraft mc = toasts.getMinecraft();
        graphics.blitSprite(BACKGROUND, 0, 0, width(), height());
        graphics.drawString(mc.font, title, 30, 7, 0xFF500050, false);
        graphics.drawString(mc.font, mc.font.substrByWidth(description, width() - 34).getString(), 30, 18, 0xFF000000, false);

        ItemStack icon = icons.isEmpty() ? new ItemStack(Items.POTION)
                : icons.get((int) (timeSinceLastVisible / Math.max(1L, ICON_TIME) % icons.size()));
        graphics.pose().pushPose();
        graphics.pose().scale(0.6F, 0.6F, 1.0F);
        graphics.renderFakeItem(new ItemStack(Items.BREWING_STAND), 3, 3);
        graphics.pose().popPose();
        graphics.renderFakeItem(icon, 8, 8);

        double shownFor = DISPLAY_TIME * toasts.getNotificationDisplayTimeMultiplier();
        return timeSinceLastVisible - lastChanged >= shownFor ? Visibility.HIDE : Visibility.SHOW;
    }
}
