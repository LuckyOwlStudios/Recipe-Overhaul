package com.luckyowlstudios.codex.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface IPlatformHelper {
    /** Name of the running mod loader, e.g. "Fabric" or "NeoForge". */
    String getPlatformName();

    /** The game's config directory. */
    Path getConfigDir();

    /** Sends a payload to one player, if their client has the mod. */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
