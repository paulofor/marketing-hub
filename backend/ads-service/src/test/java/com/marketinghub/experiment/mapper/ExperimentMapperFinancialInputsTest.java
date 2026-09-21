package com.marketinghub.experiment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.ads.mapper.FacebookInstantFormMapperImpl;
import com.marketinghub.experiment.Experiment;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Impede que o somador de custos seja usado como conversor global de campos financeiros ausentes.
 */
@SpringJUnitConfig({ExperimentMapperImpl.class, FacebookInstantFormMapperImpl.class})
class ExperimentMapperFinancialInputsTest {
  @Autowired private ExperimentMapper mapper;

  /** Preserva NULL em premissas e limites, distinguindo ausência de zero informado. */
  @Test
  void preservesUnknownFinancialInputsAndKeepsExplicitAggregation() {
    var dto = mapper.toDto(Experiment.builder().id(301L).build());
    assertThat(dto.getDailyBudget()).isNull();
    assertThat(dto.getMediaSpendLimit()).isNull();
    assertThat(dto.getBaselineCvr()).isNull();
    assertThat(dto.getTargetCvr()).isNull();
    assertThat(dto.getUnitPrice()).isNull();
    assertThat(dto.getCost()).isNull();
    assertThat(dto.getExpense()).isNull();
    assertThat(dto.getAuditableTotalCost()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  /** Mantém valores informados e reconciliação de custos sem depender de produto específico. */
  @Test
  void preservesExplicitZeroAndKnownAmounts() {
    var dto =
        mapper.toDto(
            Experiment.builder()
                .id(302L)
                .dailyBudget(new BigDecimal("12.50"))
                .mediaSpendLimit(new BigDecimal("75"))
                .baselineCvr(BigDecimal.ZERO)
                .targetCvr(new BigDecimal("4"))
                .cost(new BigDecimal("9.75"))
                .expense(new BigDecimal("2.25"))
                .build());
    assertThat(dto.getDailyBudget()).isEqualByComparingTo("12.50");
    assertThat(dto.getMediaSpendLimit()).isEqualByComparingTo("75");
    assertThat(dto.getBaselineCvr()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(dto.getTargetCvr()).isEqualByComparingTo("4");
    assertThat(dto.getAuditableTotalCost()).isEqualByComparingTo("12.00");
  }
}
