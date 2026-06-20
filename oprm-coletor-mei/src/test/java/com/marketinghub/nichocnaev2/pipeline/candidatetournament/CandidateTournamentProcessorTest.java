package com.marketinghub.nichocnaev2.pipeline.candidatetournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CandidateTournamentProcessorTest {
    @Test
    void shouldSelectUpToTwoFinalistsFromObservedEvidence() {
        CandidateTournamentProcessor processor = new CandidateTournamentProcessor();

        StageResult result = processor.process(new StageContext(
                "job-80",
                "stage-4",
                Map.of("candidates", List.of(
                        Map.of("candidateId", "a", "directEvidenceCount", 4, "independentSourceCount", 3),
                        Map.of("candidateId", "b", "directEvidenceCount", 3, "independentSourceCount", 2),
                        Map.of("candidateId", "c", "directEvidenceCount", 1, "independentSourceCount", 1)))));

        assertThat(result.status()).isEqualTo("FINALISTS_SELECTED");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> finalists = (List<Map<String, Object>>) result.output().get("finalists");
        assertThat(finalists).hasSize(2);
        assertThat(result.output()).containsEntry("nextStageCode", "source-fetcher-reranker");
    }

    @Test
    void shouldAllowNoViableSubnicheWhenEvidenceIsWeak() {
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
}
