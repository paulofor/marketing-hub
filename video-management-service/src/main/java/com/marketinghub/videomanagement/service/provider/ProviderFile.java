package com.marketinghub.videomanagement.service.provider;

import com.marketinghub.videomanagement.client.dto.AssetType;
import org.springframework.http.MediaType;

/**
 * Representa um arquivo gerado pelo provider antes do upload ao backend.
 */
public record ProviderFile(String fileName,
                           MediaType mediaType,
                           AssetType assetType,
                           ProviderAssetRole role,
                           byte[] content) {
}
