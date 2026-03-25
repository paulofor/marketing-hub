package com.marketinghub.storage;

import java.util.Locale;

/**
 * Categorias reconhecidas para upload de arquivos no storage compartilhado.
 */
public enum AssetUploadCategory {
    EXPERIMENT_CREATIVE("experiments/creatives"),
    LEAD_PORTAL_FORM("lead-portal/forms"),
    SALES_VIDEO("sales-videos"),
    GENERIC("uploads");

    private final String rootFolder;

    AssetUploadCategory(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public static AssetUploadCategory fromKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return GENERIC;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (AssetUploadCategory category : values()) {
            if (category.name().equals(normalized)) {
                return category;
            }
        }
        return GENERIC;
    }
}
