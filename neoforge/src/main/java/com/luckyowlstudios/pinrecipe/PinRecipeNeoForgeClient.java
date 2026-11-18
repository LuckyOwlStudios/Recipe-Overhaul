package com.luckyowlstudios.pinrecipe;

import com.luckyowlstudios.pinrecipe.brewing.ClientBrewingKnowledge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only setup: pin key, screen hooks for the pinned recipe overlay and the brewing recipe book. */
@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class PinRecipeNeoForgeClient {
    public PinRecipeNeoForgeClient(IEventBus modBus) {
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(PinRecipeClient.PIN_KEY));

        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> ClientBrewingKnowledge.clear());

        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, event -> PinRecipeClient.onScreenInit(event.getScreen()));

        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, event ->
                PinRecipeClient.onScreenRender(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY()));

        NeoForge.EVENT_BUS.addListener(ScreenEvent.KeyPressed.Pre.class, event -> {
            if (PinRecipeClient.onKeyPressed(event.getScreen(), event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
                event.setCanceled(true);
            }
        });

        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonPressed.Pre.class, event -> {
            if (PinRecipeClient.onMouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
                event.setCanceled(true);
            }
        });

        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonReleased.Pre.class, event -> {
            if (PinRecipeClient.onMouseReleased(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
                event.setCanceled(true);
            }
        });
    }
}
