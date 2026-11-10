package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: preservar recibos individuais dos tetos sem criar autorizações paralelas. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleCostCeilings {
  private static final String ACTION = "APPROVE_COST_CEILING";
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleJson json;
  private final LearningCycleCumulativeBudgetHistory cumulative;

  /** Confere contexto e recupera somente uma decisão idêntica, antes de reaplicar limites. */
  public boolean replay(LearningSalesCycle cycle, ApproveCostCeilingRequest request) {
    require(
        Objects.equals(request.chainDefinitionId(), cycle.getChainDefinitionId())
            && Objects.equals(request.experimentId(), cycle.getExperimentId())
            && Objects.equals(request.productVersion(), cycle.getProductVersion()),
        "A decisão pertence a outro contexto ou versão.");
    var previous =
        events.findByCycleIdAndRequestKey(cycle.getId(), request.requestKey().toString());
    if (previous.isEmpty()) return false;
    require(
        ACTION.equals(previous.get().getAction())
            && json.read(json.write(request)).equals(json.read(previous.get().getRequestJson())),
        "A chave desta decisão já foi usada com outros valores.");
    return true;
  }

  /** Registra a decisão sob o lock do ciclo depois de conferir o contrato operacional aplicável. */
  public void record(LearningSalesCycle cycle, ApproveCostCeilingRequest request, Instant now) {
    cycle.setRevision(cycle.getRevision() + 1);
    cycle.setUpdatedAt(now);
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRevision(cycle.getRevision());
    event.setRequestKey(request.requestKey().toString());
    event.setRequestJson(json.write(request));
    event.setEvidenceJson(json.write(request));
    event.setAction(ACTION);
    event.setFromStage(cycle.getStage());
    event.setToStage(cycle.getStage());
    event.setOperatorName(request.operatorName().trim());
    event.setSummary(
        "Teto aprovado para "
            + request.code()
            + ": "
            + request.approved()
            + ". Escopo e gates operacionais preservados; sem início de gasto.");
    event.setEvidenceReference(
        "internal://learning-cycles/" + cycle.getId() + "/cost-ceilings/" + request.requestKey());
    event.setCreatedAt(now);
    events.saveAndFlush(event);
    log.info(
        "Teto financeiro registrado productId={} cycleId={} eventId={} code={} approved={}",
        cycle.getProductId(),
        cycle.getId(),
        event.getId(),
        request.code(),
        request.approved());
  }

  /** Mantém um teto por grupo na linhagem, sem renovar valores pela mudança de ciclo. */
  public List<CostWorksheet.CeilingReceipt> history(LearningSalesCycle cycle) {
    Set<String> seen = new HashSet<>();
    return cumulative.history(cycle, ACTION).stream()
        .map(
            receipt -> {
              var event = receipt.event();
              var data = json.read(event.getEvidenceJson());
              String code = data.path("code").asText();
              return new CostWorksheet.CeilingReceipt(
                  event.getId(),
                  code,
                  data.path("approved").decimalValue(),
                  event.getOperatorName(),
                  event.getCreatedAt(),
                  data.path("justification").asText(),
                  receipt.eligible() && seen.add(code),
                  event.getCycleId(),
                  !Objects.equals(cycle.getId(), event.getCycleId()));
            })
        .toList();
  }
}
