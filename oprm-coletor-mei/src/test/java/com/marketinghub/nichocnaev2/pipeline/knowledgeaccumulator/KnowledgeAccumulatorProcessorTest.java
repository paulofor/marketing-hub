package com.marketinghub.nichocnaev2.pipeline.knowledgeaccumulator;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a etapa 8 de acumulação de conhecimento do pipeline NichoCNAE v2. */
class KnowledgeAccumulatorProcessorTest {
    /** Garante que apenas claims validados com trecho exato viram fatos positivos no snapshot. */
    @Test
    void shouldBuildVersionedSnapshotOnlyFromValidatedClaimsWithExactSpan() {
        KnowledgeAccumulatorProcessor processor = new KnowledgeAccumulatorProcessor();
        StageResult result = processor.process(new StageContext("job-1", "stage-8", Map.of(
                "researchCycleId", 72,
                "candidateId", 31,
                "candidateVersion", 2,
                "knowledgeVersion", 4,
                "claims", List.of(
                        Map.of(
                                "claimId", 101,
                                "claimType", "ROUTINE_TASK",
                                "claimText", "agenda clientes por WhatsApp",
                                "exactEvidenceSpan", "agenda clientes por WhatsApp",
                                "sourceUrl", "https://exemplo.com/rotina",
                                "status", "ACCEPTED"),
                        Map.of(
                                "claimId", 102,
                                "claimType", "ECONOMIC_IMPACT",
                                "claimText", "perde dinheiro",
                                "status", "ACCEPTED")),
                "sources", List.of(Map.of("url", "https://exemplo.com/rotina", "contentHash", "abc")),
                "queries", List.of(Map.of("queryText", "rotina manicure agenda", "resultCount", 3)))));

        assertThat(result.status()).isEqualTo("KNOWLEDGE_SNAPSHOT_READY");
        assertThat(result.output()).containsEntry("knowledgeVersion", 5);
        Map<String, Object> snapshot = (Map<String, Object>) result.output().get("snapshot");
        assertThat((List<?>) snapshot.get("validatedFacts")).hasSize(1);
        assertThat((List<String>) snapshot.get("evidenceGaps")).contains("ECONOMIC_IMPACT_OR_WORKAROUND", "THREE_INDEPENDENT_DOMAINS");
        assertThat(result.output()).containsEntry("nextStageCode", "reprocess-controller");
    }

    /** Garante que fontes e claims rejeitados permanecem auditáveis para evitar repetição de erro. */
    @Test
    void shouldPreserveRejectedSourcesAndContradictedClaimsAsDiagnostics() {
        KnowledgeAccumulatorProcessor processor = new KnowledgeAccumulatorProcessor();
        StageResult result = processor.process(new StageContext("job-1", "stage-8", Map.of(
                "knowledgeVersion", 1,
                "claims", List.of(Map.of(
                        "claimId", 201,
                        "claimType", "PAIN",
                        "epistemicState", "CONTRADICTED",
                        "rejectionReason", "ATOR_ADJACENTE")),
                "sources", List.of(Map.of(
                        "url", "https://adult.example/conteudo",
                        "unsafe", true,
                        "rejectionReason", "UNSAFE_SOURCE")))));

        Map<String, Object> snapshot = (Map<String, Object>) result.output().get("snapshot");
        assertThat((List<?>) snapshot.get("rejectedClaims")).hasSize(1);
        assertThat((List<?>) snapshot.get("rejectedSources")).hasSize(1);
    }
}
