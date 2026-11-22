package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.CostWaiver;
import com.marketinghub.repository.jdbc.learningcycle.CyclePreparationCostRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: registrar dispensa explícita por tarefa no ledger, sem retomar processos. */
@Component
@RequiredArgsConstructor
public class LearningCycleCostWaivers {
  private final CyclePreparationCostRepository costs;
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleJson json;

  /** Expõe pendências e recibos persistidos, inclusive custos recuperados posteriormente. */
  public CostWaiver.View read(Long productId) {
    var tasks = costs.waiverTasks(productId);
    var receipts =
        events.findCostWaivers(productId).stream()
            .map(
                e -> {
                  var proof = json.read(e.getEvidenceJson());
                  var ids = new ArrayList<Long>();
                  proof.path("taskIds").forEach(id -> ids.add(id.longValue()));
                  return new CostWaiver.Receipt(
                      e.getId(),
                      e.getCycleId(),
                      e.getOperatorName(),
                      e.getCreatedAt(),
                      proof.path("reason").asText(),
                      ids);
                })
            .toList();
    return new CostWaiver.View(
        tasks, receipts, tasks.stream().filter(t -> t.costUsd() == null && !t.waived()).count());
  }

  /** Valida seleção sob lock do produto/ciclo e grava recibo idempotente sem tocar em custos. */
  public void record(LearningSalesCycle cycle, CostWaiver.Request request, Instant now) {
    require(request != null && request.requestKey() != null, "Informe a identidade da decisão.");
    require(Objects.equals(request.chainId(), cycle.getChainDefinitionId()), "Cadeia divergente.");
    var input = json.write(request);
    var replay = events.findByCycleIdAndRequestKey(cycle.getId(), request.requestKey().toString());
    if (replay.isPresent()) {
      require(
          "WAIVE_LEGACY_AGENT_COST".equals(replay.get().getAction())
              && json.read(input).equals(json.read(replay.get().getRequestJson())),
          "Chave já usada por outra decisão.");
      return;
    }
    require(
        "OPEN".equals(cycle.getStatus())
            && Objects.equals(request.expectedRevision(), cycle.getRevision()),
        "O ciclo mudou ou foi encerrado. Atualize antes de decidir.");
    require(
        request.confirmed()
            && request.operatorName() != null
            && !request.operatorName().isBlank()
            && request.operatorName().length() <= 160
            && request.reason() != null
            && !request.reason().isBlank()
            && request.reason().length() <= 2000,
        "Confirme a dispensa, informe responsável e motivo.");
    require(
        request.taskIds() != null
            && !request.taskIds().isEmpty()
            && request.taskIds().size() <= 500,
        "Selecione as tarefas legadas.");
    var selected = new HashSet<>(request.taskIds());
    require(selected.size() == request.taskIds().size(), "Não repita tarefas na seleção.");
    var tasks = costs.waiverTasks(cycle.getProductId());
    var eligible =
        tasks.stream()
            .filter(CostWaiver.Task::selectable)
            .map(CostWaiver.Task::taskId)
            .collect(java.util.stream.Collectors.toSet());
    require(
        eligible.containsAll(selected),
        "Há tarefas de outro produto, já dispensadas, em execução ou com custo informado. Atualize a consulta.");
    cycle.setRevision(cycle.getRevision() + 1);
    cycle.setUpdatedAt(now);
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRevision(cycle.getRevision());
    event.setRequestKey(request.requestKey().toString());
    event.setRequestJson(input);
    event.setFromStage(cycle.getStage());
    event.setToStage(cycle.getStage());
    event.setAction("WAIVE_LEGACY_AGENT_COST");
    event.setOperatorName(request.operatorName().trim());
    event.setSummary(
        "Dispensa de conciliação de "
            + selected.size()
            + " tarefas. Custos desconhecidos preservados; retomada separada.");
    event.setEvidenceReference(
        "internal://learning-cycles/" + cycle.getId() + "/cost-waivers/" + request.requestKey());
    var proof = (com.fasterxml.jackson.databind.node.ObjectNode) json.read(input);
    proof.put("productId", cycle.getProductId());
    proof.put("contract", "LEGACY_AGENT_COST_WAIVER_V1");
    proof.set(
        "tasksAtDecision",
        json.read(
            json.write(tasks.stream().filter(task -> selected.contains(task.taskId())).toList())));
    event.setEvidenceJson(json.write(proof));
    event.setCreatedAt(now);
    events.saveAndFlush(event);
  }
}
