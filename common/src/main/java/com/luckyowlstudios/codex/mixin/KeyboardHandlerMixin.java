package com.luckyowlstudios.codex.mixin;

import com.luckyowlstudios.codex.client.BrewingRecipeBook;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sends typed characters to the brewing recipe book's search box while it has focus. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void codex$typeInBrewingSearch(long window, int codePoint, int modifiers, CallbackInfo ci) {
        boolean used = false;
        for (char c : Character.toChars(codePoint)) {
            used |= BrewingRecipeBook.charTyped(c, modifiers);
        }
        if (used) {
            ci.cancel();
        }
    }
}
