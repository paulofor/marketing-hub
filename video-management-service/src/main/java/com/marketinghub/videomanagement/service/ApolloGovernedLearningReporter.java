package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.payload.ApolloLearningObservationPayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Responsabilidade: integrar o replay híbrido ao job real de Apolo sem controlar provider ou orçamento. */
@Service
public class ApolloGovernedLearningReporter {
    private static final Logger log = LoggerFactory.getLogger(ApolloGovernedLearningReporter.class);
    private final ApolloHybridShadowReplay replay;
    private final BackendVideoClient backend;
    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;

    /** Configura replay, callback canônico e contratos versionados de Apolo. */
    public ApolloGovernedLearningReporter(ApolloHybridShadowReplay replay, BackendVideoClient backend,
                                          VideoManagementProperties properties, ObjectMapper objectMapper) {
        this.replay = replay;
        this.backend = backend;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Executa e registra o replay quando habilitado, sem modificar a decisão produtiva do job. */
    public void observe(SalesVideoJob original, SalesVideoJob planned) {
        if (!properties.getApolloPlanner().getCodexShadow().isEnabled()) return;
        try {
            JsonNode frozen = objectMapper.readTree(original.metadataJson());
            JsonNode baseline = objectMapper.readTree(planned.metadataJson()).path("apollo_ai_plan");
            ApolloHybridShadowReplay.HybridComparison result =
                    replay.compare(original.id(), frozen, baseline, original.providerName());
            var current = result.comparison().current();
            var candidate = result.comparison().candidate();
            String scopeId = frozen.path("studio_project_id").asText("job-" + original.id());
            backend.reportApolloLearning(new ApolloLearningObservationPayload(
                    original.id(), scopeId, properties.getApolloPlanner().getModel(), result.candidateModel(),
                    java.math.BigDecimal.valueOf(current.qualityScore()),
                    java.math.BigDecimal.valueOf(candidate.qualityScore()), current.expectedCostUsd(),
                    candidate.expectedCostUsd(), objectMapper.writeValueAsString(result),
                    result.providerCalled(), result.spendingAuthorized(), false));
        } catch (Exception ex) {
            log.error("Falha no replay governado de Apolo; jobId={}; provider pago não foi liberado pelo replay",
                    original.id(), ex);
        }
    }
}
