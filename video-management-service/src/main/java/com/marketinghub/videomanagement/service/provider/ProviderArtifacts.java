package com.marketinghub.videomanagement.service.provider;

import java.util.List;
import java.util.Map;

/**
 * Resultado consolidado de uma execução de provider.
 */
public record ProviderArtifacts(String providerJobId,
                                ProviderFile videoFile,
                                ProviderFile posterFile,
                                ProviderFile captionFile,
                                Map<String, Object> metadata,
                                List<ProviderFile> auditFiles) {

    /** Mantém compatibilidade com providers que não produzem arquivos adicionais de auditoria. */
    public ProviderArtifacts(String providerJobId,
                             ProviderFile videoFile,
                             ProviderFile posterFile,
                             ProviderFile captionFile,
                             Map<String, Object> metadata) {
        this(providerJobId, videoFile, posterFile, captionFile, metadata, List.of());
    }

    /** Normaliza a coleção de auditoria para impedir nulos durante o upload. */
    public ProviderArtifacts {
        auditFiles = auditFiles == null ? List.of() : List.copyOf(auditFiles);
    }
}
