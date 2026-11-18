package com.luckyowlstudios.codex.platform;

import com.luckyowlstudios.codex.Constants;
import com.luckyowlstudios.codex.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

/** Loads the loader-specific implementation of each service via META-INF/services. */
public final class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private Services() {
    }

    public static <T> T load(Class<T> clazz) {
        T service = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", service, clazz);
        return service;
    }
}
