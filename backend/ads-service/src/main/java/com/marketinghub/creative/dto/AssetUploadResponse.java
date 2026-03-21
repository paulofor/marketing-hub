package com.marketinghub.creative.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.storage.AssetUploadCategory;

/**
 * Resposta padronizada para uploads de assets.
 */
public record AssetUploadResponse(String url,
                                  @JsonProperty("stored_file_name") String storedFileName,
                                  AssetUploadCategory category) {
}
