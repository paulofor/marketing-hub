package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Valida a regra canônica de tentativas OpenAI Flex, Flex e Standard/default. */
class OpenAiServiceTierRetryPolicyTest {

    /** Garante que as três tentativas usam os tiers definidos para todo o sistema. */
    @Test
    void shouldUseFlexFlexAndStandardDefault() {
        assertThat(OpenAiServiceTierRetryPolicy.serviceTierForAttempt(1)).isEqualTo("flex");
        assertThat(OpenAiServiceTierRetryPolicy.serviceTierForAttempt(2)).isEqualTo("flex");
        assertThat(OpenAiServiceTierRetryPolicy.serviceTierForAttempt(3)).isEqualTo("default");
        assertThat(OpenAiServiceTierRetryPolicy.shouldOmitServiceTier(3)).isTrue();
    }
}
