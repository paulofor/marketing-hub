package com.marketinghub.financialplan.v1.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.financialplan.v1.service.getplan.PlanEvaluation;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: proteger economia unitária, fontes ausentes e limite contratado com casos
 * sintéticos.
 */
class FinancialPlanCalculatorTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /** Carrega premissas sintéticas independentes de produtos e tarifas reais. */
  private ObjectNode input() throws IOException {
    return (ObjectNode)
        json.readTree(getClass().getResourceAsStream("/financial-plan/assumptions.json"));
  }

  /** Executa o mesmo cálculo usado na persistência administrativa. */
  private PlanEvaluation evaluate(ObjectNode p) throws IOException {
    return FinancialPlanCalculator.evaluate(json.treeToValue(p, PlanAssumptions.class));
  }

  /** Converte a fixture detalhada em envelope agregado sem declarar componentes como zero. */
  private ObjectNode aggregateInput() throws IOException {
    var p = input();
    var costs = (ObjectNode) p.get("costs");
    for (String field :
        java.util.List.of(
            "feePercent",
            "taxPercent",
            "commissionPercent",
            "refundPercent",
            "fixedFeeBrl",
            "supportBrl",
            "storageBrl",
            "deliveryBrl",
            "otherVariableBrl")) costs.putNull(field);
    ((ObjectNode) p.get("ai")).putNull("perAttempt");
    p.putObject("variableCostEnvelope")
        .put("amountPerCustomerBrl", 13.5)
        .put("coverage", "ALL_VARIABLE_COSTS_EXCLUDING_CAC")
        .put("sourceReference", "commercial-plan:9@v4:variableCostPerSaleBrl")
        .put("checkedOn", "2026-09-20");
    return p;
  }

  /** Confere manualmente BRL, deduções únicas, fixos, investimento e estresse por cliente. */
  @Test
  void calculaPacoteCompletoSemDuplaContagem() throws Exception {
    var result = evaluate(input());
    assertThat(result.status()).isEqualTo("PROJECTED_VIABLE");
    assertThat(result.scenarios()).hasSize(4);
    var s = result.scenarios().getFirst();
    assertThat(s.netRevenueBrl()).isEqualByComparingTo("800");
    assertThat(s.aiCostPerCustomerBrl()).isEqualByComparingTo("6");
    assertThat(s.aiCostPerUsefulUnitBrl()).isEqualByComparingTo("1.5");
    assertThat(s.contributionBeforeCacBrl()).isEqualByComparingTo("70");
    assertThat(s.contributionAfterCacBrl()).isEqualByComparingTo("50");
    assertThat(s.marginPercent()).isEqualByComparingTo("62.5");
    assertThat(s.operatingResultBrl()).isEqualByComparingTo("400");
    assertThat(s.resultAfterInvestmentBrl()).isEqualByComparingTo("300");
    assertThat(s.breakEvenCustomers()).isEqualByComparingTo("4");
    assertThat(s.maximumAffordableCacBrl()).isEqualByComparingTo("54");
    assertThat(result.scenarios().getLast().contributionAfterCacBrl()).isEqualByComparingTo("48");
  }

  /** Impede que custo desconhecido vire zero ou mantenha métricas aparentemente confiáveis. */
  @Test
  void custoAusenteBloqueia() throws Exception {
    var p = input();
    ((ObjectNode) p.get("costs")).putNull("supportBrl");
    var result = evaluate(p);
    assertThat(result.status()).isEqualTo("MISSING_INPUTS");
    assertThat(result.scenarios()).isEmpty();
    assertThat(result.blockers()).anyMatch(s -> s.contains("suporte"));
  }

  /** Libera somente o parecer quando o envelope e os limites determinísticos são suficientes. */
  @Test
  void envelopeAgregadoFicaProntoParaPlutusSemSimularDecomposicao() throws Exception {
    var result = evaluate(aggregateInput());
    assertThat(result.status()).isEqualTo("READY_FOR_ANALYSIS");
    assertThat(result.scenarios()).isEmpty();
    assertThat(result.blockers()).isEmpty();
  }

  /** Bloqueia dupla dedução e contribuição não positiva antes de qualquer chamada paga. */
  @Test
  void envelopeAgregadoRejeitaComponenteDuplicadoEUnidadeInviavel() throws Exception {
    var duplicated = aggregateInput();
    ((ObjectNode) duplicated.get("costs")).put("supportBrl", 2);
    assertThat(evaluate(duplicated).blockers()).anyMatch(v -> v.contains("combine os dois"));

    var unprofitable = aggregateInput();
    unprofitable.put("priceBrl", 30);
    assertThat(evaluate(unprofitable).blockers())
        .anyMatch(v -> v.contains("contribuição após o CAC"));
  }

  /** Recusa conversão cambial sem fonte mesmo quando um número foi informado. */
  @Test
  void cambioExigeFonte() throws Exception {
    var p = input();
    ((ObjectNode) p.get("ai")).putNull("exchangeSource");
    assertThat(evaluate(p).status()).isEqualTo("MISSING_INPUTS");
  }

  /** Aceita tarifa BRL sem exigir uma cotação que não será utilizada. */
  @Test
  void brlNaoExigeCambio() throws Exception {
    var p = input();
    var ai = (ObjectNode) p.get("ai");
    ai.put("currency", "BRL");
    ai.putNull("usdBrl");
    ai.putNull("exchangeSource");
    assertThat(evaluate(p).status()).isEqualTo("PROJECTED_VIABLE");
  }

  /** Mantém razão de IA indisponível sem receita positiva, inclusive com zero clientes. */
  @Test
  void receitaZeroNaoGeraIndiceZero() throws Exception {
    var p = input();
    ((ObjectNode) p.get("scenarios").get(0)).put("customers", 0);
    assertThat(evaluate(p).scenarios().getFirst().aiToNetRevenuePercent()).isNull();
    ((ObjectNode) p.get("costs")).put("feePercent", 100);
    var s = evaluate(p).scenarios().get(1);
    assertThat(s.marginPercent()).isNull();
    assertThat(s.breakEvenCustomers()).isNull();
  }

  /** Expõe inviabilidade no limite mesmo quando as tentativas médias parecem rentáveis. */
  @Test
  void usoIntensoPodeReprovarPlanoMedioBom() throws Exception {
    var p = input();
    ((ObjectNode) p.get("ai")).put("maximumAttempts", 60).put("maximumCostPerCustomerBrl", 60);
    var result = evaluate(p);
    assertThat(result.scenarios().getFirst().viable()).isTrue();
    assertThat(result.scenarios().getLast().viable()).isFalse();
    assertThat(result.status()).isEqualTo("REVIEW_REQUIRED");
  }

  /** Bloqueia teto insuficiente e promessa de mais resultados que tentativas possíveis. */
  @Test
  void limitePrecisaCobrirEntrega() throws Exception {
    var p = input();
    ((ObjectNode) p.get("ai")).put("maximumCostPerCustomerBrl", 3).put("includedUnits", 9);
    assertThat(evaluate(p).blockers()).anyMatch(s -> s.contains("teto por cliente"));
    assertThat(evaluate(p).blockers()).anyMatch(s -> s.contains("resultados contratada"));
  }

  /** Aceita kit sem consumo variável mantendo custo de criação e entrega. */
  @Test
  void kitSemIaNaoZeraOutrosCustos() throws Exception {
    var p = input();
    ((ObjectNode) p.get("ai"))
        .put("perAttempt", 0)
        .put("maximumAttempts", 0)
        .put("maximumCostPerCustomerBrl", 0);
    p.get("scenarios").forEach(s -> ((ObjectNode) s).put("attemptsPerCustomer", 0));
    var r = evaluate(p).scenarios().getFirst();
    assertThat(r.variableDeliveryPerCustomerBrl()).isEqualByComparingTo("4");
    assertThat(r.resultAfterInvestmentBrl()).isEqualByComparingTo("360");
  }

  /** Distingue recuperação do investimento de economia unitária negativa. */
  @Test
  void equilibrioNaoExisteComContribuicaoNegativa() throws Exception {
    var p = input();
    p.put("priceBrl", 10);
    assertThat(evaluate(p).scenarios().getFirst().breakEvenCustomers()).isNull();
  }

  /** Não aceita cenários duplicados para satisfazer artificialmente o número de alternativas. */
  @Test
  void exigeCenariosDistintos() throws Exception {
    var p = input();
    ((ObjectNode) p.get("scenarios").get(1)).put("code", "CONSERVATIVE");
    assertThat(evaluate(p).status()).isEqualTo("MISSING_INPUTS");
  }
}
