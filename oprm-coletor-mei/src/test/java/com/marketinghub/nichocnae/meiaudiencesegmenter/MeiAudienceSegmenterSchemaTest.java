package com.marketinghub.nichocnae.meiaudiencesegmenter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o schema estrito da segmentação MEI/autônomo antes de chamar a IA. */
class MeiAudienceSegmenterSchemaTest {
    private final MeiAudienceSegmenterSchema schemaBuilder = new MeiAudienceSegmenterSchema();

    /** Deve exigir todos os campos comportamentais sem permitir campos comerciais ou extras no JSON final. */
    @Test
    void shouldBuildStrictBehavioralSchemaWithoutCommercialFields() {
        Map<String, Object> schema = schemaBuilder.buildSchema();

        assertThat(schema).containsEntry("type", "object").containsEntry("additionalProperties", false);
        assertThat(requiredFields(schema))
                .contains(
                        "audienceName",
                        "occupationTerms",
                        "workMode",
                        "customerAcquisitionBehavior",
                        "dailyRoutineSummary",
                        "recurringTasksSummary",
                        "operationalPainsSummary",
                        "emotionalPainsSummary",
                        "dreamsSummary",
                        "fearsSummary",
                        "languagePatterns",
                        "channelsUsed",
                        "recentSourceSummary",
                        "autonomousProfessionalFitScore",
                        "behavioralEvidenceScore",
                        "sourceFreshnessScore",
                        "outdatedSourceRiskScore",
                        "structuredBusinessDriftRiskScore",
                        "solutionLanguageRiskScore")
                .doesNotContain("product", "offer", "campaign", "promise", "landingPage", "price");
        assertThat(properties(schema).keySet())
                .containsExactlyInAnyOrderElementsOf(requiredFields(schema));
    }

    /** Lê com cast controlado a lista de campos obrigatórios do schema montado para a OpenAI. */
    @SuppressWarnings("unchecked")
    private List<String> requiredFields(Map<String, Object> schema) {
        return (List<String>) schema.get("required");
    }

    /** Lê com cast controlado o mapa de propriedades do schema montado para a OpenAI. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> properties(Map<String, Object> schema) {
        return (Map<String, Object>) schema.get("properties");
    }
}
