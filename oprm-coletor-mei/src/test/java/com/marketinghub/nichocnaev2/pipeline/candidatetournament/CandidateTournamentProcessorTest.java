package com.marketinghub.nichocnaev2.pipeline.candidatetournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CandidateTournamentProcessorTest {
    @Test
    void shouldSelectUpToThreeOperationalFinalistsFromDiscoverySignals() {
        CandidateTournamentProcessor processor = new CandidateTournamentProcessor();

        StageResult result = processor.process(new StageContext(
                "job-80",
                "stage-4",
                Map.of("candidates", List.of(
                        Map.of("candidateId", "a", "operator", "OWNER_OPERATOR", "job", "ATENDIMENTO", "operationalContext", "Atendimento local", "directEvidenceCount", 1),
                        Map.of("candidateId", "b", "operator", "OWNER_OPERATOR", "job", "ESTOQUE", "operationalContext", "Reposição de peças", "independentSourceCount", 1),
                        Map.of("candidateId", "c", "operator", "STORE_ASSISTANT", "job", "VITRINE", "operationalContext", "Exposição visual")))));

        assertThat(result.status()).isEqualTo("FINALISTS_SELECTED");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> finalists = (List<Map<String, Object>>) result.output().get("finalists");
        assertThat(finalists).hasSize(3);
        assertThat(result.output()).containsEntry("nextStageCode", "source-fetcher-reranker");
    }

    @Test
    void shouldAllowNoViableSubnicheWhenOperationalClarityIsMissing() {
        CandidateTournamentProcessor processor = new CandidateTournamentProcessor();

        StageResult result = processor.process(new StageContext(
                "job-81",
                "stage-4",
                Map.of("candidates", List.of(
                        Map.of("candidateId", "a", "directEvidenceCount", 1, "independentSourceCount", 0),
                        Map.of("candidateId", "b", "directEvidenceCount", 0, "independentSourceCount", 1)))));

        assertThat(result.status()).isEqualTo("NO_VIABLE_SUBNICHE");
        assertThat(result.output()).containsEntry("finalistCount", 0);
        assertThat(result.output()).containsEntry("gateDecision", "NO_VIABLE_SUBNICHE");
        assertThat(result.output()).containsEntry("reasonCode", "NO_VIABLE_SUBNICHE");
        assertThat(result.output()).containsEntry("nextStageCode", "reprocess-controller");
    }

    @Test
    void shouldRejectMissingCandidatesAsInvalidStageContract() {
        CandidateTournamentProcessor processor = new CandidateTournamentProcessor();

        assertThatThrownBy(() -> processor.process(new StageContext("job-82", "stage-4", Map.of("plannedQueries", List.of()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Candidate Tournament exige candidates");
    }

    @Test
    void shouldAcceptRankedCandidatesWhenReprocessingTournament() {
        CandidateTournamentProcessor processor = new CandidateTournamentProcessor();

        StageResult result = processor.process(new StageContext(
                "job-83",
                "stage-4",
                Map.of("rankedCandidates", List.of(
                        Map.of("candidateId", "a", "operator", "OWNER_OPERATOR", "job", "ATENDIMENTO", "operationalContext", "Atendimento local")))));

        assertThat(result.status()).isEqualTo("FINALISTS_SELECTED");
        assertThat(result.output()).containsEntry("candidateCount", 1);
    }
}
