package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand;
import com.marketinghub.product.service.agentvalidation.PdeAgentValidationGateActivityExecutor;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: continuar a integração e suas revisões no processo ativo sem comandos humanos
 * duplicados.
 */
@Component
public class LearningCycleVideoContinuation {
  private final LearningSalesCycleRepository cycles;
  private final ProductRepository products;
  private final BusinessProcessDefinitionRepository definitions;
  private final BusinessProcessChainDefinitionRepository chains;
  private final BusinessProcessActivityDefinitionRepository activityDefinitions;
  private final AgentTaskRepository tasks;
  private final LearningCycleVideoBinding binding;
  private final LearningCycleVideoBudget budget;
  private final LearningCycleService service;
  private final LearningCycleEvidence evidence;
  private final BusinessProcessActivityExecutionService executions;
  private final PdeAgentValidationGateActivityExecutor gate;
  private final ObjectMapper json;

  /** Configura o coordenador e os comandos canônicos, resolvidos somente ao executar o processo. */
  public LearningCycleVideoContinuation(
      LearningSalesCycleRepository cycles,
      ProductRepository products,
      BusinessProcessDefinitionRepository definitions,
      BusinessProcessChainDefinitionRepository chains,
      BusinessProcessActivityDefinitionRepository activityDefinitions,
      AgentTaskRepository tasks,
      LearningCycleVideoBinding binding,
      LearningCycleVideoBudget budget,
      @Lazy LearningCycleService service,
      LearningCycleEvidence evidence,
      @Lazy BusinessProcessActivityExecutionService executions,
      @Lazy PdeAgentValidationGateActivityExecutor gate,
      ObjectMapper json) {
    this.cycles = cycles;
    this.products = products;
    this.definitions = definitions;
    this.chains = chains;
    this.activityDefinitions = activityDefinitions;
    this.tasks = tasks;
    this.binding = binding;
    this.budget = budget;
    this.service = service;
    this.evidence = evidence;
    this.executions = executions;
    this.gate = gate;
    this.json = json;
  }

  /** Explica somente trabalho efetivamente enfileirado ou uma pendência comprovada. */
  public record Progress(String status, String reason) {}

