package com.marketinghub.facebookadsworker.util;

/** Utility methods for composing URLs. */
public final class UrlUtils {
    private UrlUtils() {
    }

    /**
     * Joins a base URL, a prefix and a path ensuring no duplicated slashes are generated.
     */
    public static String joinPath(String base, String prefix, String path) {
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String p = prefix.startsWith("/") ? prefix : "/" + prefix;
        p = p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
        String r = path.startsWith("/") ? path : "/" + path;
        return b + p + r;
    }
}

