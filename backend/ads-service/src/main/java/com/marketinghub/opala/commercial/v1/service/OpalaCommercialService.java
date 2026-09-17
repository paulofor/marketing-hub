package com.marketinghub.opala.commercial.v1.service;

import static com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext.require;

import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.*;
import com.marketinghub.businessprocess.execution.service.backendactivity.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleCommercialReadiness;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: governar resultados, bloqueios e conclusão da preparação comercial Opala. */
@Service
@Slf4j
public class OpalaCommercialService
    implements AgentTaskCompletionHook, AgentProductProcessActivityReadinessProvider {
  private static final Set<String> PREPARATION =
      Set.of("entry", "creative", "checkout", "targeting");
  private static final List<String> REVIEWS =
      List.of("economics", "humanExperienceReview", "commercialIntegrityReview");
  private final OpalaCommercialContext context;
  private final OpalaCommercialMaterialization materialization;
  private final LearningCycleCommercialReadiness commercialReadiness;
  private final AgentTaskRepository tasks;
  private final OpalaCommercialRouting routing;

  /**
   * Resolve integrações sob demanda para preservar a composição dos validadores e callbacks BPM.
   */
  public OpalaCommercialService(
      OpalaCommercialContext context,
      @Lazy OpalaCommercialMaterialization materialization,
      @Lazy LearningCycleCommercialReadiness commercialReadiness,
      AgentTaskRepository tasks,
      OpalaCommercialRouting routing) {
    this.context = context;
    this.materialization = materialization;
    this.commercialReadiness = commercialReadiness;
    this.tasks = tasks;
    this.routing = routing;
  }

  /** Reconhece apenas tarefas pertencentes ao subprocesso versionado. */
  @Override
  public boolean supports(AgentTask task) {
    return task != null
        && task.getProcessDefinition() != null
        && OpalaCommercialContext.CODE.equals(task.getProcessDefinition().getProcessCode());
  }

  /** Aplica o contrato às atividades de agentes e ao gate final deste subprocesso. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && OpalaCommercialContext.CODE.equals(process.getProcessCode());
  }

  /** Verifica identidade e insumos antes de iniciar revisões que consumam modelo. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    try {
      var scope = context.scope(source);
      require(
          product != null && Objects.equals(product.getId(), scope.cycle().getProductId()),
          "O subprocesso pertence a outro produto.");
      if ("economics".equals(activity.getActivityId())) {
        var financialPlan = context.snapshot(source).path("financialPlan");
        require(
            "READY".equals(financialPlan.path("status").asText()),
            financialPlan
                .path("reason")
                .asText("Plano financeiro da versão ainda não está pronto."));
        require(
            scope.cycle().getWindowEnd() != null
                && scope.cycle().getWindowEnd().isAfter(Instant.now()),
            "A janela comercial venceu; revalide o período antes de solicitar novo parecer de Plutus.");
      }
      if (Set.of("humanExperienceReview", "commercialIntegrityReview")
          .contains(activity.getActivityId())) {
        var preparation = commercialReadiness.inspect(scope.cycle());
        require(
            preparation != null && preparation.readyForReview(),
            preparation == null
                ? "Canal ainda não suportado nesta homologação."
                : preparation.guidance());
      }
      return new AgentProductProcessActivityReadiness(
          true,
          "Executar somente a preparação desta versão; publicação e gasto exigem seus controles próprios.");
    } catch (RuntimeException ex) {
      log.warn(
          "Preparação Opala bloqueada. productId={} source={} activity={}",
          product == null ? null : product.getId(),
          source,
          activity.getActivityId(),
          ex);
      return new AgentProductProcessActivityReadiness(false, ex.getMessage());
    }
  }

  /** Aplica instruções ou registra parecer, recusando callbacks de outra versão ou identidade. */
  @Override
  @Transactional
  public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
    var scope = context.scope(task.getSourceReference());
    var evidence = context.read(request.evidenceJson());
    var identity = evidence.path("opalaScope");
    require(
        identity.path("cycleId").asLong() == scope.cycle().getId()
            && identity.path("productId").asLong() == scope.cycle().getProductId()
            && identity.path("experimentId").asLong() == scope.cycle().getExperimentId()
            && scope.cycle().getProductVersion().equals(identity.path("productVersion").asText()),
        "Resposta pertence a outra ocorrência ou versão do Opala.");
    require(
        task.getCreatedAt() != null
            && (scope.cycle().getVersionChangedAt() == null
                || !task.getCreatedAt().isBefore(scope.cycle().getVersionChangedAt())),
        "A tarefa foi criada antes da versão vigente.");
    var result = context.read(request.resultJson());
    String activity = task.getProcessActivityId();
    if (PREPARATION.contains(activity)) {
      require(
          "READY".equals(result.path("decision").asText()),
          "Preparação ainda não concluída pelo agente.");
      materialization.apply(activity, scope, result.path("instruction"));
    } else {
      require(
          REVIEWS.contains(activity)
              && ("economics".equals(activity) ? "APPROVE" : "APPROVED")
                  .equals(result.path("decision").asText()),
          "Parecer não aprovou esta preparação.");
      if ("economics".equals(activity)) {
        var economics = result.path("economics");
        var financialPlan = context.snapshot(task.getSourceReference()).path("financialPlan");
        require(
            "READY".equals(financialPlan.path("status").asText()),
            "O plano financeiro mudou ou deixou de estar vigente durante a avaliação.");
        var assumptions = financialPlan.path("assumptions");
        var baseScenario = baseScenario(financialPlan);
        require(
            economics.path("offerPriceBrl").isNumber()
                && economics.path("variableCostPerSaleBrl").isNumber()
                && economics.path("contributionPerSaleBrl").isNumber()
                && scope.experiment().getUnitPrice() != null,
            "Plutus precisa informar preço, custo e contribuição reais.");
        var price = economics.path("offerPriceBrl").decimalValue();
        var cost = economics.path("variableCostPerSaleBrl").decimalValue();
        var contribution = economics.path("contributionPerSaleBrl").decimalValue();
        require(
            price.compareTo(scope.experiment().getUnitPrice()) == 0
                && cost.signum() >= 0
                && contribution.signum() > 0
                && price
                        .subtract(cost)
                        .subtract(contribution)
                        .abs()
                        .compareTo(new BigDecimal("0.01"))
                    <= 0,
            "A economia aprovada não reconcilia com preço, custo e margem do experimento.");
        var projectedContribution = baseScenario.path("contributionBeforeCacBrl").decimalValue();
        var projectedCost = price.subtract(projectedContribution);
        var projectedMargin =
            projectedContribution
                .multiply(new BigDecimal("100"))
                .divide(price, 6, RoundingMode.HALF_UP);
        require(
            close(cost, projectedCost)
                && close(contribution, projectedContribution)
                && close(
                    economics.path("contributionMarginPercent").decimalValue(), projectedMargin)
                && close(
                    economics.path("maxCacBrl").decimalValue(),
                    assumptions.path("maximumCacBrl").decimalValue())
                && close(
                    economics.path("expectedRefundPercent").decimalValue(),
                    assumptions.path("costs").path("refundPercent").decimalValue()),
            "O parecer de Plutus diverge da projeção financeira versionada.");
        require(
            economics.path("maxBudgetBrl").isNumber()
                && economics.path("maxBudgetBrl").decimalValue().signum() > 0
                && scope.cycle().getBudgetLimitBrl() != null
                && economics
                        .path("maxBudgetBrl")
                        .decimalValue()
                        .compareTo(scope.cycle().getBudgetLimitBrl())
                    <= 0,
            "O parecer precisa respeitar o teto de mídia do ciclo.");
        require(
            result.path("scenarios").size() == 3
                && scope.cycle().getBudgetLimitBrl() != null
                && scope.cycle().getBudgetLimitBrl().signum() > 0,
            "Informe três cenários e um teto de mídia válido.");
        require(
            validDeadline(scope, financialPlan, economics.path("deadline").asText()),
            "O parecer financeiro expirou.");
      } else {
        require(
            identity.equals(context.snapshot(task.getSourceReference())),
            "Os ativos avaliados mudaram; refaça a revisão da mesma ocorrência.");
        var preparation = commercialReadiness.inspect(scope.cycle());
        require(
            preparation != null && preparation.readyForReview(),
            "As condições comerciais mudaram durante a revisão; revalide os insumos.");
        require(
            result.path("gateChecks").isArray()
                && result.path("gateChecks").size()
                    >= ("humanExperienceReview".equals(activity) ? 8 : 10)
                && result.path("evidence").isArray()
                && !result.path("evidence").isEmpty()
                && result.path("requiredChanges").isArray()
                && result.path("requiredChanges").isEmpty(),
            "O parecer não comprovou todos os critérios comerciais.");
        for (var check : result.path("gateChecks"))
          require(
              "PASS".equals(check.path("status").asText()),
              "O parecer aprovou com critério pendente.");
      }
    }
    return CompletionDisposition.COMPLETE;
  }

  /** Invalida a conclusão quando o ciclo muda de versão, preservando a tentativa histórica. */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    OpalaCommercialContext.Scope scope;
    try {
      scope = context.scope(source);
    } catch (RuntimeException ex) {
      log.debug("Consulta histórica Opala sem nova execução. source={}", source, ex);
      return false;
    }
    if ("ready".equals(activity.getActivityId())) return !routing.completed(scope.cycle());
    return tasks
        .findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            process.getId(), source)
        .stream()
        .filter(
            t ->
                source.equals(t.getSourceReference())
                    && activity.getActivityId().equals(t.getProcessActivityId())
                    && "COMPLETED".equals(t.getStatus()))
        .reduce((a, b) -> b)
        .map(
            t -> {
              var snapshot = context.read(t.getEvidenceJson()).path("opalaScope");
              if (!scope
                  .cycle()
                  .getProductVersion()
                  .equals(snapshot.path("productVersion").asText())) return true;
              String step = activity.getActivityId();
              if (PREPARATION.contains(step))
                return !materialization.current(
                    step, scope, context.read(t.getResultJson()).path("instruction"));
              var current = context.snapshot(source);
              if ("economics".equals(step))
                return !sameDecimal(snapshot.path("priceBrl"), current.path("priceBrl"))
                    || !sameDecimal(snapshot.path("budgetLimitBrl"), current.path("budgetLimitBrl"))
                    || !snapshot.path("windowEnd").equals(current.path("windowEnd"))
                    || !sameFinancialPlanRevision(
                        snapshot.path("financialPlan"), current.path("financialPlan"))
                    || !snapshot.path("productContract").equals(current.path("productContract"))
                    || java.time.LocalDate.parse(
                            context
                                .read(t.getResultJson())
                                .path("economics")
                                .path("deadline")
                                .asText())
                        .isBefore(java.time.LocalDate.now(java.time.ZoneOffset.UTC));
              return !snapshot.equals(current);
            })
        .orElse(false);
  }

  /**
   * Compara a identidade imutável do plano sem invalidar números JSON equivalentes após leitura.
   */
  private static boolean sameFinancialPlanRevision(
      com.fasterxml.jackson.databind.JsonNode previous,
      com.fasterxml.jackson.databind.JsonNode current) {
    return previous.path("id").isIntegralNumber()
        && current.path("id").isIntegralNumber()
        && previous.path("revision").isIntegralNumber()
        && current.path("revision").isIntegralNumber()
        && previous.path("id").longValue() == current.path("id").longValue()
        && previous.path("revision").intValue() == current.path("revision").intValue();
  }

  /** Compara valores financeiros pelo valor decimal, sem depender do tipo numérico do JSON. */
  private static boolean sameDecimal(
      com.fasterxml.jackson.databind.JsonNode previous,
      com.fasterxml.jackson.databind.JsonNode current) {
    return previous.isNumber()
        && current.isNumber()
        && previous.decimalValue().compareTo(current.decimalValue()) == 0;
  }

  /**
   * Localiza o cenário-base calculado pelo backend, sem aceitar números reconstruídos pelo modelo.
   */
  private static com.fasterxml.jackson.databind.JsonNode baseScenario(
      com.fasterxml.jackson.databind.JsonNode financialPlan) {
    for (var scenario : financialPlan.path("deterministicEvaluation").path("scenarios"))
      if ("BASE".equals(scenario.path("code").asText())) return scenario;
    throw new IllegalStateException("Plano financeiro sem cenário-base determinístico.");
  }

  /** Compara valores financeiros na precisão comercial de um centavo. */
  private static boolean close(BigDecimal left, BigDecimal right) {
    return left != null
        && right != null
        && left.subtract(right).abs().compareTo(new BigDecimal("0.01")) <= 0;
  }

  /** Limita o parecer à menor validade entre plano financeiro e janela comercial. */
  private static boolean validDeadline(
      OpalaCommercialContext.Scope scope,
      com.fasterxml.jackson.databind.JsonNode financialPlan,
      String rawDeadline) {
    LocalDate deadline = LocalDate.parse(rawDeadline);
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate planLimit =
        LocalDate.parse(financialPlan.path("assumptions").path("validUntil").asText());
    LocalDate windowLimit = scope.cycle().getWindowEnd().atZone(ZoneOffset.UTC).toLocalDate();
    return !deadline.isBefore(today)
        && !deadline.isAfter(planLimit)
        && !deadline.isAfter(windowLimit);
  }
}
