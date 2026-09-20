package com.marketinghub.financialplan.v1.service;

import com.marketinghub.financialplan.v1.service.prepareplan.PreparePlanRequest;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions.*;
import com.marketinghub.planning.CommercialPlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;

/** Responsabilidade: compor premissas com escolhas simples sem inventar custos ou aprovações. */
final class FinancialPlanPreparation {
  /** Impede instanciação do preparador puro. */
  private FinancialPlanPreparation() {}

  /** Reaproveita fontes compatíveis e invalida custos cuja base operacional foi modificada. */
  static PlanAssumptions prepare(
      PlanAssumptions source, PreparePlanRequest request, CommercialPlan plan, BigDecimal price) {
    var today = LocalDate.now(ZoneOffset.UTC);
    var previous = source == null ? null : source.preparation();
    var ai = source == null ? emptyAi(null) : source.ai();
    if (!request.personalizedAi()) {
      ai =
          new AiCost(
              Currency.BRL,
              "Sem geração personalizada de IA por cliente",
              BigDecimal.ZERO,
              "Escolha explícita na preparação simplificada; investimento inicial permanece separado.",
              today,
              null,
              null,
              ai.includedUnits() == null ? 1 : ai.includedUnits(),
              0,
              BigDecimal.ZERO);
    } else if (ai.perAttempt() != null && ai.perAttempt().signum() == 0) {
      ai = emptyAi(ai.includedUnits());
    }
    var c =
        source == null
            ? new Costs(null, null, null, null, null, null, null, null, null, null, null, null)
            : source.costs();
    if (previous == null || !previous.supportDays().equals(request.supportDays()))
      c =
          new Costs(
              c.feePercent(),
              c.taxPercent(),
              c.commissionPercent(),
              c.refundPercent(),
              c.fixedFeeBrl(),
              null,
              c.storageBrl(),
              c.deliveryBrl(),
              c.otherVariableBrl(),
              c.initialAiBrl(),
              c.initialOtherBrl(),
              c.fixedPerPeriodBrl());
    final var selectedAi = ai;
    var scenarios =
        Arrays.stream(ScenarioCode.values())
            .map(
                code -> {
                  var old =
                      source == null
                          ? null
                          : source.scenarios().stream()
                              .filter(s -> s.code() == code)
                              .findFirst()
                              .orElse(null);
                  Integer attempts = null;
                  if (!request.personalizedAi()) attempts = 0;
                  else if (selectedAi.perAttempt() != null && old != null)
                    attempts = old.attemptsPerCustomer();
                  return new Scenario(
                      code,
                      old == null ? null : old.customers(),
                      attempts,
                      old == null ? null : old.cacBrl());
                })
            .toList();
    String note =
        "Preparação simplificada v1: suporte de "
            + request.supportDays()
            + " dias; geração personalizada com IA: "
            + (request.personalizedAi() ? "sim" : "não")
            + ". Plano comercial #"
            + plan.getId()
            + ", versão "
            + request.commercialPlanVersion()
            + ". Preço e CAC reaproveitados das referências cadastradas; custos sem fonte permanecem pendentes."
            + " O suporte não altera prazo de acesso nem contratos vendidos."
            + " O período econômico é distinto do suporte; quando ausente, a proposta inicial é 30 dias."
            + (!request.personalizedAi()
                ? " Sem IA variável: tentativas e tarifa zero; unidade é o pacote quando não definida."
                : "");
    return new PlanAssumptions(
        request.productVersion(),
        source == null || source.periodDays() == null ? 30 : source.periodDays(),
        source == null ? today.plusDays(7) : source.validUntil(),
        evidence(source, note),
        ai,
        c,
        price,
        source == null ? null : source.minimumMarginPercent(),
        source != null && source.maximumCacBrl() != null
            ? source.maximumCacBrl()
            : plan.getExpectedCacBrl(),
        scenarios,
        new Preparation(request.supportDays(), request.personalizedAi()));
  }

  /** Preserva o texto de origem sem acumular notas automáticas a cada gravação idêntica. */
  private static String evidence(PlanAssumptions source, String note) {
    String original = source == null ? "" : source.evidence();
    int marker = original.indexOf("\n\nPreparação simplificada v1:");
    if (marker >= 0) original = original.substring(0, marker);
    if (original.startsWith("Preparação simplificada v1:")) original = "";
    return original.isBlank() ? note : original + "\n\n" + note;
  }

  /** Mantém tarifa, provedor e limites desconhecidos até haver fonte financeira verificável. */
  private static AiCost emptyAi(Integer units) {
    return new AiCost(Currency.BRL, null, null, null, null, null, null, units, null, null);
  }
}
