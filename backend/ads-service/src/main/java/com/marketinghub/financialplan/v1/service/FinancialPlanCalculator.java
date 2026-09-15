package com.marketinghub.financialplan.v1.service;

import com.marketinghub.financialplan.v1.service.getplan.PlanEvaluation;
import com.marketinghub.financialplan.v1.service.getplan.PlanEvaluation.ScenarioResult;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Responsabilidade: calcular economia determinística sem preencher custos ou receitas ausentes. */
public final class FinancialPlanCalculator {
  private static final BigDecimal HUNDRED = new BigDecimal("100");

  /** Impede instanciação do calculador sem estado. */
  private FinancialPlanCalculator() {}

  /** Calcula cenários declarados e estresse de consumo, preservando lacunas explícitas. */
  public static PlanEvaluation evaluate(PlanAssumptions p) {
    List<String> missing = missing(p);
    if (!missing.isEmpty())
      return new PlanEvaluation("MISSING_INPUTS", "Premissas incompletas", missing, List.of());
    List<String> blockers = new ArrayList<>();
    var ai = p.ai();
    BigDecimal attempt = attemptBrl(ai);
    if (ai.perAttempt().signum() == 0 && ai.maximumAttempts() != 0)
      blockers.add(
          "Plutus / Dédalo: custo de IA zero exige entrega sem chamadas de IA e tentativas zero.");
    if (ai.perAttempt().signum() > 0 && ai.maximumAttempts() < ai.includedUnits())
      blockers.add("Dédalo: as tentativas não cobrem a quantidade de resultados contratada.");
    if (attempt
            .multiply(BigDecimal.valueOf(ai.maximumAttempts()))
            .compareTo(ai.maximumCostPerCustomerBrl())
        > 0)
      blockers.add("Plutus / Dédalo: o teto por cliente não cobre todas as tentativas permitidas.");
    List<ScenarioResult> results = new ArrayList<>();
    for (var s : p.scenarios())
      results.add(calculate(p, s, s.code().name(), label(s.code().name())));
    var conservative =
        p.scenarios().stream()
            .filter(s -> s.code() == ScenarioCode.CONSERVATIVE)
            .findFirst()
            .orElseThrow();
    results.add(
        calculate(
            p,
            new Scenario(
                ScenarioCode.CONSERVATIVE,
                conservative.customers(),
                ai.maximumAttempts(),
                conservative.cacBrl()),
            "INTENSIVE",
            "Uso intenso (limite contratado)"));
    for (var r : results) for (var issue : r.blockers()) blockers.add(r.label() + ": " + issue);
    return new PlanEvaluation(
        blockers.isEmpty() ? "PROJECTED_VIABLE" : "REVIEW_REQUIRED",
        blockers.isEmpty() ? "Viável em projeção · requer parecer e gates" : "Revisar viabilidade",
        List.copyOf(blockers),
        List.copyOf(results));
  }

  /** Lista campos essenciais sem converter ausência em uma hipótese numérica silenciosa. */
  private static List<String> missing(PlanAssumptions p) {
    List<String> issues = new ArrayList<>();
    need(issues, p.periodDays(), "período contratado");
    need(issues, p.priceBrl(), "preço por cliente/pacote");
    need(issues, p.minimumMarginPercent(), "margem mínima proposta");
    need(issues, p.maximumCacBrl(), "CAC máximo proposto");
    var ai = p.ai();
    need(issues, ai.providerModel(), "provedor/modelo ou declaração sem IA");
    need(issues, ai.perAttempt(), "custo de cada tentativa de IA");
    need(issues, ai.pricingSource(), "fonte da tarifa ou justificativa de custo zero");
    need(issues, ai.pricingCheckedOn(), "data da conferência da tarifa");
    need(issues, ai.includedUnits(), "resultados úteis incluídos");
    need(issues, ai.maximumAttempts(), "limite de tentativas");
    need(issues, ai.maximumCostPerCustomerBrl(), "teto de IA por cliente");
    if (ai.currency() == PlanAssumptions.Currency.USD) {
      need(issues, ai.usdBrl(), "câmbio USD/BRL");
      need(issues, ai.exchangeSource(), "fonte e data do câmbio");
    }
    var c = p.costs();
    need(issues, c.feePercent(), "taxa percentual");
    need(issues, c.taxPercent(), "tributos");
    need(issues, c.commissionPercent(), "comissão");
    need(issues, c.refundPercent(), "provisão de reembolsos");
    need(issues, c.fixedFeeBrl(), "taxa fixa por pedido");
    need(issues, c.supportBrl(), "suporte por cliente");
    need(issues, c.storageBrl(), "armazenamento por cliente");
    need(issues, c.deliveryBrl(), "entrega por cliente");
    need(issues, c.otherVariableBrl(), "outros custos variáveis");
    need(issues, c.initialAiBrl(), "investimento inicial em IA");
    need(issues, c.initialOtherBrl(), "demais investimentos iniciais");
    need(issues, c.fixedPerPeriodBrl(), "custos fixos do período");
    if (p.scenarios().stream().map(Scenario::code).distinct().count() != 3)
      issues.add("Plutus: informe exatamente um cenário conservador, base e otimista.");
    for (var s : p.scenarios()) {
      need(issues, s.customers(), label(s.code().name()) + " · clientes projetados");
      need(issues, s.attemptsPerCustomer(), label(s.code().name()) + " · tentativas por cliente");
      need(issues, s.cacBrl(), label(s.code().name()) + " · CAC");
    }
    return issues;
  }

