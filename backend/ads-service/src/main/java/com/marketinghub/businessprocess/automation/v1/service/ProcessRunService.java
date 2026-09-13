package com.marketinghub.businessprocess.automation.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.*;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.automation.v1.service.status.*;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar comandos e progressão durável dos processos sem agendamento local. */
@Service
@Slf4j
public class ProcessRunService {
  private static final Set<String> ACTIVE_TASK = Set.of("PENDING", "IN_PROGRESS");
  private static final Set<String> OMITTED = Set.of("HISTORICAL", "NOT_APPLICABLE", "RECORDED");
  private final ProcessRunRepository runs;
  private final ProcessRunEventRepository events;
  private final ProductRepository products;
  private final ProcessRunContext context;
  private final ProcessRunNavigation navigation;
  private final ProcessRunSubprocesses subprocesses;
  private final ProcessRunGuidance guidance;
  private final BusinessProcessActivityExecutionService activities;
  private final ObjectMapper json;
  private final TransactionTemplate transaction;

  /** Configura transações curtas e os contratos responsáveis pela execução real das atividades. */
  public ProcessRunService(
      ProcessRunRepository runs,
      ProcessRunEventRepository events,
      ProductRepository products,
      ProcessRunContext context,
      ProcessRunNavigation navigation,
      ProcessRunSubprocesses subprocesses,
      ProcessRunGuidance guidance,
      BusinessProcessActivityExecutionService activities,
      ObjectMapper json,
      PlatformTransactionManager manager) {
    this.runs = runs;
    this.events = events;
    this.products = products;
    this.context = context;
    this.navigation = navigation;
    this.subprocesses = subprocesses;
    this.guidance = guidance;
    this.activities = activities;
    this.json = json;
    this.transaction = new TransactionTemplate(manager);
  }

  /**
   * Mostra progresso e confere objetivos concluídos para permitir revalidação sem apagar o diário.
   */
  public ProcessRunResponse status(Long productId, Long processId, ProcessRunCommand command) {
    return transaction.execute(
        ignored -> {
          var existing = runs.findByScopeKey(scope(productId, processId, command));
          if (existing.isPresent()) {
            var run = existing.get();
            return "COMPLETED".equals(run.getStatus())
                ? response(run, context.read(run, false))
                : response(run);
          }
          var history = context.read(productId, processId, command, false);
          var preview = newRun(productId, processId, command, null);
          updateCounts(preview, history);
          boolean enabled =
              !Boolean.FALSE.equals(
                  products.findById(productId).orElseThrow().getAutomaticExecutionEnabled());
          boolean missingReference =
              command.sourceReference() == null || command.sourceReference().isBlank();
          preview.setStatus(
              missingReference
                  ? "UNAVAILABLE"
                  : history.objectiveAchieved()
                      ? "COMPLETED"
                      : enabled && "PUBLISHED".equals(history.selectedProcessStatus())
                          ? "READY"
                          : "UNAVAILABLE");
          preview.setReason(
              missingReference
                  ? "Aguardando o contexto oficial de execução; a navegação entre os processos continua disponível."
                  : history.objectiveAchieved()
                      ? "Os objetivos aplicáveis deste processo já foram comprovados."
                      : !enabled
                          ? "O produto está em STOP."
                          : !"PUBLISHED".equals(history.selectedProcessStatus())
                              ? "Esta versão não está publicada."
                              : "Execute o processo para iniciar as atividades em sequência. Você pode fechar esta tela.");
          return response(preview);
        });
  }

  /** Registra uma única autorização durável por contexto, sem usar o navegador como executor. */
  public ProcessRunResponse start(Long productId, Long processId, ProcessRunCommand command) {
    return transaction.execute(
        ignored -> {
          lockProduct(productId);
          var existing = runs.findByScopeKey(scope(productId, processId, command));
          if (existing.isPresent()) return response(existing.get());
          var history = context.read(productId, processId, command, true);
          requirePlay(productId);
          if (!"PUBLISHED".equals(history.selectedProcessStatus()))
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Somente a versão publicada pode iniciar o processo.");
          context.graph(processId);
          var run = newRun(productId, processId, command, null);
          updateCounts(run, history);
          if (history.objectiveAchieved()) {
            run.setStatus("COMPLETED");
            run.setFinishedAt(Instant.now());
            run.setReason("Os objetivos aplicáveis já foram comprovados.");
          }
          runs.saveAndFlush(run);
          event(
              run,
              "STARTED",
              run.getReason(),
              null,
              Map.of(
                  "sourceReference",
                  run.getSourceReference(),
                  "chainId",
                  run.getChainDefinitionId()));
          return response(run);
        });
  }

