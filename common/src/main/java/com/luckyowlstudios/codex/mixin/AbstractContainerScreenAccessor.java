package com.luckyowlstudios.codex.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int codex$getLeftPos();

    @Accessor("leftPos")
    void codex$setLeftPos(int leftPos);

    @Accessor("topPos")
    int codex$getTopPos();

    @Accessor("imageWidth")
    int codex$getImageWidth();
}
