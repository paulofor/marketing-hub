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

    /**
     * Joins base URL, API prefix and multiple path segments ensuring the resulting value does not
     * contain duplicated slashes.
     */
    public static String joinPath(String base, String prefix, String path, String... additionalPaths) {
        String result = joinPath(base, prefix, path);
        if (additionalPaths == null || additionalPaths.length == 0) {
            return result;
        }
        String joined = result;
        for (String segment : additionalPaths) {
            if (segment == null || segment.isEmpty()) {
                continue;
            }
            joined = joinPath(joined, "", segment);
        }
        return joined;
    }
}
