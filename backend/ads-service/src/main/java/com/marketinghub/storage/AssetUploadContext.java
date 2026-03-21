package com.marketinghub.storage;

/**
 * Informações auxiliares para nomear e organizar uploads de assets.
 */
public record AssetUploadContext(
        AssetUploadCategory category,
        Long experimentId,
        Long flowId,
        String flowSlug) {

    public AssetUploadContext {
        if (category == null) {
            category = AssetUploadCategory.GENERIC;
        }
    }
}
