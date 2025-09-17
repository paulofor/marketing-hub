package com.marketinghub.worker.util;

/** Utility methods for composing URLs. */
public final class UrlUtils {
    private UrlUtils() {
    }

    /**
     * Joins base URL, API prefix and path ensuring the resulting value does not
     * contain duplicated slashes.
     */
    public static String joinPath(String base, String prefix, String path) {
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPrefix = prefix.startsWith("/") ? prefix : "/" + prefix;
        normalizedPrefix = normalizedPrefix.endsWith("/")
                ? normalizedPrefix.substring(0, normalizedPrefix.length() - 1)
                : normalizedPrefix;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPrefix + normalizedPath;
    }
}