  /** Registra a lacuna e seu responsável de planejamento sem assumir custo zero. */
  private static void need(List<String> issues, Object value, String field) {
    if (value == null || value instanceof String text && text.isBlank())
      issues.add("Plutus / responsável pelo produto: informar " + field + ".");
  }

  /** Converte somente a tarifa de IA com câmbio explicitamente informado. */
  private static BigDecimal attemptBrl(AiCost ai) {
    return ai.currency() == PlanAssumptions.Currency.USD
        ? ai.perAttempt().multiply(ai.usdBrl())
        : ai.perAttempt();
  }

  /** Calcula margem antes de arredondar e deduz cada componente financeiro uma única vez. */
  private static ScenarioResult calculate(
      PlanAssumptions p, Scenario s, String code, String label) {
    var c = p.costs();
    List<String> issues = new ArrayList<>();
    var percent =
        c.feePercent().add(c.taxPercent()).add(c.commissionPercent()).add(c.refundPercent());
    var net =
        p.priceBrl()
            .subtract(p.priceBrl().multiply(percent).divide(HUNDRED))
            .subtract(c.fixedFeeBrl());
    var ai = attemptBrl(p.ai()).multiply(BigDecimal.valueOf(s.attemptsPerCustomer()));
    var delivery =
        ai.add(c.supportBrl()).add(c.storageBrl()).add(c.deliveryBrl()).add(c.otherVariableBrl());
    var before = net.subtract(delivery);
    var after = before.subtract(s.cacBrl());
    var initial = c.initialAiBrl().add(c.initialOtherBrl());
    var count = BigDecimal.valueOf(s.customers());
    var operating = after.multiply(count).subtract(c.fixedPerPeriodBrl());
    var margin = ratio(after, net);
    var affordable =
        net.signum() > 0
            ? before
                .subtract(net.multiply(p.minimumMarginPercent()).divide(HUNDRED))
                .max(BigDecimal.ZERO)
            : null;
    if (net.signum() <= 0) issues.add("Plutus: receita líquida por cliente não positiva.");
    if (after.signum() <= 0) issues.add("Plutus: contribuição após aquisição não positiva.");
    if (net.signum() > 0
        && after.multiply(HUNDRED).compareTo(net.multiply(p.minimumMarginPercent())) < 0)
      issues.add("Plutus: margem abaixo da política proposta.");
    if (s.cacBrl().compareTo(p.maximumCacBrl()) > 0)
      issues.add("Hermes / Plutus: CAC excede o teto proposto.");
    if (s.attemptsPerCustomer() > p.ai().maximumAttempts())
      issues.add("Dédalo: tentativas excedem o limite contratado.");
    if (p.ai().perAttempt().signum() > 0 && s.attemptsPerCustomer() < p.ai().includedUnits())
      issues.add("Dédalo: tentativas insuficientes para entregar os resultados incluídos.");
    if (p.ai().perAttempt().signum() == 0 && s.attemptsPerCustomer() != 0)
      issues.add("Dédalo: entrega sem IA não pode pressupor chamadas pagas.");
    if (ai.compareTo(p.ai().maximumCostPerCustomerBrl()) > 0)
      issues.add("Plutus: IA excede o teto por cliente.");
    if (operating.signum() <= 0)
      issues.add("Atena / Plutus: volume projetado não gera resultado operacional positivo.");
    return new ScenarioResult(
        code,
        label,
        s.customers(),
        s.attemptsPerCustomer(),
        money(p.priceBrl().multiply(count)),
        money(net.multiply(count)),
        money(ai),
        ai.divide(BigDecimal.valueOf(p.ai().includedUnits()), 6, RoundingMode.HALF_UP),
        money(delivery),
        money(before),
        money(after),
        margin,
        money(affordable),
        money(operating),
        money(operating.subtract(initial)),
        after.signum() > 0
            ? c.fixedPerPeriodBrl().add(initial).divide(after, 0, RoundingMode.CEILING)
            : null,
        ratio(ai.multiply(count), net.multiply(count)),
        issues.isEmpty(),
        List.copyOf(issues));
  }

  /** Formata precisão de apresentação sem alterar a decisão calculada. */
  private static BigDecimal money(BigDecimal value) {
    return value == null ? null : value.setScale(6, RoundingMode.HALF_UP);
  }

  /** Mantém índices indisponíveis quando o denominador não é positivo. */
  private static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
    return denominator.signum() <= 0
        ? null
        : numerator.multiply(HUNDRED).divide(denominator, 6, RoundingMode.HALF_UP);
  }

  /** Expõe nomes de cenário definidos pelo backend. */
  private static String label(String code) {
    return switch (code) {
      case "CONSERVATIVE" -> "Conservador";
      case "BASE" -> "Base";
      case "OPTIMISTIC" -> "Otimista";
      default -> "Uso intenso";
    };
  }
}
