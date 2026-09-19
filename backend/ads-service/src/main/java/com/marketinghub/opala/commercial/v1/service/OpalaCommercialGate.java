package com.marketinghub.opala.commercial.v1.service;

import static com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext.require;

import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleCommercialReadiness;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: comprovar a preparação integral sem ativar campanha nem concluir vendas. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpalaCommercialGate implements BackendProductProcessActivityExecutor {
  private final OpalaCommercialContext context;
  private final LearningCycleCommercialReadiness readiness;
  private final AgentTaskRepository tasks;
  private final BusinessProcessActivityInstanceRepository instances;

  /** Reconhece exclusivamente a consolidação final do subprocesso Opala. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && OpalaCommercialContext.CODE.equals(process.getProcessCode())
        && "ready".equals(activity.getActivityId());
  }

  /** Revalida insumos reais e todas as entregas da mesma ocorrência antes de concluir. */
  @Override
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    try {
      var scope = context.scope(source);
      require(
          Objects.equals(product.getId(), scope.cycle().getProductId()),
          "Produto diferente do ciclo.");
      var preparation = readiness.inspect(scope.cycle());
      require(
          preparation != null && preparation.readyForReview(),
          preparation == null ? "Canal não suportado." : preparation.guidance());
      require(
          scope.cycle().getWindowEnd() != null
              && scope.cycle().getWindowEnd().isAfter(Instant.now()),
          "A janela comercial expirou; solicite nova decisão de orçamento.");
      Map<String, com.marketinghub.agenttask.AgentTaskProcessExecutionListSnapshot> latestByStep =
          new LinkedHashMap<>();
      tasks
          .findProcessExecutionListSnapshots(source, process.getProcessCode())
          .forEach(task -> latestByStep.putIfAbsent(task.processActivityId(), task));
      Map<Long, com.marketinghub.agenttask.AgentTaskProcessExecutionEvidenceSnapshot>
          evidenceByTaskId = new LinkedHashMap<>();
      for (String step :
          List.of(
              "entry",
              "creative",
              "checkout",
              "targeting",
              "economics",
              "humanExperienceReview",
              "commercialIntegrityReview")) {
        var last =
            java.util.Optional.ofNullable(latestByStep.get(step))
                .orElseThrow(() -> new IllegalStateException("Falta executar " + step + "."));
        require(
            "COMPLETED".equals(last.status()),
            "A atividade " + step + " não comprovou a versão atual.");
        evidenceByTaskId.put(last.taskId(), null);
      }
      tasks
          .findProcessExecutionEvidenceSnapshots(evidenceByTaskId.keySet())
          .forEach(task -> evidenceByTaskId.put(task.taskId(), task));
      for (String step :
          List.of(
              "entry",
              "creative",
              "checkout",
              "targeting",
              "economics",
              "humanExperienceReview",
              "commercialIntegrityReview")) {
        var last = latestByStep.get(step);
        var evidence = evidenceByTaskId.get(last.taskId());
        require(
            evidence != null
                && scope
                    .cycle()
                    .getProductVersion()
                    .equals(
                        context
                            .read(evidence.evidenceJson())
                            .path("opalaScope")
                            .path("productVersion")
                            .asText()),
            "A atividade " + step + " não comprovou a versão atual.");
        if ("economics".equals(step)) {
          var economics = context.read(evidence.resultJson()).path("economics");
          require(
              !java.time.LocalDate.parse(economics.path("deadline").asText())
                      .isBefore(java.time.LocalDate.now(java.time.ZoneOffset.UTC))
                  && economics
                          .path("offerPriceBrl")
                          .decimalValue()
                          .compareTo(scope.experiment().getUnitPrice())
                      == 0,
              "O parecer financeiro expirou ou o preço mudou; revalide com Plutus.");
        }
        if (java.util.Set.of("humanExperienceReview", "commercialIntegrityReview").contains(step))
          require(
              context
                  .read(evidence.evidenceJson())
                  .path("opalaScope")
                  .equals(context.snapshot(source)),
              "Os ativos mudaram após a revisão; renove os pareceres afetados.");
      }
      return new BackendProductProcessActivityReadiness(
          true, "Preparação comprovada. A ativação permanece sujeita à autorização final.");
    } catch (RuntimeException ex) {
      log.warn(
          "Gate Opala bloqueado. productId={} source={}",
          product == null ? null : product.getId(),
          source,
          ex);
      return new BackendProductProcessActivityReadiness(false, ex.getMessage());
    }
  }

  /** Grava uma conclusão idempotente e auditável, sem alterar ciclo, orçamento ou publicação. */
  @Override
  @Transactional
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    var check = readiness(process, activity, product, source);
    require(check.ready(), check.reason());
    var previous =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), source);
    var proof = context.snapshot(source).deepCopy();
    proof.put("evidenceType", "OPALA_COMMERCIAL_READY_V1");
    proof.put("salesProven", false);
    if (previous
        .filter(
            i ->
                i.isObjectiveAchieved()
                    && "COMPLETED".equals(i.getStatus())
                    && proof.equals(context.read(i.getObjectiveEvidenceJson())))
        .isPresent())
      return new BackendProductProcessActivityExecutionResult(
          source, "COMPLETED", true, check.reason());
    var instance = new BusinessProcessActivityInstance();
    Instant now = Instant.now();
    instance.setActivityDefinition(activity);
    instance.setSourceReference(source);
    instance.setOccurrenceNumber(previous.map(i -> i.getOccurrenceNumber() + 1).orElse(1));
    instance.setStatus("COMPLETED");
    instance.setObjectiveAchieved(true);
    instance.setObjectiveEvidenceJson(proof.toString());
    instance.setEnteredAt(now);
    instance.setExitedAt(now);
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    instance.setKnownCostUsd(BigDecimal.ZERO);
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    instances.saveAndFlush(instance);
    return new BackendProductProcessActivityExecutionResult(
        source, "COMPLETED", true, check.reason());
  }
}
