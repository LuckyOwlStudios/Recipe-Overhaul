package com.luckyowlstudios.pinrecipe;

import com.luckyowlstudios.pinrecipe.brewing.ClientBrewingKnowledge;
import com.luckyowlstudios.pinrecipe.network.SyncKnownBrewsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

public class PinRecipeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PinRecipeClient.init();
        KeyBindingHelper.registerKeyBinding(PinRecipeClient.PIN_KEY);

        ClientPlayNetworking.registerGlobalReceiver(SyncKnownBrewsPayload.TYPE,
                (payload, context) -> ClientBrewingKnowledge.handle(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientBrewingKnowledge.clear());

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            PinRecipeClient.onScreenInit(screen);
            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    PinRecipeClient.onScreenRender(s, graphics, mouseX, mouseY));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) ->
                    !PinRecipeClient.onKeyPressed(s, key, scancode, modifiers));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) ->
                    !PinRecipeClient.onMouseClicked(s, mouseX, mouseY, button));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, mouseX, mouseY, button) ->
                    !PinRecipeClient.onMouseReleased(s, mouseX, mouseY, button));
        });
    }
}
