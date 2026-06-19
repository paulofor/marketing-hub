package com.marketinghub.nichocnaev2.pipeline.adaptivequeryplanner;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdaptiveQueryPlannerProcessorTest {
    @Test
    void shouldPlanOnlyEvidenceGapQueriesWithoutOfferLanguage() {
        AdaptiveQueryPlannerProcessor processor = new AdaptiveQueryPlannerProcessor();
        StageResult result = processor.process(new StageContext(
                "job-72",
                "stage-72",
                Map.of(
                        "audience", "Motoristas autônomos de transfer aeroportuário",
                        "jobContext", "corridas agendadas para passageiros e empresas",
                        "evidenceGaps", List.of("MISSING_EXECUTOR_ROUTINE_EVIDENCE", "impacto econômico sem oferta de produto"),
                        "previousQueryHashes", List.of())));

        assertThat(result.status()).isEqualTo("PLAN_READY");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> plannedQueries = (List<Map<String, Object>>) result.output().get("plannedQueries");
        assertThat(plannedQueries).isNotEmpty().hasSizeLessThanOrEqualTo(8);
        assertThat(plannedQueries)
                .extracting(query -> String.valueOf(query.get("query")))
                .allSatisfy(query -> assertThat(query).doesNotContain("oferta", "produto"));
    }

    @Test
    void shouldStopWhenMemoryShowsNoNewQueryGain() {
        AdaptiveQueryPlannerProcessor processor = new AdaptiveQueryPlannerProcessor();
        StageResult first = processor.process(new StageContext(
                "job-72",
                "stage-72",
                Map.of(
                        "audience", "Motoristas autônomos de transfer aeroportuário",
                        "evidenceGaps", List.of("cancelamento tardio"),
                        "previousQueryHashes", List.of())));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> planned = (List<Map<String, Object>>) first.output().get("plannedQueries");
        List<String> hashes = planned.stream().map(query -> String.valueOf(query.get("queryHash"))).toList();

        StageResult second = processor.process(new StageContext(
                "job-72",
                "stage-72",
                Map.of(
                        "audience", "Motoristas autônomos de transfer aeroportuário",
                        "evidenceGaps", List.of("cancelamento tardio"),
                        "previousQueryHashes", hashes)));

        assertThat(second.status()).isEqualTo("NO_RESEARCH_GAIN");
        assertThat(second.output()).containsEntry("earlyStopping", true);
    }
}
