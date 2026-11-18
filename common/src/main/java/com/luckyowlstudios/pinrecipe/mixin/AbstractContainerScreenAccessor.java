package com.luckyowlstudios.pinrecipe.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int pinrecipe$getLeftPos();

    @Accessor("topPos")
    int pinrecipe$getTopPos();

    @Accessor("imageWidth")
    int pinrecipe$getImageWidth();
}
