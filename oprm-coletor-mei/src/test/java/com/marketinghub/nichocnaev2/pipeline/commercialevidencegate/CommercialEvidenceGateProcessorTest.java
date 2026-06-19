package com.marketinghub.nichocnaev2.pipeline.commercialevidencegate;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommercialEvidenceGateProcessorTest {
    @Test
    void shouldAllowAutomaticMaterializationOnlyFromEconomicEvidenceLevel() {
        CommercialEvidenceGateProcessor processor = new CommercialEvidenceGateProcessor();

        StageResult result = processor.process(new StageContext(
                "job-120",
                "stage-6",
                Map.of(
                        "materializationEnabled", true,
                        "claims", List.of(
                                claim("TASK", "a.com"),
                                claim("TASK", "b.com"),
                                claim("TASK", "c.com"),
                                claim("PAIN", "a.com"),
                                claim("PRACTICAL_PAIN", "b.com"),
                                claim("ECONOMIC_IMPACT", "c.com")))));

        assertThat(result.status()).isEqualTo("MATERIALIZE");
        assertThat(result.output()).containsEntry("evidenceLevel", "E3_ECONOMIC_PAIN");
        assertThat(result.output()).containsEntry("automaticMaterializationAllowed", true);
        assertThat(result.output()).containsEntry("nextStageCode", "enriched-niche-materializer");
    }

    @Test
    void shouldKeepPainSeparatedFromPurchaseIntentAndAskMoreResearch() {
        CommercialEvidenceGateProcessor processor = new CommercialEvidenceGateProcessor();

        StageResult result = processor.process(new StageContext(
                "job-121",
                "stage-6",
                Map.of(
                        "previousEvidenceLevel", "E1_ACTIVITY_EXISTS",
                        "claims", List.of(
                                claim("TASK", "a.com"),
                                claim("PAIN", "a.com"),
                                claim("PAIN", "b.com")))));

        assertThat(result.status()).isEqualTo("NEEDS_MORE_RESEARCH");
        assertThat(result.output()).containsEntry("evidenceLevel", "E2_ROUTINE_AND_PAIN");
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) result.output().get("missingEvidence");
        assertThat(missing).contains("ECONOMIC_IMPACT_OR_WORKAROUND", "DIRECT_PURCHASE_INTENT_OR_HIRING_BEHAVIOR");
    }

    @Test
    void shouldNotCountClaimWithoutExactEvidenceSpan() {
        CommercialEvidenceGateProcessor processor = new CommercialEvidenceGateProcessor();

        StageResult result = processor.process(new StageContext(
                "job-122",
                "stage-6",
                Map.of("claims", List.of(Map.of(
                        "claimType", "ECONOMIC_IMPACT",
                        "status", "ACCEPTED",
                        "epistemicState", "VALIDATED",
                        "canonicalDomain", "a.com")))));

        assertThat(result.status()).isEqualTo("NO_PUBLIC_EVIDENCE");
        assertThat(result.output()).containsEntry("evidenceLevel", "E0_MODEL_HYPOTHESIS");
    }

    @Test
    void shouldRequireIndependentDomainsBeforeAutomaticMaterialization() {
        CommercialEvidenceGateProcessor processor = new CommercialEvidenceGateProcessor();

        StageResult result = processor.process(new StageContext(
                "job-123",
                "stage-7",
                Map.of(
                        "materializationEnabled", true,
                        "claims", List.of(
                                claim("TASK", "a.com"),
                                claim("TASK", "a.com"),
                                claim("TASK", "b.com"),
                                claim("PAIN", "a.com"),
                                claim("PRACTICAL_PAIN", "b.com"),
                                claim("ECONOMIC_IMPACT", "b.com")))));

        assertThat(result.status()).isEqualTo("NEEDS_MORE_RESEARCH");
        assertThat(result.output()).containsEntry("evidenceLevel", "E2_ROUTINE_AND_PAIN");
        assertThat(result.output()).containsEntry("automaticMaterializationAllowed", false);
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) result.output().get("missingEvidence");
        assertThat(missing).contains("THREE_INDEPENDENT_DOMAINS");
    }

    @Test
    void shouldSendContradictoryEconomicEvidenceToHumanReviewInsteadOfMaterializing() {
        CommercialEvidenceGateProcessor processor = new CommercialEvidenceGateProcessor();

        StageResult result = processor.process(new StageContext(
                "job-124",
                "stage-7",
                Map.of(
                        "materializationEnabled", true,
                        "claims", List.of(
                                claim("TASK", "a.com"),
                                claim("TASK", "b.com"),
                                claim("TASK", "c.com"),
                                claim("PAIN", "a.com"),
                                claim("PRACTICAL_PAIN", "b.com"),
                                claim("ECONOMIC_IMPACT", "c.com"),
                                Map.of(
                                        "claimType", "ECONOMIC_IMPACT",
                                        "status", "REJECTED",
                                        "epistemicState", "CONTRADICTED",
                                        "exactEvidenceSpan", "trecho literal que contradiz o impacto econômico",
                                        "canonicalDomain", "d.com")))));

        assertThat(result.status()).isEqualTo("HUMAN_REVIEW");
        assertThat(result.output()).containsEntry("automaticMaterializationAllowed", false);
        assertThat(result.output()).containsEntry("humanReviewRequired", true);
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) result.output().get("missingEvidence");
        assertThat(missing).contains("RESOLVE_CONTRADICTORY_EVIDENCE");
    }

    private Map<String, Object> claim(String type, String domain) {
        return Map.of(
                "claimType", type,
                "status", "ACCEPTED",
                "epistemicState", "VALIDATED",
                "exactEvidenceSpan", "trecho literal da fonte com evidência operacional suficiente",
                "canonicalDomain", domain);
    }
}
