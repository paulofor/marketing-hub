package com.marketinghub.videomanagement.service.provider;

import java.util.Map;

/**
 * Resultado consolidado de uma execução de provider.
 */
public record ProviderArtifacts(String providerJobId,
                                ProviderFile videoFile,
                                ProviderFile posterFile,
                                ProviderFile captionFile,
                                Map<String, Object> metadata) {
}
