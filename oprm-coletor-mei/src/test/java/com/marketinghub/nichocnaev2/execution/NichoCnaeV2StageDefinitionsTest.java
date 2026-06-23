package com.marketinghub.nichocnaev2.execution;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Valida que o catálogo v2 fica limitado à preparação de insumo para hipótese. */
class NichoCnaeV2StageDefinitionsTest {
    /** Garante que o executor não avance para etapas de dor, oferta ou materialização comercial. */
    @Test
    void shouldNotRegisterCommercialOrMaterializationStagesInV2Executor() {
        NichoCnaeV2StageDefinitions definitions = new NichoCnaeV2StageDefinitions();

        assertThat(definitions.all())
                .extracting(NichoCnaeV2StageDefinition::stageCode)
                .contains("candidate-generator", "adaptive-query-planner", "knowledge-accumulator")
                .doesNotContain("commercial-evidence-gate", "enriched-niche-materializer");
    }
}
