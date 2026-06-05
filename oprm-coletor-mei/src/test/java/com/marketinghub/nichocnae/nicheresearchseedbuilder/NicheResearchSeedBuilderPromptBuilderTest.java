package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar que o prompt da etapa dois guia a IA para pesquisa de rotina sem viés de solução. */
class NicheResearchSeedBuilderPromptBuilderTest {
    private final NicheResearchSeedBuilderPromptBuilder promptBuilder = new NicheResearchSeedBuilderPromptBuilder();

    /** Deve declarar explicitamente rotina/dificuldades e proibir produto, oferta, ferramenta e solução. */
    @Test
    void shouldBuildRoutineRealityPromptWithoutSolutionSearch() {
        String prompt = promptBuilder.buildPrompt(pending());

        assertThat(prompt)
                .contains("pesquisar a rotina real do nicho CNAE")
                .contains("tarefas")
                .contains("dificuldades")
                .contains("Não proponha solução")
                .contains("Não procure produto")
                .contains("Não procure oferta")
                .contains("Não procure ferramenta")
                .contains("OPERATIONAL_DIFFICULTY_DISCOVERY")
                .doesNotContain("PRODUCT_SERVICE_DISCOVERY")
                .doesNotContain("OFFER_PATTERN_DISCOVERY")
                .doesNotContain("SALES_PAIN_DISCOVERY");
    }

    /** Cria uma pendência padrão para montagem determinística do prompt da etapa dois. */
    private NicheResearchSeedBuilderPending pending() {
        return new NicheResearchSeedBuilderPending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure",
                BigDecimal.valueOf(92),
                "AUTO_SCORE_QUEUE",
                "RUNNING",
                Instant.now(),
                Instant.now());
    }
}
