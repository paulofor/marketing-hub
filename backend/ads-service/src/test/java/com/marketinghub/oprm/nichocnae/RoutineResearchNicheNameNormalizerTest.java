package com.marketinghub.oprm.nichocnae;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.oprm.nichocnae.RoutineResearchNicheNameNormalizer.NormalizedNicheName;
import org.junit.jupiter.api.Test;

/** Valida a neutralização de nomes de nicho antes da pesquisa de realidade operacional. */
class RoutineResearchNicheNameNormalizerTest {
    private final RoutineResearchNicheNameNormalizer normalizer = new RoutineResearchNicheNameNormalizer();

    /** Deve remover enquadramento de IA e manter apenas a ocupação real como nome de pesquisa. */
    @Test
    void shouldRemoveIaGrowthPrefixFromNicheName() {
        NormalizedNicheName result = normalizer.normalize(
                "IA para crescimento de Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure");

        assertThat(result.originalNicheName()).isEqualTo("IA para crescimento de Cabeleireiros, manicure e pedicure");
        assertThat(result.neutralNicheName()).isEqualTo("Cabeleireiros, manicure e pedicure");
        assertThat(result.solutionLanguageRiskScore()).isEqualByComparingTo("100.00");
    }

    /** Deve usar a descrição CNAE quando a remoção do enquadramento deixa o nome operacional fraco. */
    @Test
    void shouldFallbackToCnaeDescriptionWhenNeutralNameIsWeak() {
        NormalizedNicheName result = normalizer.normalize("software para", "Manutenção de computadores");

        assertThat(result.neutralNicheName()).isEqualTo("Manutenção de computadores");
        assertThat(result.solutionLanguageRiskScore()).isEqualByComparingTo("100.00");
    }

    /** Deve preservar nomes já neutros sem elevar risco de contaminação por solução. */
    @Test
    void shouldKeepNeutralNicheName() {
        NormalizedNicheName result = normalizer.normalize("Cabeleireiros e manicures", "Cabeleireiros, manicure e pedicure");

        assertThat(result.originalNicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(result.neutralNicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(result.solutionLanguageRiskScore()).isEqualByComparingTo("0.00");
    }
}
