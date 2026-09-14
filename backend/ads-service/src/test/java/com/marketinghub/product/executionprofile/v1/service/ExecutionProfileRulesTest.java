package com.marketinghub.product.executionprofile.v1.service;

import static org.assertj.core.api.Assertions.*;

import com.marketinghub.product.executionprofile.v1.service.saveprofile.ProfileContract;
import com.marketinghub.product.executionprofile.v1.service.saveprofile.ProfileContract.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Responsabilidade: comprovar especialização por capacidade e economia do pacote completo. */
class ExecutionProfileRulesTest {
  /** Constrói uma ficha sintética com custos conhecidos e três cenários independentes. */
  static ProfileContract contract(Capability capability) {
    return new ProfileContract(
        "fixture-v1",
        capability,
        "Pacote útil ao cliente",
        List.of("Referências consentidas"),
        List.of("Duas imagens utilizáveis"),
        List.of("Fidelidade e legibilidade"),
        "Entrega autenticada",
        "Compra única",
        false,
        2,
        4,
        "gpt-image-2.5-sunburst",
        "fixture-rate-v1",
        new BigDecimal("5"),
        new BigDecimal("2"),
        new BigDecimal("8"),
        new BigDecimal("20"),
        new ProductionBudget("gpt-image-2.5-sunburst", 4, new BigDecimal("2"), new BigDecimal("8")),
        Arrays.stream(ScenarioCode.values())
            .map(
                code ->
                    new Scenario(
                        code,
                        new BigDecimal("100"),
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        new BigDecimal("2"),
                        new BigDecimal("1"),
                        new BigDecimal("4")))
            .toList());
  }

  /** Todos os percursos mantêm as seis fases e os controles obrigatórios. */
  @ParameterizedTest
  @EnumSource(Capability.class)
  void preservesCommonChainAndHumanGates(Capability capability) {
    var c = contract(capability);
    var work = ExecutionProfileRules.work(c);
    assertThat(work.stream().map(w -> w.processCode()).distinct())
        .containsExactlyElementsOf(ExecutionProfileRules.PHASES);
    assertThat(work.stream().filter(w -> !w.applicable()).map(w -> w.activityId()))
        .containsExactly("audiovisual");
    assertThat(
            work.stream()
                .filter(w -> w.activityId().equals("homologation"))
                .findFirst()
                .orElseThrow()
                .requirements())
        .contains("Preservar todos os gates humanos");
    assertThat(ExecutionProfileRules.blockers(c)).isEmpty();
  }

  /**
   * Quantifica o custo integral com regenerações e demais despesas, sem usar uma geração isolada.
   */
  @Test
  void countsWholePackageAndAllCostComponents() {
    var result = ExecutionProfileRules.economics(contract(Capability.PERSONALIZED_IMAGES));
    assertThat(result).hasSize(3);
    assertThat(result.getFirst().fullCostBrl()).isEqualByComparingTo("30");
    assertThat(result.getFirst().contributionBrl()).isEqualByComparingTo("70");
  }

  /** Impede tratar a ausência de custo e cenários como uma margem válida. */
  @Test
  void validatesRequiredCostsAndScenarios() {
    var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
    var c = contract(Capability.AI_TOOL);
    var invalid =
        new ProfileContract(
            c.productVersion(),
            c.capability(),
            c.purchasedOutcome(),
            c.inputs(),
            c.deliverables(),
            c.qualityCriteria(),
            c.deliveryMode(),
            c.revenueModel(),
            false,
            2,
            1,
            c.costModel(),
            c.pricingRevision(),
            c.usdBrl(),
            null,
            c.maximumDeliveryCostBrl(),
            c.minimumContributionBrl(),
            c.productionBudget(),
            List.of());
    assertThat(validator.validate(invalid)).isNotEmpty();
  }

  /** Não permite esconder prejuízo de um cenário nem repetir o cenário favorável três vezes. */
  @Test
  void rejectsLossesAndDuplicateScenarios() {
    var c = contract(Capability.DIGITAL_PACKAGE);
    var bad =
        new Scenario(
            ScenarioCode.FAVORABLE,
            BigDecimal.ONE,
            BigDecimal.TEN,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    var invalid =
        new ProfileContract(
            c.productVersion(),
            c.capability(),
            c.purchasedOutcome(),
            c.inputs(),
            c.deliverables(),
            c.qualityCriteria(),
            c.deliveryMode(),
            c.revenueModel(),
            false,
            2,
            4,
            c.costModel(),
            c.pricingRevision(),
            c.usdBrl(),
            c.maximumAttemptCostBrl(),
            c.maximumDeliveryCostBrl(),
            c.minimumContributionBrl(),
            c.productionBudget(),
            List.of(bad, bad, bad));
    assertThat(ExecutionProfileRules.blockers(invalid)).hasSize(2);
  }
}
