package com.marketinghub.worker.openai.core.qualityreview;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.InvalidModelResponseException;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o gate determinístico da revisão comercial da landing. */
class QualityReviewResponseValidatorTest {

    private final QualityReviewResponseValidator validator =
            new QualityReviewResponseValidator(new ObjectMapper());

    /** Aceita score calibrado quando todas as dimensões passam e não há bloqueio real. */
    @Test
    void shouldApproveAtEightyFiveWithoutBlockingIssues() {
        assertThatCode(
                        () ->
                                validator.validateAndParse(
                                        response(85, "APPROVE_FOR_PUBLICATION", "[]", "[]", 8)))
                .doesNotThrowAnyException();
    }

    /** Impede que um score alto esconda falha de publicação. */
    @Test
    void shouldRejectApprovalWithBlockingIssue() {
        assertThatThrownBy(() -> validator.validateAndParse(
                        response(92, "APPROVE_FOR_PUBLICATION", "[\"checkout sem URL\"]", "[]", 9)))
                .isInstanceOf(InvalidModelResponseException.class);
    }

    /** Impede aprovação quando uma dimensão essencial não atinge o piso. */
    @Test
    void shouldRejectApprovalBelowEssentialCriteriaFloor() {
        assertThatThrownBy(
                        () ->
                                validator.validateAndParse(
                                        response(88, "APPROVE_FOR_PUBLICATION", "[]", "[]", 7)))
                .isInstanceOf(InvalidModelResponseException.class);
    }

    /** Exige causa bloqueante e etapa corretiva quando a recomendação for regenerar. */
    @Test
    void shouldRejectRegenerationWithoutBlockingCause() {
        assertThatThrownBy(() -> validator.validateAndParse(
                        response(82, "REGENERATE_BEFORE_PUBLICATION", "[]", "[]", 8)))
                .isInstanceOf(InvalidModelResponseException.class);
    }

    /** Monta uma resposta completa para exercitar as combinações do gate. */
    private String response(
            int score,
            String recommendation,
            String blockingIssues,
            String regeneration,
            int criterion) {
        return """
                {
                  "score": %d,
                  "targetAudienceSpecificity": "high",
                  "commercialReadiness": "strong",
                  "criteriaScores": {
                    "firstFoldClarity": %d,
                    "painResultMechanism": %d,
                    "proofStrength": %d,
                    "offerDesirability": %d,
                    "ctaAndFormStrength": %d,
                    "visualPremiumFeel": %d,
                    "mobileDesktopExecution": %d
                  },
                  "blockingIssues": %s,
                  "improvementOpportunities": ["testar variante futura de CTA"],
                  "recommendedRegeneration": %s,
                  "approvalRecommendation": "%s"
                }
                """
                .formatted(
                        score,
                        criterion,
                        criterion,
                        criterion,
                        criterion,
                        criterion,
                        criterion,
                        criterion,
                        blockingIssues,
                        regeneration,
                        recommendation);
    }
}
