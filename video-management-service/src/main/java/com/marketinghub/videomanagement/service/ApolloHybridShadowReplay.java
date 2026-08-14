package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.videomanagement.client.ApolloCodexShadowClient;
import org.springframework.stereotype.Service;

/** Responsabilidade: comparar o plano persistido da API com uma candidata Codex sem consumir mídia. */
@Service
public class ApolloHybridShadowReplay {
    private final ApolloCodexShadowClient codexClient;
    private final ApolloStoryboardShadowReplay replay;

    /** Combina o runner Codex isolado com a avaliação determinística já usada por Apolo. */
    public ApolloHybridShadowReplay(ApolloCodexShadowClient codexClient, ApolloStoryboardShadowReplay replay) {
        this.codexClient = codexClient;
        this.replay = replay;
    }

    /** Executa a candidata Codex e a compara com o plano API congelado sem chamar provider de vídeo. */
    public HybridComparison compare(Long jobId, JsonNode frozenMetadata, JsonNode persistedApiPlan,
                                    String providerName) {
        ApolloCodexShadowClient.CodexShadowResult codex =
                codexClient.plan(jobId, frozenMetadata, persistedApiPlan);
        if (codex.providerCalled() || codex.spendingAuthorized()) {
            throw new IllegalStateException("Replay sombra inválido por efeito externo");
        }
        ApolloStoryboardShadowReplay.ReplayComparison comparison =
                replay.compare(frozenMetadata, persistedApiPlan, codex.plan(), providerName);
        return new HybridComparison("OPENAI_API_PERSISTED", "CODEX_SESSION", codex.model(),
                true, false, false, comparison, codex.request(), codex.rawResponse());
    }

    /** Consolida origens, auditoria e decisão do replay híbrido. */
    public record HybridComparison(String baselineOrigin, String candidateOrigin, String candidateModel,
                                   boolean shadowMode, boolean providerCalled, boolean spendingAuthorized,
                                   ApolloStoryboardShadowReplay.ReplayComparison comparison,
                                   String candidateRequest, String candidateRawResponse) {}
}
