package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.FinancialAdvice;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.FinancialAdvice.Figure;
import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: resumir pareceres existentes para decisão sem criar inferência ou aprovação.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleFinancialAdvice {
  private final AgentTaskRepository tasks;
  private final LearningCycleJson json;

  /**
   * Prioriza a revisão comercial vinculada e usa o planejamento exato como referência explícita.
   */
  public FinancialAdvice read(LearningSalesCycle cycle, PlanView plan, String planUrl) {
    if (plan != null) return fromPlan(plan, planUrl);
    String reference = "experiment:" + cycle.getExperimentId();
    var latest = tasks.findFinancialAdviceSnapshots(reference, PageRequest.of(0, 1));
    if (latest.isEmpty()) return absent("Ainda não há parecer de Plutus neste ciclo.", planUrl);
    var task = latest.getFirst();
    String taskUrl =
        "/products/"
            + cycle.getProductId()
            + "/value-chain-history/processes/"
            + task.processId()
            + "/activities?chainId="
            + cycle.getChainDefinitionId()
            + "&learningCycleId="
            + cycle.getId()
            + "&sourceReference=experiment%3A"
            + cycle.getExperimentId()
            + "#activity-economics";
    if (!List.of("COMPLETED", "BLOCKED").contains(task.status()) || task.resultJson() == null) {
      boolean pending = List.of("PENDING", "IN_PROGRESS").contains(task.status());
      return new FinancialAdvice(
          pending ? "PENDING" : "UNAVAILABLE",
          pending ? "Parecer ainda não concluído" : "Avaliação interrompida",
          pending
              ? "Aguarde a avaliação atual antes de decidir novos gastos."
              : "Confira a falha da última avaliação antes de solicitar outra tentativa.",
          "PLANNING",
          task.id(),
          null,
          taskUrl,
          List.of(),
          "Não reutilizamos uma aprovação antiga no lugar da tarefa atual.",
          null);
    }
    JsonNode result;
    try {
      result = json.read(task.resultJson());
    } catch (RuntimeException ex) {
      log.error(
          "Módulo financeiro: parecer ilegível cycleId={} taskId={}", cycle.getId(), task.id(), ex);
      return absent("O parecer precisa ser recuperado antes de orientar a decisão.", taskUrl);
    }
    if (result == null || !"AGENT_VALIDATION_HYPOTHESIS".equals(result.path("mode").asText()))
      return absent("O parecer anterior não possui uma base compatível para este resumo.", taskUrl);
    var e = result.path("economics");
    BigDecimal price = number(e, "offerPriceBrl"),
        cost = number(e, "variableCostPerSaleBrl"),
        surplus = number(e, "contributionPerSaleBrl");
    if (price == null
        || price.signum() <= 0
        || cost == null
        || cost.signum() < 0
        || surplus == null
        || price.subtract(cost).subtract(surplus).abs().compareTo(new BigDecimal("0.01")) > 0)
      return absent("Preço, custos e sobra do parecer precisam ser conferidos.", taskUrl);
    String decision = result.path("decision").asText();
    if (!List.of("APPROVE", "ADJUST", "REJECT").contains(decision))
      return absent("A decisão de Plutus não está disponível neste parecer.", taskUrl);
    if ("APPROVE".equals(decision) && !"COMPLETED".equals(task.status()))
      return new FinancialAdvice(
          "UNAVAILABLE",
          "Parecer ainda não aceito",
          "O parecer foi produzido, mas seu aceite não foi concluído. Confira o bloqueio antes de avançar.",
          "PLANNING",
          task.id(),
          task.deliveredAt(),
          taskUrl,
          List.of(),
          "Não é necessário gerar outro parecer apenas para recuperar o resultado existente.",
          result.path("rationale").asText(null));
    boolean approved = "APPROVE".equals(decision) && "COMPLETED".equals(task.status());
    boolean stale =
        (approved && !Boolean.TRUE.equals(task.accepted()))
            || task.createdAt() == null
            || (cycle.getVersionChangedAt() != null
                && task.createdAt().isBefore(cycle.getVersionChangedAt()))
            || tasks
                .existsBySourceReferenceAndProcessDefinitionIdAndProcessActivityIdAndCreatedAtAfter(
                    reference, task.processId(), "marketStrategy", task.createdAt());
    boolean rejected = "REJECT".equals(decision);
    var figures =
        List.of(
            new Figure("Preço por venda", price, "BRL"),
            new Figure("Custos por venda, sem anúncios", cost, "BRL"),
            new Figure("Sobra por venda, antes dos anúncios", surplus, "BRL"));
    return new FinancialAdvice(
        stale ? "STALE" : approved ? "PREPARATION_ONLY" : rejected ? "REJECT" : "ADJUST",
        stale
            ? "Reavaliar antes de investir mais"
            : approved
                ? "Preparação interna aprovada por Plutus"
                : rejected ? "Hipótese reprovada por Plutus" : "Ajustar antes de avançar",
        stale
            ? "Este parecer não está válido para a estratégia e a versão atuais. Atualize a avaliação antes de decidir novos gastos."
            : approved
                ? "Plutus recomenda seguir somente com a preparação interna. A venda e os anúncios ainda precisam de avaliação comercial."
                : rejected
                    ? "Plutus não recomenda avançar com esta hipótese. Confira os motivos antes de investir mais."
                    : "Plutus pediu ajustes na economia. Confira as condições antes de assumir novos gastos.",
        "PLANNING",
        task.id(),
        task.deliveredAt(),
        taskUrl,
        figures,
        "Hipótese de planejamento: a sobra ainda não desconta anúncios nem custos fixos; não é lucro líquido. O teto técnico proposto não é autorização de gasto.",
        result.path("rationale").asText(null));
  }

  /**
   * Expõe números do cálculo canônico e a decisão real da mesma revisão, sem recalcular cenários.
   */
  private FinancialAdvice fromPlan(PlanView plan, String url) {
    var analysis = plan.analysis();
    var result = analysis == null ? null : analysis.result();
    String decision = result == null ? "" : result.path("decision").asText();
    boolean complete =
        analysis != null
            && "COMPLETED".equals(analysis.status())
            && List.of("APPROVE", "ADJUST", "BLOCKED").contains(decision);
    boolean pending = analysis != null && List.of("PENDING", "RUNNING").contains(analysis.status());
    String status =
        plan.stale() ? "STALE" : complete ? decision : pending ? "PENDING" : "UNAVAILABLE";
    String title =
        switch (status) {
          case "APPROVE" -> "Parecer favorável de Plutus";
          case "STALE" -> "Atualizar o parecer antes de investir";
          case "ADJUST" -> "Ajustar antes de avançar";
          case "BLOCKED" -> "Faltam dados para decidir";
          case "UNAVAILABLE" ->
              analysis == null ? "Parecer ainda não solicitado" : "Avaliação precisa de atenção";
          default -> "Parecer ainda não concluído";
        };
    var figures = new ArrayList<Figure>();
    var assumptions = plan.assumptions();
    if (assumptions != null)
      figures.add(new Figure("Preço por venda", assumptions.priceBrl(), "BRL"));
    var base =
        plan.evaluation() == null
            ? null
            : plan.evaluation().scenarios().stream()
                .filter(s -> "BASE".equals(s.code()))
                .findFirst()
                .orElse(null);
    if (base != null) {
      figures.add(
          new Figure("Sobra por venda, após anúncios", base.contributionAfterCacBrl(), "BRL"));
      figures.add(
          new Figure("Máximo para conquistar uma compra", base.maximumAffordableCacBrl(), "BRL"));
      figures.add(
          new Figure(
              "Vendas para cobrir investimento e custos fixos do período",
              base.breakEvenCustomers(),
              "COUNT"));
    }
    String recommendation =
        plan.stale()
            ? "Atualize as premissas: estes números pertencem à revisão anterior."
            : analysis == null
                ? "Confira os cenários e solicite o parecer no plano financeiro, respeitando o saldo autorizado."
                : !complete && !pending
                    ? "Confira a última avaliação no plano financeiro; não há parecer concluído disponível."
                    : !complete
                        ? "Os números são cenários calculados; aguarde o parecer de Plutus antes de decidir."
                        : concise(
                            result
                                .path("executiveSummary")
                                .asText(result.path("decisionBasis").asText(title)));
    return new FinancialAdvice(
        status,
        title,
        recommendation,
        "FINANCIAL_PLAN",
        analysis == null ? null : analysis.executionId(),
        analysis == null ? null : analysis.finishedAt(),
        url,
        List.copyOf(figures),
        "Cenário-base projetado, não previsão de vendas. Contribuição é sobra após custos variáveis e anúncios; não é lucro líquido. O parecer não autoriza gasto.",
        result == null ? null : result.path("executiveSummary").asText(null));
  }

  /** Mantém ausência explícita, sem inventar parecer ou retornar custo zero. */
  private FinancialAdvice absent(String reason, String url) {
    return new FinancialAdvice(
        "UNAVAILABLE",
        "Sem parecer disponível",
        reason,
        "NONE",
        null,
        null,
        url,
        List.of(),
        "A consulta não inicia análise paga nem altera limites.",
        null);
  }

  /** Aceita apenas valores numéricos reais do contrato persistido. */
  private BigDecimal number(JsonNode source, String field) {
    var value = source.path(field);
    return value.isNumber() ? value.decimalValue() : null;
  }

  /** Limita a prévia legada; o texto integral permanece disponível nos detalhes. */
  private String concise(String text) {
    if (text.length() <= 600) return text;
    int end = text.lastIndexOf(' ', 600);
    return text.substring(0, end > 0 ? end : 600) + "…";
  }
}
