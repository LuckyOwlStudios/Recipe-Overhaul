package com.luckyowlstudios.codex.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
    @Accessor("recipeBookPage")
    RecipeBookPage codex$getRecipeBookPage();

    @Accessor("searchBox")
    EditBox codex$getSearchBox();

    @Accessor("ghostRecipe")
    GhostRecipe codex$getGhostRecipe();

    @Accessor("xOffset")
    int codex$getXOffset();
}
