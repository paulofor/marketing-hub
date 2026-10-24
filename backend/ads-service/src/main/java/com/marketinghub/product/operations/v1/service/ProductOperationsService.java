package com.marketinghub.product.operations.v1.service;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleValueFlowProjection;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleWorkResolver;
import com.marketinghub.product.Product;
import com.marketinghub.product.operations.v1.service.inplay.*;
import com.marketinghub.product.service.valuechainposition.ProductValueChainPositionService;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.product.operations.ProductOperationsRunRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/** Responsabilidade: consolidar a atividade oficial e a execução real sem comandar a cadeia. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOperationsService {
  private final ProductRepository products;
  private final LearningSalesCycleRepository cycles;
  private final ProductOperationsRunRepository runs;
  private final ProductValueChainPositionService positions;
  private final BusinessProcessActivityExecutionService executions;
  private final LearningCycleWorkResolver workResolver;
  private final LearningCycleValueFlowProjection valueFlow;
  private static final String CHAIN = "pde-value-creation-delivery";

  /** Filtra PLAY no banco e mantém uma falha isolada visível sem ocultar os demais produtos. */
  public ProductsInPlayResponse inPlay() {
    var items =
        products.findAllInPlayState().stream()
            .map(this::safeSnapshot)
            .sorted(
                Comparator.comparing(
                        ProductInPlayResponse::productName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(ProductInPlayResponse::productId))
            .toList();
    return new ProductsInPlayResponse(
        Instant.now(),
        items.size(),
        count(items, "EXECUTING"),
        count(items, "WAITING", "AWAITING_DECISION", "QUEUED", "NOT_STARTED"),
        count(items, "BLOCKED"),
        count(items, "PAUSED"),
        count(items, "UNAVAILABLE"),
        items);
  }

  /** Conta os estados oficiais para a tela apenas formatar os totais. */
  private int count(List<ProductInPlayResponse> items, String... statuses) {
    var accepted = Set.of(statuses);
    return (int) items.stream().filter(item -> accepted.contains(item.status())).count();
  }

  /** Registra o contexto e a exceção completa sem apresentar detalhes internos ao operador. */
  private ProductInPlayResponse safeSnapshot(Product product) {
    try {
      return snapshot(product);
    } catch (RuntimeException ex) {
      log.error("Produtos em PLAY: falha ao consultar atividade productId={}", product.getId(), ex);
      return new ProductInPlayResponse(
          product.getId(),
          name(product),
          product.getName(),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          "Atividade não confirmada",
          null,
          "UNAVAILABLE",
          label("UNAVAILABLE"),
          "Não foi possível confirmar a atividade. Tente atualizar ou consulte o histórico.",
          "/products/" + product.getId() + "/value-chain-history",
          null);
    }
  }

  /** Reutiliza a fila persistida e a projeção canônica, preservando a cadeia original do ciclo. */
  private ProductInPlayResponse snapshot(Product product) {
    var cycle =
        cycles
            .findFirstByProductIdAndChainCodeAndOpenSlot(product.getId(), CHAIN, 1)
            .filter(c -> "OPEN".equals(c.getStatus()) && c.getClosedAt() == null)
            .orElse(null);
    Long cycleId = cycle == null ? null : cycle.getId();
    String reference = cycle == null ? null : "experiment:" + cycle.getExperimentId();
    Long processId = null;
    Long chainId = null;
    ProductProcessActivityExecutionHistoryResponse initial = null;
    if (cycle == null) {
      var position = positions.getPosition(product.getId());
      var subprocess = position.subprocessPosition();
      processId =
          subprocess != null && subprocess.currentSubprocessDefinitionId() != null
              ? subprocess.currentSubprocessDefinitionId()
              : position.processDefinitionId();
      chainId = position.chainDefinitionId();
      if (processId != null && chainId != null) {
        initial =
            executions.productProcessExecutions(
                processId, product.getId(), null, chainId, false, null);
        reference = initial.currentExecutionReference();
      }
    }
    var roots =
        reference == null
            ? List.<ProcessRun>of()
            : runs.findOpenRootsForOperations(product.getId(), cycleId, reference);
    ProcessRun run = roots.isEmpty() ? null : leaf(roots.getFirst());
    if (run != null) {
      processId = run.getProcessDefinitionId();
      chainId = run.getChainDefinitionId();
      reference = run.getSourceReference();
    } else if (cycle != null) {
      var work = workResolver.resolve(cycle);
      processId = work == null ? cycle.getProcessDefinitionId() : work.processDefinitionId();
      chainId = cycle.getChainDefinitionId();
    }
    if (processId == null || chainId == null)
      throw new IllegalStateException(
          "Posição oficial não identificada para produto " + product.getId());
    var history =
        run == null && initial != null
            ? initial
            : executions.productProcessExecutions(
                processId, product.getId(), cycleId, chainId, false, reference);
    if (!Objects.equals(history.productId(), product.getId())
        || !Objects.equals(history.selectedProcessDefinitionId(), processId))
      throw new IllegalStateException("Projeção de atividade pertence a outro contexto");
    if (reference != null && !Objects.equals(reference, history.currentExecutionReference()))
      throw new IllegalStateException("Referência da atividade diverge da execução selecionada");
    reference = history.currentExecutionReference();
    var activity =
        history.activities().stream()
            .filter(
                a ->
                    a.selectedVersionActivity()
                        && Objects.equals(a.activityId(), history.currentActivityId()))
            .findFirst()
            .orElse(null);
    String state = status(activity, run, history.objectiveAchieved());
    String reason =
        activity == null ? "Consulte o histórico e a próxima passagem." : activity.stateReason();
    if (run != null
        && run.getReason() != null
        && !Set.of("EXECUTING", "COMPLETED").contains(state)
        && ("PAUSED".equals(state)
            || "QUEUED".equals(state) && "QUEUED".equals(run.getStatus())
            || !"QUEUED".equals(state)
                && Objects.equals(run.getCurrentActivityId(), history.currentActivityId())))
      reason = run.getReason();
    if (cycle != null
        && Objects.equals(processId, cycle.getProcessDefinitionId())
        && !Set.of("EXECUTING", "PAUSED", "COMPLETED").contains(state)) {
      var flow = valueFlow.resolve(cycle, null);
      if (Boolean.TRUE.equals(flow.decisionNeeded())) {
        state = "AWAITING_DECISION";
        reason = flow.decisionReason();
      }
    }
    String sequence =
        history.chainPosition() == null ? null : history.chainPosition().sequenceLabel();
    String number =
        activity == null || activity.sequenceNumber() == null
            ? null
            : (sequence == null ? "" : sequence + ".") + activity.sequenceNumber();
    var url =
        UriComponentsBuilder.fromPath(
                "/products/"
                    + product.getId()
                    + "/value-chain-history/processes/"
                    + processId
                    + "/activities")
            .queryParam("chainId", chainId);
    if (cycleId != null) url.queryParam("learningCycleId", cycleId);
    if (reference != null) url.queryParam("sourceReference", reference);
    url.fragment(activity == null ? "process-execution" : "activity-" + activity.activityId());
    return new ProductInPlayResponse(
        product.getId(),
        name(product),
        product.getName(),
        chainId,
        cycleId,
        reference,
        processId,
        run == null ? null : run.getId(),
        activity == null ? null : activity.activityId(),
        number,
        activity == null
            ? (history.objectiveAchieved() ? "Etapa concluída" : "Atividade não confirmada")
            : activity.activityName(),
        activity == null ? null : activity.activityOwnerName(),
        state,
        label(state),
        reason,
        url.build().encode().toUriString(),
        run == null ? null : run.getLastReconciledAt());
  }

  /** Segue somente a delegação real do mesmo contexto e impede ciclos ou vínculos cruzados. */
  private ProcessRun leaf(ProcessRun root) {
    var visited = new HashSet<Long>();
    var current = root;
    while ("WAITING_SUBPROCESS".equals(current.getStatus()) && current.getChildRunId() != null) {
      if (!visited.add(current.getId()) || visited.size() > 16)
        throw new IllegalStateException("Delegação circular na execução " + root.getId());
      var child = runs.findById(current.getChildRunId()).orElseThrow();
      if (!Objects.equals(child.getProductId(), root.getProductId())
          || !Objects.equals(child.getChainDefinitionId(), root.getChainDefinitionId())
          || !Objects.equals(child.getLearningCycleId(), root.getLearningCycleId())
          || !Objects.equals(child.getSourceReference(), root.getSourceReference())
          || !Objects.equals(child.getParentRunId(), current.getId()))
        throw new IllegalStateException("Subprocesso fora do contexto da execução " + root.getId());
      if (Set.of("COMPLETED", "CLOSED").contains(child.getStatus())) break;
      current = child;
    }
    return current;
  }

  /** Distingue tarefa executando, fila, espera humana e pausa sem usar PLAY como execução. */
  private String status(
      ProductProcessActivityExecutionGroupResponse activity, ProcessRun run, boolean completed) {
    if (completed) return "COMPLETED";
    if (run != null && "PAUSED".equals(run.getStatus())) return "PAUSED";
    if (activity == null) return "UNAVAILABLE";
    String state = activity.operationalState();
    if ("IN_PROGRESS".equals(state)) return "EXECUTING";
    if ("PENDING".equals(state)) return "QUEUED";
    var control = activity.executionControl();
    if (control != null
        && ("HUMAN".equals(control.executorType())
            || "APPROVAL".equals(control.interactionType())
            || control.confirmationRequired())) return "AWAITING_DECISION";
    boolean same = run != null && Objects.equals(run.getCurrentActivityId(), activity.activityId());
    if (same && "WAITING_HUMAN".equals(run.getStatus())) return "AWAITING_DECISION";
    if (same && Set.of("BLOCKED", "ERROR").contains(run.getStatus()) || "BLOCKED".equals(state))
      return "BLOCKED";
    if (run != null && "QUEUED".equals(run.getStatus())) return "QUEUED";
    if (same && run.getStatus().startsWith("WAITING") || "WAITING".equals(state)) return "WAITING";
    if ("NOT_STARTED".equals(state)) return "NOT_STARTED";
    if ("CANCELLED".equals(state)) return "BLOCKED";
    return "UNAVAILABLE";
  }

  /** Prioriza o nome interno que o operador reconhece sem depender de identidades fixas. */
  private String name(Product product) {
    if (product.getInternalName() != null && !product.getInternalName().isBlank())
      return product.getInternalName();
    if (product.getName() != null && !product.getName().isBlank()) return product.getName();
    return "Produto " + product.getId();
  }

  /** Centraliza os rótulos operacionais do contrato sem regras de negócio no navegador. */
  private String label(String state) {
    return switch (state) {
      case "EXECUTING" -> "Executando";
      case "QUEUED" -> "Na fila";
      case "AWAITING_DECISION" -> "Aguardando decisão";
      case "WAITING" -> "Aguardando";
      case "BLOCKED" -> "Bloqueada";
      case "PAUSED" -> "Pausada";
      case "NOT_STARTED" -> "Aguardando início";
      case "COMPLETED" -> "Concluída";
      default -> "Não confirmado";
    };
  }
}