  /** Executa no máximo uma nova tarefa por conciliação, preservando pausa, STOP e versões. */
  public Progress advance(ProcessRun run) {
    if (run.getLearningCycleId() == null
        || Set.of("PAUSING", "PAUSED", "CLOSED", "COMPLETED", "ERROR").contains(run.getStatus()))
      return null;
    var parent = definitions.findById(run.getProcessDefinitionId()).orElseThrow();
    if (!"pde-sales-delivery-learning".equals(parent.getProcessCode())) return null;
    var cycle = cycles.findLockedById(run.getLearningCycleId()).orElseThrow();
    require(
        Objects.equals(run.getProductId(), cycle.getProductId())
            && Objects.equals(run.getChainDefinitionId(), cycle.getChainDefinitionId())
            && Objects.equals(run.getSourceReference(), "experiment:" + cycle.getExperimentId()),
        "A continuidade pertence a outro contexto de processo.");
    if (!"OPEN".equals(cycle.getStatus())
        || !Set.of("VIDEO_APPROVAL", "VALIDATION").contains(cycle.getStage())
        || !binding.supports(cycle)) return null;
    var product = products.findById(cycle.getProductId()).orElseThrow();
    if (Boolean.FALSE.equals(product.getAutomaticExecutionEnabled()))
      return new Progress(
          "WAITING_INPUT", "O produto está em STOP. Aprovações e resultados foram preservados.");
    if ("VIDEO_APPROVAL".equals(cycle.getStage())) {
      if (binding.awaitingApproval(cycle)) return null;
      service.integrateApprovedVideos(cycle.getProductId(), cycle.getId());
      return new Progress(
          "WAITING_ACTIVITY",
          "Vídeos aprovados e integrados. A homologação do conjunto está na fila de continuidade automática.");
    }
    if (binding.receipt(cycle).isEmpty()) return null;
    var integrated = binding.current(cycle);
    var process =
        chains.findById(cycle.getChainDefinitionId()).orElseThrow().getItems().stream()
            .map(i -> i.getProcessDefinition())
            .filter(p -> "pde-construction-approval".equals(p.getProcessCode()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("O ciclo não possui subprocesso de homologação."));
    var history =
        tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            process.getId(), run.getSourceReference());
    for (String code : LearningCycleVideoBinding.REVIEWS) {
      var task =
          history.stream()
              .filter(t -> code.equals(t.getProcessActivityId()))
              .reduce((a, b) -> b)
              .orElse(null);
      if (!binding.currentTask(cycle, task)) {
        var authorized = budget.current(cycle);
        require(
            authorized != null
                && authorized.budgetLimitUsd() != null
                && authorized.budgetLimitUsd().signum() > 0,
            "A revisão dos vídeos depende do teto financeiro já autorizado para esta versão. Nenhuma tarefa paga foi criada.");
        var dispatched =
            executions.requestProductActivityExecution(
                process.getId(), product.getId(), code, null, cycle.getId());
        require(
            !dispatched.tasks().isEmpty(),
            "A homologação não criou a tarefa esperada: " + code + ".");
        return new Progress(
            "WAITING_ACTIVITY",
            "Vídeos aprovados e integrados. "
                + code
                + " enfileirada automaticamente · tarefa #"
                + dispatched.tasks().getFirst().id()
                + ".");
      }
      if (Set.of("PENDING", "IN_PROGRESS").contains(task.getStatus()))
        return new Progress(
            "WAITING_ACTIVITY",
            "Vídeos aprovados e integrados. Aguardando "
                + task.getProcessActivityName()
                + " · tarefa #"
                + task.getId()
                + ".");
      require(
          "COMPLETED".equals(task.getStatus()),
          "A tarefa #"
              + task.getId()
              + " exige correção: "
              + Objects.requireNonNullElse(task.getExecutionError(), task.getStatus())
              + ". Nenhuma tentativa paga foi repetida.");
      require(
          "APPROVED".equals(result(task).path("decision").asText()),
          "O parecer da tarefa #"
              + task.getId()
              + " exige correção antes da próxima revisão. Nenhuma tentativa paga foi repetida.");
    }
    var reviewed =
        LearningCycleVideoBinding.REVIEWS.stream()
            .map(
                code ->
                    history.stream()
                        .filter(t -> code.equals(t.getProcessActivityId()))
                        .reduce((a, b) -> b)
                        .orElseThrow())
            .toList();
    binding.validateReviews(run.getSourceReference(), reviewed);
    var gateActivity =
        activityDefinitions
            .findByProcessDefinitionIdAndActivityId(process.getId(), "agentValidationGate")
            .orElseThrow();
    gate.execute(process, gateActivity, product, run.getSourceReference());
    var approvals = evidence.approvals(cycle);
    require(approvals.size() == 1, "A homologação atual precisa de um gate único aprovado.");
    var proof = json.createObjectNode();
    proof.put("approvalInstanceId", approvals.getFirst().id());
    proof.put(
        "journeyEvidence",
        "Gate multiagente #"
            + approvals.getFirst().id()
            + "; integração "
            + integrated.path("integrationFingerprint").asText());
    proof.put("instrumentationVerified", true);
    service.completeIntegratedVideoValidation(
        product.getId(),
        cycle.getId(),
        new LearningCycleCommand(
            UUID.nameUUIDFromBytes(
                ("video-validation:" + cycle.getId() + ":" + cycle.getRevision())
                    .getBytes(StandardCharsets.UTF_8)),
            cycle.getRevision(),
            LearningCycleCommand.Action.COMPLETE,
            "Marketing Hub · continuidade automática",
            "Homologação do conjunto com os vídeos concluída pelos executores e gates oficiais. Aprovações humanas das peças preservadas; autorização comercial permanece separada.",
            "business_process_activity_instance:" + approvals.getFirst().id(),
            proof));
    return null;
  }

  /** Lê o parecer persistido e preserva contexto e stack trace em falha de contrato. */
  private com.fasterxml.jackson.databind.JsonNode result(
      com.marketinghub.agenttask.AgentTask task) {
    try {
      return json.readTree(task.getResultJson() == null ? "{}" : task.getResultJson());
    } catch (Exception ex) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .error(
              "Parecer audiovisual ilegível taskId={} sourceReference={}",
              task.getId(),
              task.getSourceReference(),
              ex);
      throw new IllegalStateException(
          "O parecer da tarefa #" + task.getId() + " está ilegível.", ex);
    }
  }
}
