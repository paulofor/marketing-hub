package com.marketinghub.facebookadsworker.facebookcampaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Protege a autorização financeira quando o teto nativo da campanha é incompatível. */
class CampaignBudgetPolicyTest {
    /** O último dia de Vega recebe somente vinte reais, sem acumular quatro dias perdidos. */
    @Test
    void capsVegaRemainingDayWithoutRecoveringLostBudget() {
        assertEquals("2000", CampaignBudgetPolicy.remainingLifetimeBudget(new BigDecimal("20"),
                new BigDecimal("100"), "2026-09-02", "2026-09-06", Instant.parse("2026-09-06T22:30:00Z")));
    }

    /** Um período futuro mantém a média planejada e nunca aumenta o teto total. */
    @Test
    void boundsFutureWindowByExplicitTotal() {
        assertEquals("10000", CampaignBudgetPolicy.remainingLifetimeBudget(new BigDecimal("20"),
                new BigDecimal("100"), "2026-09-07", "2026-09-11", Instant.parse("2026-09-06T22:30:00Z")));
        assertEquals("10000", CampaignBudgetPolicy.remainingLifetimeBudget(new BigDecimal("20"),
                new BigDecimal("100"), "2026-09-07", "2026-09-30", Instant.parse("2026-09-06T22:30:00Z")));
    }

    /** Período encerrado bloqueia antes de qualquer tentativa sem spend_cap. */
    @Test
    void rejectsExpiredWindow() {
        assertThrows(IllegalArgumentException.class, () -> CampaignBudgetPolicy.remainingLifetimeBudget(
                new BigDecimal("20"), new BigDecimal("100"), "2026-09-02", "2026-09-06",
                Instant.parse("2026-09-07T03:00:00Z")));
    }

    /** Ausência de orçamento ou teto menor que o diário não admite fallback. */
    @Test
    void rejectsUnfundedFallback() {
        assertThrows(IllegalArgumentException.class, () -> CampaignBudgetPolicy.remainingLifetimeBudget(
                new BigDecimal("20"), new BigDecimal("10"), "2026-09-02", "2026-09-06",
                Instant.parse("2026-09-06T22:30:00Z")));
    }
}