  /** Pausa novos disparos preservando a execução e o resultado de tarefas já reservadas. */
  public ProcessRunResponse pause(Long productId, Long processId, Long runId) {
    return locked(
        productId,
        processId,
        runId,
        run -> {
          if (!Set.of("PAUSING", "PAUSED", "COMPLETED", "CLOSED").contains(run.getStatus()))
            transition(
                run,
                "PAUSING",
                "Pausa solicitada. Aguardando tarefas já iniciadas; nenhuma próxima atividade será disparada.",
                "PAUSE_REQUESTED");
          return response(run);
        });
  }

  /**
   * Confere resultados já obtidos antes de autorizar nova tentativa após revisão do impedimento.
   */
  public ProcessRunResponse resume(Long productId, Long processId, Long runId) {
    return locked(
        productId,
        processId,
        runId,
        run -> {
          if ("CLOSED".equals(run.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, run.getReason());
          boolean revalidation = "COMPLETED".equals(run.getStatus());
          if (Set.of("PAUSING", "PAUSED", "BLOCKED", "ERROR", "WAITING_INPUT", "COMPLETED")
              .contains(run.getStatus())) {
            var readiness = context.read(run, false);
            if (completedObjectives(readiness)) {
              if (!revalidation) {
                updateCounts(run, readiness);
                complete(run);
              }
              return response(run);
            }
            requirePlay(productId);
            String blocker = context.dispatchBlockReason(run);
            if (blocker != null) throw new ResponseStatusException(HttpStatus.CONFLICT, blocker);
            updateCounts(run, readiness);
            run.setFinishedAt(null);
            run.setRetryEpoch(run.getRetryEpoch() + 1);
            run.setFailureCount(0);
            transition(
                run,
                "QUEUED",
                revalidation
                    ? "Revalidação autorizada. As provas atuais serão conferidas sem apagar as aprovações anteriores."
                    : "Retomada registrada; os contratos e os resultados serão conferidos antes de executar.",
                revalidation ? "REVALIDATION_REQUESTED" : "RESUMED");
          }
          return response(run);
        });
  }

  /** Entrega IDs duráveis em ordem justa para o módulo externo solicitar conciliação. */
  public List<Long> pending(int limit) {
    return runs.pending(PageRequest.of(0, Math.max(1, Math.min(limit, 100))));
  }

  /**
   * Concilia um contexto atomicamente e registra falhas depois de reverter a transação inválida.
   */
  public ProcessRunResponse reconcile(Long runId) {
    var identity =
        runs.identity(runId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Execução não encontrada."));
    try {
      return locked(identity.productId(), identity.processDefinitionId(), runId, this::advance);
    } catch (RuntimeException ex) {
      log.error(
          "Falha na conciliação do processo. runId={} productId={} processDefinitionId={}",
          runId,
          identity.productId(),
          identity.processDefinitionId(),
          ex);
      return locked(
          identity.productId(),
          identity.processDefinitionId(),
          runId,
          run -> {
            if (Set.of("PAUSING", "PAUSED", "COMPLETED", "CLOSED").contains(run.getStatus()))
              return response(run);
            run.setFailureCount(run.getFailureCount() + 1);
            run.setLastReconciledAt(Instant.now());
            String reason =
                ex instanceof ResponseStatusException response
                    ? response.getReason()
                    : "Falha técnica ao conferir ou executar a atividade. Consulte o registro desta execução.";
            transition(
                run,
                ex instanceof ResponseStatusException
                    ? "BLOCKED"
                    : run.getFailureCount() >= 3 ? "ERROR" : "WAITING_INPUT",
                reason,
                "RECONCILIATION_FAILED");
            return response(run);
          });
    }
  }

  /** Lê o diário paginado exclusivamente dentro do produto, processo e execução solicitados. */
  public List<ProcessRunEventResponse> history(
      Long productId, Long processId, Long runId, Long beforeId) {
    return transaction.execute(
        ignored -> {
          requiredRun(productId, processId, runId);
          return events.history(runId, beforeId, PageRequest.of(0, 50)).stream()
              .map(
                  e -> {
                    try {
                      return new ProcessRunEventResponse(
                          e.getId(),
                          e.getEventType(),
                          e.getStatus(),
                          e.getActivityId(),
                          e.getMessage(),
                          json.readTree(e.getDetailsJson()),
                          e.getCreatedAt());
                    } catch (Exception ex) {
                      log.error(
                          "Falha ao ler diário do processo. runId={} eventId={}",
                          runId,
                          e.getId(),
                          ex);
                      throw new IllegalStateException("Diário da execução inválido.", ex);
                    }
                  })
              .toList();
        });
  }

  /** Observa provas e decisões humanas antes dos novos disparos e aplica um comando canônico. */
  private ProcessRunResponse advance(ProcessRun run) {
    if (Set.of("PAUSED", "COMPLETED", "ERROR", "CLOSED").contains(run.getStatus()))
      return response(run);
    run.setLastReconciledAt(Instant.now());
    var snapshot = context.read(run, false);
    updateCounts(run, snapshot);
    run.setFailureCount(0);
    var graph = context.graph(run.getProcessDefinitionId());
    var ordered = graph.ordered(snapshot.activities());
    if ("PAUSING".equals(run.getStatus())) {
      if (inFlight(run, new HashSet<>())) return response(run);
      if (!completedObjectives(snapshot)) {
        transition(
            run,
            "PAUSED",
            "Pausado. Resultados preservados; retome quando desejar continuar.",
            "PAUSED");
        return response(run);
      }
    }
    var active =
        ordered.stream().filter(a -> ACTIVE_TASK.contains(a.operationalState())).findFirst();
    if (active.isPresent()) {
      current(run, active.get());
      var userAction = guidance.resolve(run);
      if (userAction != null) run.setCurrentOwnerName(userAction.responsible());
      transition(
          run,
          userAction == null ? "WAITING_ACTIVITY" : "WAITING_HUMAN",
          userAction == null
              ? (active.get().stateReason() == null || active.get().stateReason().isBlank()
                  ? "Aguardando a conclusão validada da atividade."
                  : active.get().stateReason())
              : userAction.reason(),
          "WAITING");
      return response(run);
    }
    if (!closeUnneededChildren(run, snapshot)) return response(run);
    if (completedObjectives(snapshot)) {
      complete(run);
      return response(run);
    }
    String blocker = context.dispatchBlockReason(run);
    if (blocker != null) {
      run.setCurrentActivityId(null);
      run.setCurrentActivityName(null);
      run.setCurrentOwnerName(null);
      run.setCurrentSequence(null);
      run.setFinishedAt(Instant.now());
      run.setNavigationUrl(null);
      transition(run, "CLOSED", blocker, "CONTEXT_CLOSED");
      return response(run);
    }
    if (ancestorPaused(run)) {
      transition(run, "WAITING_PARENT", "Aguardando a retomada do processo de origem.", "WAITING");
      return response(run);
    }
    if (Boolean.FALSE.equals(
        products.findById(run.getProductId()).orElseThrow().getAutomaticExecutionEnabled())) {
      transition(
          run,
          "WAITING_INPUT",
          "O produto está em STOP. O processo continuará após o produto voltar a PLAY.",
          "PRODUCT_STOPPED");
      return response(run);
    }
    var roots = runs.activeRoots(run.getProductId());
    Long rootId = rootId(run);
    if (!roots.isEmpty() && !Objects.equals(roots.getFirst().getId(), rootId)) {
      transition(
          run,
          "QUEUED",
          "Aguardando outro processo deste produto encerrar ou ser pausado.",
          "QUEUED");
      return response(run);
    }
    var candidates =
        ordered.stream()
            .filter(
                a ->
                    a.selectedVersionActivity()
                        && !a.objectiveAchieved()
                        && !OMITTED.contains(a.operationalState()))
            .toList();
    var recovery = candidates.stream().filter(a -> graph.recovery(a, ordered)).findFirst();
    var selected =
        recovery.or(
            () ->
                candidates.stream()
                    .filter(a -> graph.predecessorsSatisfied(a.activityId(), ordered))
                    .findFirst());
    if (selected.isEmpty()) {
      transition(
          run,
          "BLOCKED",
          "Nenhuma atividade possui predecessoras e contrato de avanço satisfeitos. Revise os bloqueios e o BPM deste processo.",
          "BLOCKED");
      return response(run);
    }
    var activity = selected.get();
    current(run, activity);
    var control = activity.executionControl();
    if (control == null) {
      transition(
          run, "BLOCKED", "A atividade não possui contrato de execução automática.", "BLOCKED");
      return response(run);
    }
    if (control.confirmationRequired()
        || "HUMAN".equals(control.executorType())
        || "APPROVAL".equals(control.interactionType())) {
      transition(run, "WAITING_HUMAN", control.availabilityReason(), "HUMAN_ACTION_REQUIRED");
      return response(run);
    }
    if ("SUBPROCESS".equals(control.interactionType())) {
      if (!control.actionAvailable() || control.targetProcessDefinitionId() == null) {
        transition(run, "WAITING_INPUT", control.availabilityReason(), "WAITING");
        return response(run);
      }
      var childCommand = context.command(run);
      Long childProcess =
          navigation.children(run).stream()
              .filter(relation -> activity.activityId().equals(relation.activityId()))
              .map(ProcessRunRelationResponse::processDefinitionId)
              .findFirst()
              .orElse(control.targetProcessDefinitionId());
      var childReadiness = context.read(run.getProductId(), childProcess, childCommand, true);
      var child =
          runs.findByScopeKey(scope(run.getProductId(), childProcess, childCommand))
              .orElseGet(
                  () -> {
                    var created =
                        runs.saveAndFlush(
                            newRun(run.getProductId(), childProcess, childCommand, run.getId()));
                    event(
                        created,
                        "STARTED",
                        "Subprocesso iniciado pela chamada do processo de origem.",
                        null,
                        Map.of("parentRunId", run.getId()));
                    return created;
                  });
      if (child.getId().equals(rootId) || child.getId().equals(run.getId()))
        throw new IllegalStateException("Chamada recursiva de subprocesso recusada.");
      if (child.getParentRunId() == null && !"COMPLETED".equals(child.getStatus())) {
        child.setParentRunId(run.getId());
        event(
            child,
            "DELEGATION_LINKED",
            "Execução existente vinculada à chamada do processo de origem, sem duplicar trabalho.",
            null,
            Map.of("parentRunId", run.getId()));
      } else if (child.getParentRunId() != null
          && !Objects.equals(child.getParentRunId(), run.getId())) {
        transition(
            run,
            "BLOCKED",
            "O subprocesso já pertence a outra chamada em andamento. Aguarde sua conclusão.",
            "DELEGATION_CONFLICT");
        return response(run);
      }
      if ("COMPLETED".equals(child.getStatus()) && !childReadiness.objectiveAchieved()) {
        child.setParentRunId(run.getId());
        child.setFinishedAt(null);
        child.setRetryEpoch(child.getRetryEpoch() + 1);
        updateCounts(child, childReadiness);
        transition(
            child,
            "QUEUED",
            "O processo de origem requer revalidação das provas atuais do subprocesso.",
            "REVALIDATION_REQUESTED");
      }
      run.setChildRunId(child.getId());
      run.setNavigationUrl(
          navigation.children(run).stream()
              .filter(relation -> activity.activityId().equals(relation.activityId()))
              .map(ProcessRunRelationResponse::navigationUrl)
              .findFirst()
              .orElse(null));
      if ("COMPLETED".equals(child.getStatus()) && completedObjectives(childReadiness)) {
        if (child.getParentRunId() == null) child.setParentRunId(run.getId());
        subprocesses.complete(run, activity, child, childReadiness);
        transition(
            run,
            "RUNNING",
            "Subprocesso concluído e confirmado na atividade de origem.",
            "SUBPROCESS_COMPLETED");
        return response(run);
      }
      transition(
          run,
          "WAITING_SUBPROCESS",
          "Aguardando o subprocesso " + childReadiness.processName() + ": " + child.getReason(),
          "SUBPROCESS");
      return response(run);
    }
    if (!Set.of("COMMAND", "WORKSPACE").contains(control.interactionType())
        || !control.actionAvailable()
        || !activity.executionRequestAvailable()) {
      transition(run, "WAITING_INPUT", control.availabilityReason(), "WAITING");
      return response(run);
    }
    var correctionInputs = ProcessRunCorrectionInputs.resolve(run, activity, ordered, graph, json);
    String actionKey = actionKey(run, activity, ordered, correctionInputs);
    if (events.existsByRunIdAndActionKey(run.getId(), actionKey)) {
      transition(
          run,
          "BLOCKED",
          "A tentativa não comprovou o objetivo e as entradas continuam iguais. "
              + failureReason(activity)
              + " Corrija o impedimento antes de retomar o processo.",
          "NO_PROGRESS");
      return response(run);
    }
    var result =
        activities.requestProductActivityExecution(
            run.getProcessDefinitionId(),
            run.getProductId(),
            activity.activityId(),
            null,
            run.getLearningCycleId());
    if (!Objects.equals(run.getSourceReference(), result.sourceReference()))
      throw new IllegalStateException("O comando tentou executar em outra referência operacional.");
    transition(
        run,
        "WAITING_ACTIVITY",
        "Atividade solicitada; aguardando resultado e validação do objetivo.",
        "DISPATCHED");
    event(
        run,
        recovery.isPresent() || !correctionInputs.isEmpty()
            ? "RECOVERY_REQUESTED"
            : "ACTIVITY_REQUESTED",
        run.getReason(),
        actionKey,
        Map.of(
            "sourceReference",
            result.sourceReference(),
            "activityId",
            activity.activityId(),
            "taskIds",
            result.tasks().stream().map(t -> t.id()).toList(),
            "retryEpoch",
            run.getRetryEpoch(),
            "correctionInputHash",
            correctionInputs.isEmpty() ? "" : hash(correctionInputs.toString())));
    return response(run);
  }

  /**
   * Encerra filhos cuja chamada foi substituída por objetivo backend comprovado, sem aprovar
   * tarefas.
   */
  private boolean closeUnneededChildren(
      ProcessRun parent, ProductProcessActivityExecutionHistoryResponse snapshot) {
    for (var child : runs.findAllByParentRunId(parent.getId())) {
      if (Set.of("COMPLETED", "CLOSED").contains(child.getStatus())) continue;
      var relation =
          navigation.children(parent).stream()
              .filter(r -> child.getProcessDefinitionId().equals(r.processDefinitionId()))
              .findFirst();
      if (relation.isEmpty()) continue;
      var activity =
          snapshot.activities().stream()
              .filter(a -> relation.get().activityId().equals(a.activityId()))
              .findFirst();
      if (activity.isEmpty() || !activity.get().objectiveAchieved()) continue;
      var control = activity.get().executionControl();
      if (control == null
          || !"BACKEND".equals(control.executorType())
          || !"COMMAND".equals(control.interactionType())
          || control.targetProcessDefinitionId() != null) continue;
      if (inFlight(child, new HashSet<>())) {
        transition(
            parent,
            "WAITING_SUBPROCESS",
            "O objetivo do destino já foi comprovado; aguardando a tarefa em curso antes de encerrar a chamada anterior.",
            "WAITING_SUPERSEDED_SUBPROCESS");
        return false;
      }
      closeUnneededTree(child);
      if (Objects.equals(parent.getChildRunId(), child.getId())) parent.setChildRunId(null);
      event(
          parent,
          "SUBPROCESS_NOT_REQUIRED",
          "Destino aprovado confirmado; o subprocesso anterior foi encerrado.",
          null,
          Map.of(
              "childRunId",
              child.getId(),
              "activityId",
              activity.get().activityId(),
              "activityInstanceId",
              activity.get().activityInstanceId()));
    }
    return true;
  }

  /** Preserva resultados e custos ao encerrar uma árvore sem trabalho em curso e sem uso no pai. */
  private void closeUnneededTree(ProcessRun run) {
    for (var child : runs.findAllByParentRunId(run.getId())) {
      if (!Set.of("COMPLETED", "CLOSED").contains(child.getStatus())) closeUnneededTree(child);
    }
    run.setFinishedAt(Instant.now());
    transition(
        run,
        "CLOSED",
        "Subprocesso não necessário neste ciclo: o destino aprovado já atende ao objetivo do processo pai. Consulte as tentativas anteriores no histórico.",
        "SUBPROCESS_NOT_REQUIRED");
  }

  /** Expõe as lacunas da última tentativa bloqueada sem substituir sua causa pela prontidão. */
  private String failureReason(ProductProcessActivityExecutionGroupResponse activity) {
    var task =
        activity.tasks().stream()
            .filter(t -> "BLOCKED".equals(t.status()))
            .max(Comparator.comparing(t -> t.taskId()));
    if (task.isEmpty()) return activity.stateReason();
    String reason = task.get().executionError();
    if (task.get().comments() != null) {
      try {
        var gaps = json.readTree(task.get().comments()).path("evidenceGaps");
        List<String> details = new ArrayList<>();
        if (gaps.isArray())
          for (var gap : gaps)
            if (gap.isTextual() && !gap.asText().isBlank()) details.add(gap.asText());
        if (!details.isEmpty()) reason = String.join("; ", details);
      } catch (Exception ex) {
        log.warn(
            "Falha ao ler lacunas da tentativa. taskId={} activityId={}",
            task.get().taskId(),
            activity.activityId(),
            ex);
      }
    }
    return "Tarefa #"
        + task.get().taskId()
        + ": "
        + (reason == null ? activity.stateReason() : reason);
  }

  /** Exige todos os objetivos aplicáveis comprovados e nenhuma tarefa ainda pendente. */
  private boolean completedObjectives(ProductProcessActivityExecutionHistoryResponse snapshot) {
    return snapshot.objectiveAchieved()
        && snapshot.remainingActivityCount() == 0
        && snapshot.completedActivityCount() > 0
        && snapshot.activities().stream()
            .noneMatch(a -> ACTIVE_TASK.contains(a.operationalState()));
  }

  /** Registra conclusão comprovada e remove os atalhos que já não representam pendências. */
  private void complete(ProcessRun run) {
    run.setCurrentActivityId(null);
    run.setCurrentActivityName(null);
    run.setCurrentOwnerName(null);
    run.setCurrentSequence(null);
    run.setNavigationUrl(null);
    run.setFinishedAt(Instant.now());
    transition(
        run,
        "COMPLETED",
        "Todos os objetivos aplicáveis foram comprovados pelos contratos do processo.",
        "COMPLETED");
  }

  /** Consolida contagens e custo oficiais sem tratar omissão como sucesso ou ausência como zero. */
  private void updateCounts(
      ProcessRun run, ProductProcessActivityExecutionHistoryResponse snapshot) {
    run.setTotalActivities(snapshot.selectedActivityCount());
    run.setCompletedActivities(snapshot.completedActivityCount());
    run.setRemainingActivities(snapshot.remainingActivityCount());
    run.setOmittedActivities(
        Math.max(
            0,
            snapshot.selectedActivityCount()
                - snapshot.completedActivityCount()
                - snapshot.remainingActivityCount()));
    run.setKnownCostUsd(
        Set.of("NO_EXECUTIONS", "NOT_REPORTED").contains(snapshot.costCoverage())
            ? null
            : snapshot.knownEstimatedCostUsd());
    run.setCostCoverage(snapshot.costCoverage());
  }

  /** Mantém no cabeçalho a atividade e o responsável escolhidos pelo backend. */
  private void current(ProcessRun run, ProductProcessActivityExecutionGroupResponse activity) {
    run.setCurrentActivityId(activity.activityId());
    run.setCurrentActivityName(activity.activityName());
    run.setCurrentSequence(activity.sequenceNumber());
    run.setCurrentOwnerName(activity.activityOwnerName());
    run.setNavigationUrl(
        activity.executionControl() == null ? null : activity.executionControl().navigationUrl());
  }

  /**
   * Relaciona a tentativa à entrada, às provas e ao parecer novo; preserva chaves históricas quando
   * não há correção e nunca inclui a própria falha como progresso.
   */
  private String actionKey(
      ProcessRun run,
      ProductProcessActivityExecutionGroupResponse activity,
      List<ProductProcessActivityExecutionGroupResponse> ordered,
      List<String> correctionInputs) {
    var proofs =
        ordered.stream()
            .filter(a -> !a.activityId().equals(activity.activityId()) && a.objectiveAchieved())
            .map(
                a ->
                    a.activityId()
                        + ":"
                        + a.activityInstanceId()
                        + ":"
                        + a.occurrenceNumber()
                        + ":"
                        + a.tasks().stream()
                            .filter(t -> "COMPLETED".equals(t.status()))
                            .map(t -> t.taskId())
                            .sorted()
                            .toList())
            .toList();
    return hash(
        run.getRetryEpoch()
            + "|"
            + activity.activityId()
            + "|"
            + context.inputVersion(run)
            + "|"
            + activity.activityObjective()
            + "|"
            + proofs
            + (correctionInputs.isEmpty() ? "" : "|correction:" + correctionInputs));
  }

  /** Cria o controle com referência explícita, sem executar trabalho durante sua construção. */
  private ProcessRun newRun(
      Long productId, Long processId, ProcessRunCommand command, Long parentId) {
    var run = new ProcessRun();
    run.setProductId(productId);
    run.setProcessDefinitionId(processId);
    run.setChainDefinitionId(command.chainId());
    run.setLearningCycleId(command.learningCycleId());
    run.setSourceReference(command.sourceReference());
    run.setScopeKey(scope(productId, processId, command));
    run.setParentRunId(parentId);
    run.setStatus("QUEUED");
    run.setReason("Processo na fila para execução automática.");
    run.setCreatedAt(Instant.now());
    run.setUpdatedAt(run.getCreatedAt());
    run.setLastReconciledAt(run.getCreatedAt());
    return run;
  }

  /** Serializa somente operações do mesmo produto, preservando paralelismo entre produtos. */
  private <T> T locked(Long productId, Long processId, Long runId, Function<ProcessRun, T> action) {
    return transaction.execute(
        ignored -> {
          lockProduct(productId);
          return action.apply(requiredRun(productId, processId, runId));
        });
  }

  /** Obtém o lock também usado pelo controle PLAY/STOP do produto. */
  private void lockProduct(Long productId) {
    products
        .findLockedById(productId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
  }

  /** Recusa uma nova autorização enquanto o produto estiver em STOP. */
  private void requirePlay(Long productId) {
    if (Boolean.FALSE.equals(
        products.findById(productId).orElseThrow().getAutomaticExecutionEnabled()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "O produto está em STOP.");
  }

  /** Protege a leitura e os comandos contra IDs de outro produto ou processo. */
  private ProcessRun requiredRun(Long productId, Long processId, Long id) {
    return runs.findById(id)
        .filter(
            r -> productId.equals(r.getProductId()) && processId.equals(r.getProcessDefinitionId()))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Execução não encontrada neste produto e processo."));
  }

  /** Encontra a raiz da delegação recusando ciclos de referência. */
  private Long rootId(ProcessRun run) {
    Set<Long> seen = new HashSet<>();
    while (run.getParentRunId() != null) {
      if (!seen.add(run.getId()))
        throw new IllegalStateException("Ciclo inválido na delegação do processo.");
      run = runs.findById(run.getParentRunId()).orElseThrow();
    }
    return run.getId();
  }

  /** Respeita pausa de qualquer processo de origem, inclusive em delegações aninhadas. */
  private boolean ancestorPaused(ProcessRun run) {
    Set<Long> seen = new HashSet<>();
    while (run.getParentRunId() != null) {
      if (!seen.add(run.getId()))
        throw new IllegalStateException("Ciclo inválido na delegação do processo.");
      run = runs.findById(run.getParentRunId()).orElseThrow();
      if (Set.of("PAUSING", "PAUSED", "ERROR", "COMPLETED", "CLOSED").contains(run.getStatus()))
        return true;
    }
    return false;
  }

  /** Aguarda trabalho real, permitindo pausar um ciclo que espera apenas entrada do operador. */
  private boolean inFlight(ProcessRun run, Set<Long> seen) {
    if (!seen.add(run.getId())) throw new IllegalStateException("Ciclo inválido de subprocessos.");
    var snapshot = context.read(run, false);
    updateCounts(run, snapshot);
    return snapshot.activities().stream().anyMatch(a -> activityInFlight(run, a))
        || runs.findAllByParentRunId(run.getId()).stream().anyMatch(child -> inFlight(child, seen));
  }

  /** Preserva tarefas reais em curso sem confundir o estado geral do ciclo com uma execução. */
  private boolean activityInFlight(
      ProcessRun run, ProductProcessActivityExecutionGroupResponse activity) {
    if (!ACTIVE_TASK.contains(activity.operationalState())) return false;
    if (activity.tasks().stream().anyMatch(task -> ACTIVE_TASK.contains(task.status())))
      return true;
    return !Objects.equals(run.getCurrentActivityId(), activity.activityId())
        || !guidance.awaitingInput(run);
  }

  /** Registra apenas alterações de estado e causa, mantendo o diário legível durante esperas. */
  private void transition(ProcessRun run, String status, String reason, String type) {
    reason =
        reason == null || reason.isBlank()
            ? "Aguardando as condições de execução da atividade."
            : reason;
    if (!Objects.equals(run.getStatus(), status) || !Objects.equals(run.getReason(), reason)) {
      run.setStatus(status);
      run.setReason(reason);
      run.setUpdatedAt(Instant.now());
      event(
          run,
          type,
          reason,
          null,
          Map.of(
              "completedActivities",
              run.getCompletedActivities(),
              "remainingActivities",
              run.getRemainingActivities()));
    }
  }

  /** Persiste decisão e entradas/saídas resumidas na mesma transação do disparo. */
  private void event(
      ProcessRun run, String type, String message, String actionKey, Object details) {
    var event = new ProcessRunEvent();
    event.setRunId(run.getId());
    event.setEventType(type);
    event.setStatus(run.getStatus());
    event.setActivityId(run.getCurrentActivityId());
    event.setMessage(message);
    event.setActionKey(actionKey);
    event.setCreatedAt(Instant.now());
    try {
      event.setDetailsJson(json.writeValueAsString(details));
    } catch (Exception ex) {
      log.error("Falha ao registrar decisão. runId={} eventType={}", run.getId(), type, ex);
      throw new IllegalStateException("Falha no diário do processo.", ex);
    }
    events.save(event);
  }

  /** Expõe estado persistido e disponibilidade explícita dos comandos, sem inferência na tela. */
  private ProcessRunResponse response(ProcessRun run) {
    return response(run, null);
  }

  /** Expõe provas e pendência humana atual sem gravar durante leitura nem disparar nova tarefa. */
  private ProcessRunResponse response(
      ProcessRun run, ProductProcessActivityExecutionHistoryResponse readiness) {
    boolean persisted = run.getId() != null;
    boolean revalidation =
        readiness != null && "COMPLETED".equals(run.getStatus()) && !readiness.objectiveAchieved();
    int total = readiness == null ? run.getTotalActivities() : readiness.selectedActivityCount();
    int completed =
        readiness == null ? run.getCompletedActivities() : readiness.completedActivityCount();
    int remaining =
        readiness == null ? run.getRemainingActivities() : readiness.remainingActivityCount();
    int omitted =
        readiness == null ? run.getOmittedActivities() : Math.max(0, total - completed - remaining);
    int applicable = total - omitted;
    var userAction = guidance.resolve(run);
    return new ProcessRunResponse(
        run.getId(),
        run.getProductId(),
        run.getProcessDefinitionId(),
        run.getChainDefinitionId(),
        run.getLearningCycleId(),
        run.getSourceReference(),
        revalidation
            ? "REVALIDATION_REQUIRED"
            : userAction != null ? "WAITING_HUMAN" : run.getStatus(),
        revalidation
            ? "As provas atuais já não comprovam todos os objetivos. Retome o processo para revalidar; as conclusões anteriores permanecem no histórico."
            : userAction != null ? userAction.reason() : run.getReason(),
        run.getCurrentActivityId(),
        run.getCurrentActivityName(),
        userAction != null ? userAction.responsible() : run.getCurrentOwnerName(),
        run.getCurrentSequence(),
        total,
        completed,
        remaining,
        omitted,
        applicable <= 0 ? 0 : Math.min(100, completed * 100 / applicable),
        run.getKnownCostUsd(),
        run.getCostCoverage(),
        !persisted && "READY".equals(run.getStatus()),
        persisted && !Set.of("PAUSING", "PAUSED", "COMPLETED", "CLOSED").contains(run.getStatus()),
        persisted
            && (revalidation
                || Set.of("PAUSING", "PAUSED", "BLOCKED", "ERROR", "WAITING_INPUT")
                    .contains(run.getStatus())),
        true,
        run.getChildRunId(),
        run.getNavigationUrl(),
        persisted ? run.getCreatedAt() : null,
        persisted ? run.getUpdatedAt() : null,
        persisted ? run.getLastReconciledAt() : null,
        run.getFinishedAt(),
        run.getRevision(),
        navigation.parents(run),
        navigation.children(run),
        userAction);
  }

  /** Gera identidade compacta com todos os limites de segregação, inclusive ciclo ausente. */
  private String scope(Long productId, Long processId, ProcessRunCommand command) {
    if (command == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contexto obrigatório.");
    return hash(
        productId
            + "|"
            + processId
            + "|"
            + command.chainId()
            + "|"
            + command.learningCycleId()
            + "|"
            + command.sourceReference());
  }

  /** Produz impressão SHA-256 sem persistir conteúdo comercial ou credenciais no identificador. */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("SHA-256 indisponível para identidade do processo", ex);
      throw new IllegalStateException(ex);
    }
  }
}
