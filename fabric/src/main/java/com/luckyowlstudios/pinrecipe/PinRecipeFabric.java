package com.luckyowlstudios.pinrecipe;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

public class PinRecipeFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(PinRecipeClient.PIN_KEY);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    PinRecipeClient.onScreenRender(s, graphics, mouseX, mouseY));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) ->
                    !PinRecipeClient.onKeyPressed(s, key, scancode));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) ->
                    !PinRecipeClient.onMouseClicked(s, mouseX, mouseY, button));
        });
    }
}
