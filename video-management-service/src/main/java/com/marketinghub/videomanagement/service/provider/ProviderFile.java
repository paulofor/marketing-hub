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
                           byte[] content,
                           String externalUrl) {

    /** Cria arquivo com conteúdo binário local para envio ao R2. */
    public ProviderFile(String fileName,
                        MediaType mediaType,
                        AssetType assetType,
                        ProviderAssetRole role,
                        byte[] content) {
        this(fileName, mediaType, assetType, role, content, null);
    }

    /** Cria arquivo que já possui URL pública externa, sem tráfego binário interno. */
    public static ProviderFile remote(String fileName,
                                      MediaType mediaType,
                                      AssetType assetType,
                                      ProviderAssetRole role,
                                      String externalUrl) {
        return new ProviderFile(fileName, mediaType, assetType, role, null, externalUrl);
    }
}
