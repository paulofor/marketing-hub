package com.marketinghub.videomanagement.service.provider;

import java.util.List;
import java.util.Map;

/** Responsabilidade: preservar respostas já consumidas quando um gate bloqueia a finalização. */
public class AuditedVideoProviderException extends VideoProviderException {
    private final ProviderArtifacts auditArtifacts;

    /** Mantém o motivo original e os binários recebidos, sem representar vídeo aprovado. */
    private AuditedVideoProviderException(String code, String message, Throwable cause, ProviderArtifacts auditArtifacts) {
        super(code, message, cause);
        this.auditArtifacts = auditArtifacts;
    }

    /** Acrescenta somente auditorias disponíveis e evita embrulhar novamente uma falha auditada. */
    public static VideoProviderException preserve(Long jobId, Exception cause,
            List<ProviderFile> files, List<Map<String, Object>> interactions) {
        if (cause instanceof AuditedVideoProviderException audited) return audited;
        String code = cause instanceof VideoProviderException provider ? provider.getCode() : "VIDEO_POST_PRODUCTION_FAILED";
        if (files.isEmpty()) return cause instanceof VideoProviderException provider
                ? provider : new VideoProviderException(code, cause.getMessage(), cause);
        return new AuditedVideoProviderException(code, cause.getMessage(), cause,
                new ProviderArtifacts("post-production-" + jobId, null, null, null,
                        Map.of("tts_interactions", List.copyOf(interactions), "audit_outcome", "BLOCKED"), files));
    }

    /** Entrega as respostas brutas para persistência antes do callback de falha. */
    public ProviderArtifacts auditArtifacts() {
        return auditArtifacts;
    }
}
