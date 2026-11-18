package com.luckyowlstudios.codex.mixin;

import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(RecipeBookPage.class)
public interface RecipeBookPageAccessor {
    @Nullable
    @Accessor("hoveredButton")
    RecipeButton codex$getHoveredButton();

    @Accessor("buttons")
    List<RecipeButton> codex$getButtons();

    @Accessor("overlay")
    OverlayRecipeComponent codex$getOverlay();
}
