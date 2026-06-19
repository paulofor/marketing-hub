package com.marketinghub.nichocnaev2.pipeline.enrichednichematerializer;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnrichedNicheMaterializerProcessorTest {
    @Test
    void blocksMaterializationWhenFlagIsDisabledEvenWithE3Gate() {
        EnrichedNicheMaterializerProcessor processor = new EnrichedNicheMaterializerProcessor();

        StageResult result = processor.process(new StageContext("job-1", "12", Map.of(
                "materializationEnabled", false,
                "gateDecision", "MATERIALIZE",
                "validationLevel", "E3_ECONOMIC_PAIN",
                "confidence", 0.78)));

        assertThat(result.status()).isEqualTo("MATERIALIZATION_BLOCKED");
        assertThat(result.output()).containsEntry("materializationDecision", "DO_NOT_MATERIALIZE");
        assertThat(result.output().get("blockingReasons").toString()).contains("Feature flag");
    }

    @Test
    void materializesOnlyWhenFlagGateAndCommercialEvidenceArePresent() {
        EnrichedNicheMaterializerProcessor processor = new EnrichedNicheMaterializerProcessor();

        StageResult result = processor.process(new StageContext("job-1", "12", Map.of(
                "materializationEnabled", true,
                "gateDecision", "MATERIALIZE",
                "validationLevel", "E4_PURCHASE_INTENT",
                "confidence", 0.82,
                "executor", "Motoristas autônomos de transfer aeroportuário")));

        assertThat(result.status()).isEqualTo("ENRICHED_NICHE_READY");
        assertThat(result.output()).containsEntry("materializationDecision", "MATERIALIZE");
        assertThat(result.output().get("enrichedNiche")).isNotNull();
    }
}
