package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: registrar e recuperar o aceite cumulativo de preparação no ledger existente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCyclePreparationBudget {
  private static final String ACTION = "AUTHORIZE_PREPARATION_BUDGET";
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleJson json;

  /** Obtém recibos sem herdar decisões de outro ciclo ou versão executável. */
  public List<CycleFinancialReview.Authorization> history(LearningSalesCycle cycle) {
    var receipts = events.findByCycleIdAndActionOrderByRevisionDesc(cycle.getId(), ACTION);
    Long currentId =
        receipts.stream()
            .filter(event -> eligible(event, cycle))
            .map(LearningSalesCycleEvent::getId)
            .findFirst()
            .orElse(null);
    return receipts.stream()
        .map(
            event -> {
              var data = json.read(event.getEvidenceJson());
              return new CycleFinancialReview.Authorization(
                  event.getId(),
                  event.getEvidenceReference(),
                  data.path("totalLimitUsd").decimalValue(),
                  data.path("reserveUsd").decimalValue(),
                  event.getOperatorName(),
                  data.path("justification").asText(),
                  event.getCreatedAt(),
                  data.path("productVersion").asText(),
                  Objects.equals(currentId, event.getId()));
            })
        .toList();
  }

  /** Recupera a autorização vigente da mesma identidade e moeda, sem interpretar notas livres. */
  public CycleFinancialReview.Authorization current(LearningSalesCycle cycle) {
    return history(cycle).stream()
        .filter(CycleFinancialReview.Authorization::current)
        .findFirst()
        .orElse(null);
  }

  /** Confere a identidade congelada e invalida recibos anteriores à mudança de versão. */
  private boolean eligible(LearningSalesCycleEvent event, LearningSalesCycle cycle) {
    var data = json.read(event.getEvidenceJson());
    return (cycle.getVersionChangedAt() == null
            || !event.getCreatedAt().isBefore(cycle.getVersionChangedAt()))
        && Objects.equals(cycle.getProductVersion(), data.path("productVersion").asText())
        && cycle.getExperimentId() == data.path("experimentId").asLong()
        && cycle.getChainDefinitionId() == data.path("chainDefinitionId").asLong()
        && "PREPARATION_CUMULATIVE".equals(data.path("scope").asText())
        && "USD".equals(data.path("currency").asText());
  }

  /** Persiste confirmação idempotente sob lock, sem iniciar tarefas, mídia ou audiovisual. */
  public void authorize(
      LearningSalesCycle cycle,
      PreparationBudgetRequest request,
      BigDecimal observedCost,
      int missing,
      Instant now) {
    require(request != null && request.requestKey() != null, "Informe a identidade da decisão.");
    require(
        Objects.equals(request.chainDefinitionId(), cycle.getChainDefinitionId())
            && Objects.equals(request.experimentId(), cycle.getExperimentId())
            && Objects.equals(request.productVersion(), cycle.getProductVersion()),
        "A decisão pertence a outro contexto. Reabra a demonstração pelo ciclo.");
    String input = json.write(request);
    var replay = events.findByCycleIdAndRequestKey(cycle.getId(), request.requestKey().toString());
    if (replay.isPresent()) {
      require(
          ACTION.equals(replay.get().getAction())
              && json.read(input).equals(json.read(replay.get().getRequestJson())),
          "A chave da decisão já foi usada com outros valores.");
      return;
    }
    require(
        Objects.equals(request.expectedRevision(), cycle.getRevision()),
        "O ciclo mudou. Atualize antes de aprovar.");
    require(
        "OPEN".equals(cycle.getStatus()),
        "Ciclo encerrado: decisões disponíveis apenas para consulta.");
    require(
        Boolean.TRUE.equals(request.confirmed()),
        "Confirme o teto total e o escopo da autorização.");
    require(
        validAmount(request.totalLimitUsd()) && validAmount(request.reserveUsd()),
        "Use valores não negativos em US$, com até duas casas decimais.");
    require(
        request.operatorName() != null
            && !request.operatorName().isBlank()
            && request.operatorName().length() <= 160
            && request.justification() != null
            && !request.justification().isBlank()
            && request.justification().length() <= 4000,
        "Informe responsável e justificativa da decisão.");
    cycle.setRevision(cycle.getRevision() + 1);
    cycle.setUpdatedAt(now);
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRevision(cycle.getRevision());
    event.setRequestKey(request.requestKey().toString());
    event.setRequestJson(input);
    event.setFromStage(cycle.getStage());
    event.setToStage(cycle.getStage());
    event.setAction(ACTION);
    event.setOperatorName(request.operatorName().trim());
    event.setSummary(
        "Teto cumulativo de preparação: US$ "
            + request.totalLimitUsd().toPlainString()
            + "; reserva para custos pendentes: US$ "
            + request.reserveUsd().toPlainString()
            + ". Sem ampliação de mídia ou audiovisual.");
    event.setEvidenceReference(
        "internal://learning-cycles/"
            + cycle.getId()
            + "/preparation-budget/"
            + request.requestKey());
    var proof = (ObjectNode) json.read(input);
    proof.put("scope", "PREPARATION_CUMULATIVE");
    proof.put("currency", "USD");
    proof.put("productId", cycle.getProductId());
    proof.put("knownPreparationUsd", observedCost);
    proof.put("unreportedTasks", missing);
    proof.put("mediaAuthorized", false);
    proof.put("audiovisualAuthorized", false);
    event.setEvidenceJson(json.write(proof));
    event.setCreatedAt(now);
    events.saveAndFlush(event);
    log.info(
        "Preparação: aceite registrado productId={} cycleId={} eventId={} totalLimitUsd={}",
        cycle.getProductId(),
        cycle.getId(),
        event.getId(),
        request.totalLimitUsd());
  }

  /** Mantém a unidade monetária e impede limites negativos ou precisão incompatível. */
  private boolean validAmount(BigDecimal value) {
    return value != null
        && value.signum() >= 0
        && value.scale() <= 2
        && value.compareTo(new BigDecimal("999999.99")) <= 0;
  }
}
