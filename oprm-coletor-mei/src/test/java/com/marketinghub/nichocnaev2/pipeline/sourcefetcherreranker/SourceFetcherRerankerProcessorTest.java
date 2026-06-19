package com.marketinghub.nichocnaev2.pipeline.sourcefetcherreranker;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SourceFetcherRerankerProcessorTest {
    @Test
    void shouldSelectDirectIndependentSourcesForSignalExtraction() {
        SourceFetcherRerankerProcessor processor = new SourceFetcherRerankerProcessor();

        StageResult result = processor.process(new StageContext(
                "job-90",
                "stage-5",
                Map.of("sources", List.of(
                        Map.of(
                                "url", "https://forum-exemplo.com/rotina",
                                "sourceDirectness", "DIRECT",
                                "actorMatch", 0.95,
                                "contextMatch", 0.9,
                                "supportedGoals", List.of("ROUTINE", "PAIN")),
                        Map.of(
                                "url", "https://blog-exemplo.com/analogia",
                                "sourceDirectness", "ANALOGY_ONLY",
                                "actorMatch", 0.3,
                                "contextMatch", 0.2,
                                "supportedGoals", List.of("ROUTINE"))))));

        assertThat(result.status()).isEqualTo("SOURCES_SELECTED");
        assertThat(result.output()).containsEntry("nextStageCode", "signal-extractor");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selected = (List<Map<String, Object>>) result.output().get("selectedSources");
        assertThat(selected).hasSize(2);
        assertThat(selected.get(0)).containsEntry("canonicalDomain", "forum-exemplo.com");
    }

    @Test
    void shouldReturnToAdaptivePlannerWhenEverySourceIsUnsafeOrDuplicated() {
        SourceFetcherRerankerProcessor processor = new SourceFetcherRerankerProcessor();

        StageResult result = processor.process(new StageContext(
                "job-91",
                "stage-5",
                Map.of(
                        "fetchedContentHashes", List.of("hash-a"),
                        "sources", List.of(
                                Map.of("url", "https://unsafe.example/a", "unsafe", true),
                                Map.of("url", "https://duplicate.example/a", "contentHash", "hash-a")))));

        assertThat(result.status()).isEqualTo("NO_FETCHABLE_DIRECT_SOURCE");
        assertThat(result.output()).containsEntry("selectedSourceCount", 0);
        assertThat(result.output()).containsEntry("nextStageCode", "adaptive-query-planner");
    }
}
