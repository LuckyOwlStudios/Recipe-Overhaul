package com.luckyowlstudios.codex.mixin;

import com.luckyowlstudios.codex.client.BrewingRecipeBook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The vanilla brewing stand draws its background centred on screen instead of at leftPos, so when the
 * recipe book pushes the screen to the right we shift the background to match, then draw the book.
 */
@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin {
    @Inject(method = "renderBg", at = @At("HEAD"))
    private void codex$shiftBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        graphics.pose().pushPose();
        graphics.pose().translate(BrewingRecipeBook.backgroundShift((BrewingStandScreen) (Object) this), 0, 0);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void codex$drawBook(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        graphics.pose().popPose();
        BrewingRecipeBook.renderBackground((BrewingStandScreen) (Object) this, graphics, mouseX, mouseY);
    }
}
