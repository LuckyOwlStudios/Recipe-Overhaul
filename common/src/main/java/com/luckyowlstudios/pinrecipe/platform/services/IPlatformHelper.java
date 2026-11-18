package com.luckyowlstudios.pinrecipe.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {
    /** Name of the running mod loader, e.g. "Fabric" or "NeoForge". */
    String getPlatformName();

    /** The game's config directory. */
    Path getConfigDir();
}
