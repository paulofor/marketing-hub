package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.finance.CurrencyConversionProperties;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanMilestone;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Responsabilidade: validar a sincronizacao de valores executados do planejamento comercial. */
class CommercialPlanExecutionSyncServiceTest {
    /** Deve preencher custo de campanha, custo de IA, receita e quantidades no mes e no marco. */
    @Test
    void syncFillsMonthlyAndMilestoneActualValues() {
        CommercialPlan plan = CommercialPlan.builder()
                .id(1L)
                .name("Julho")
                .deadline(LocalDate.of(2026, 7, 31))
                .build();
        CommercialPlanMilestone milestone = CommercialPlanMilestone.builder()
                .id(2L)
                .plan(plan)
                .sequenceOrder(1)
                .code("W1")
                .name("Semana 1")
                .dueDate(LocalDate.of(2026, 7, 31))
                .build();
        FakeJdbcTemplate jdbcTemplate = new FakeJdbcTemplate(
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(81),
                3,
                2,
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(81),
                3,
                2);
        CurrencyConversionProperties properties = new CurrencyConversionProperties();
        properties.setUsdToBrl(BigDecimal.valueOf(5));
        CommercialPlanExecutionSyncService service =
                new CommercialPlanExecutionSyncService(jdbcTemplate, new CurrencyConversionService(properties));

        service.sync(plan, List.of(milestone));

        assertThat(plan.getActualCampaignCost()).isEqualByComparingTo("120.00");
        assertThat(plan.getActualAiCost()).isEqualByComparingTo("14.00");
        assertThat(plan.getActualTotalCost()).isEqualByComparingTo("134.00");
        assertThat(plan.getActualRevenue()).isEqualByComparingTo("81.00");
        assertThat(plan.getActualExperimentsCreated()).isEqualTo(3);
        assertThat(plan.getActualExperimentsPublished()).isEqualTo(2);
        assertThat(plan.getExecutionSyncedAt()).isNotNull();
        assertThat(milestone.getActualCampaignCost()).isEqualByComparingTo("120.00");
        assertThat(milestone.getActualAiCost()).isEqualByComparingTo("14.00");
        assertThat(milestone.getActualTotalCost()).isEqualByComparingTo("134.00");
        assertThat(milestone.getActualRevenue()).isEqualByComparingTo("81.00");
        assertThat(milestone.getActualExperimentsCreated()).isEqualTo(3);
        assertThat(milestone.getActualExperimentsPublished()).isEqualTo(2);
    }

    /** Responsabilidade: devolver respostas previsiveis para as consultas sequenciais do sincronizador. */
    private static class FakeJdbcTemplate extends JdbcTemplate {
        private final Queue<Object> values = new ArrayDeque<>();

        /** Inicializa a fila de resultados SQL simulados. */
        FakeJdbcTemplate(Object... values) {
            this.values.addAll(List.of(values));
        }

        /** Retorna o proximo valor da fila respeitando o tipo solicitado. */
        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            return requiredType.cast(values.remove());
        }
    }
}
