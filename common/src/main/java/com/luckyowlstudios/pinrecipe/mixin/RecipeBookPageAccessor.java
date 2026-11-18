package com.luckyowlstudios.pinrecipe.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookPage.class)
public interface RecipeBookPageAccessor {
    @Nullable
    @Accessor("hoveredButton")
    RecipeButton pinrecipe$getHoveredButton();
}
