package com.marketinghub.hypothesis.pain.service;

/** Responsabilidade: expor para o pipeline de hipótese o gate científico exigido antes da oferta. */
public interface HypothesisProductEvidenceGate {

    /** Garante que existe uma pesquisa científica iniciada para o nicho informado. */
    void ensureProductEvidenceStarted(Long marketNicheId);

    /** Bloqueia o avanço comercial quando o pacote científico ainda não foi aprovado. */
    void requireApprovedEvidencePack(Long marketNicheId);

    /** Informa se o nicho já possui pacote científico final concluído. */
    boolean hasApprovedEvidencePack(Long marketNicheId);
}
