package com.luckyowlstudios.codex;

import com.luckyowlstudios.codex.brewing.ClientBrewingKnowledge;
import com.luckyowlstudios.codex.network.SyncKnownBrewsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

public class CodexFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CodexClient.init();
        KeyBindingHelper.registerKeyBinding(CodexClient.PIN_KEY);

        ClientPlayNetworking.registerGlobalReceiver(SyncKnownBrewsPayload.TYPE,
                (payload, context) -> ClientBrewingKnowledge.handle(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientBrewingKnowledge.clear());

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            CodexClient.onScreenInit(screen);
            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    CodexClient.onScreenRender(s, graphics, mouseX, mouseY));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) ->
                    !CodexClient.onKeyPressed(s, key, scancode, modifiers));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) ->
                    !CodexClient.onMouseClicked(s, mouseX, mouseY, button));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, mouseX, mouseY, button) ->
                    !CodexClient.onMouseReleased(s, mouseX, mouseY, button));
        });
    }
}
