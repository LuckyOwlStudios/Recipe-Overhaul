package com.luckyowlstudios.pinrecipe.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
    @Accessor("recipeBookPage")
    RecipeBookPage pinrecipe$getRecipeBookPage();

    @Accessor("searchBox")
    EditBox pinrecipe$getSearchBox();

    @Accessor("ghostRecipe")
    GhostRecipe pinrecipe$getGhostRecipe();

    @Accessor("xOffset")
    int pinrecipe$getXOffset();
}
