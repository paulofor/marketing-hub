package com.marketinghub.product.service.valuechainposition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.product.ProductProcessPeriod;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductProcessPeriodRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: consolidar tempo e custo dos processos do produto a partir das trilhas
 * oficiais.
 */
@Component
@Slf4j
public class ProductStageMeasurementResolver {
  private static final Set<String> ACTIVE_TASK_STATUSES =
      Set.of("PENDING", "IN_PROGRESS", "BLOCKED");
  private static final Set<String> CREATIVE_APPROVAL_ACTIVITIES =
      Set.of("route", "produce", "customer", "commercial");
  private static final Set<String> LANDING_APPROVAL_ACTIVITIES =
      Set.of("html", "customer", "commercial");

  private final ProductProcessPeriodRepository periodRepository;
  private final CommercialPlanRepository commercialPlanRepository;
  private final AgentTaskRepository taskRepository;
  private final StudioCostLedgerEntryRepository studioLedgerRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Configura as trilhas de permanência, tarefas, custos externos e o relógio UTC. */
  @Autowired
  public ProductStageMeasurementResolver(
      ProductProcessPeriodRepository periodRepository,
      CommercialPlanRepository commercialPlanRepository,
      AgentTaskRepository taskRepository,
      StudioCostLedgerEntryRepository studioLedgerRepository,
      ObjectMapper objectMapper) {
    this(
        periodRepository,
        commercialPlanRepository,
        taskRepository,
        studioLedgerRepository,
        objectMapper,
        Clock.systemUTC());
  }

  /** Permite testar consolidações temporais com um instante fixo. */
  ProductStageMeasurementResolver(
      ProductProcessPeriodRepository periodRepository,
      CommercialPlanRepository commercialPlanRepository,
      AgentTaskRepository taskRepository,
      StudioCostLedgerEntryRepository studioLedgerRepository,
      ObjectMapper objectMapper,
      Clock clock) {
    this.periodRepository = periodRepository;
    this.commercialPlanRepository = commercialPlanRepository;
    this.taskRepository = taskRepository;
    this.studioLedgerRepository = studioLedgerRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Consolida o histórico conhecido dos macroprocessos até a posição comercial atual. */
  public List<ProductStageMeasurementResponse> resolveProcessMeasurements(
      Product product,
      List<BusinessProcessChainItem> orderedItems,
      BusinessProcessDefinition currentProcess) {
    MeasurementContext context = context(product);
    List<ProductProcessPeriod> periods =
        periodRepository.findByProductIdOrderByEnteredAtAscIdAsc(product.getId());
    List<ProductStageMeasurementResponse> measurements = new ArrayList<>();
    for (BusinessProcessChainItem item : orderedItems) {
      BusinessProcessDefinition process = item.getProcessDefinition();
      List<ProductProcessPeriod> matchingPeriods =
          periods.stream()
              .filter(period -> process.getProcessCode().equals(period.getProcessCodeSnapshot()))
              .toList();
      if (!matchingPeriods.isEmpty()) {
        matchingPeriods.forEach(
            period ->
                measurements.add(
                    measureProcessPeriod(
                        period, process, String.valueOf(item.getSequenceNumber()), context)));
        continue;
      }
      List<AgentTask> matchingTasks = processTasks(context.tasks(), process);
      if (!matchingTasks.isEmpty()) {
        measurements.add(
            measureDerivedProcess(
                process,
                item.getSequenceNumber(),
                currentProcess,
                matchingTasks,
                orderedItems,
                periods,
                context));
      } else if (process.getId().equals(currentProcess.getId())) {
        measurements.add(
            noEvidenceMeasurement(
                "PROCESS", String.valueOf(item.getSequenceNumber()), process, "CURRENT"));
      }
    }
    return measurements;
  }

  /** Consolida subprocessos que já possuem execução persistida, incluindo o atual. */
  public List<ProductStageMeasurementResponse> resolveSubprocessMeasurements(
      Product product,
      List<BusinessProcessDefinition> subprocesses,
      BusinessProcessDefinition currentSubprocess) {
    return resolveSubprocessMeasurements(product, subprocesses, currentSubprocess, null);
  }

  /** Consolida subprocessos usando a numeração hierárquica do processo pai. */
  public List<ProductStageMeasurementResponse> resolveSubprocessMeasurements(
      Product product,
      List<BusinessProcessDefinition> subprocesses,
      BusinessProcessDefinition currentSubprocess,
      Integer parentSequenceNumber) {
    return resolveSubprocessMeasurements(
        product, subprocesses, currentSubprocess, parentSequenceNumber, false);
  }

  /** Consolida subprocessos e distingue o próximo estágio pronto daquele que já possui execução. */
  public List<ProductStageMeasurementResponse> resolveSubprocessMeasurements(
      Product product,
      List<BusinessProcessDefinition> subprocesses,
      BusinessProcessDefinition currentSubprocess,
      Integer parentSequenceNumber,
      boolean currentSubprocessAwaitingFirstExecution) {
    MeasurementContext context = context(product);
    List<ProductStageMeasurementResponse> measurements = new ArrayList<>();
    for (int index = 0; index < subprocesses.size(); index++) {
      BusinessProcessDefinition subprocess = subprocesses.get(index);
      String sequenceLabel =
          parentSequenceNumber == null ? null : parentSequenceNumber + "." + (index + 1);
      List<AgentTask> matchingTasks = subprocessTasks(context.tasks(), subprocess);
      if (matchingTasks.isEmpty()) {
        if (currentSubprocess != null && subprocess.getId().equals(currentSubprocess.getId())) {
          measurements.add(
              noEvidenceMeasurement(
                  "SUBPROCESS",
                  sequenceLabel,
                  subprocess,
                  currentSubprocessAwaitingFirstExecution ? "PLANNED" : "CURRENT"));
        }
        continue;
      }
      Instant enteredAt = firstCreatedAt(matchingTasks);
      Instant nextEntry = nextSubprocessEntry(context.tasks(), subprocesses, index, enteredAt);
      boolean active = matchingTasks.stream().anyMatch(this::isActive);
      boolean current =
          currentSubprocess != null && subprocess.getId().equals(currentSubprocess.getId());
      boolean transitioned = nextEntry != null;
      Instant objectiveAchievedAt =
          persistedSubprocessObjectiveAchievedAt(subprocess, matchingTasks);
      boolean objectiveAchieved = transitioned || objectiveAchievedAt != null;
      Instant exitedAt = objectiveAchievedAt != null ? objectiveAchievedAt : nextEntry;
      String trackingStatus =
          objectiveAchieved ? "COMPLETED" : current || active ? "CURRENT" : "RECORDED";
      String exitEvidence =
          objectiveAchievedAt != null
              ? "SUBPROCESS_OBJECTIVE_ACHIEVED"
              : transitioned ? "NEXT_SUBPROCESS_STARTED" : null;
      measurements.add(
          measurement(
              "SUBPROCESS",
              sequenceLabel,
              trackingStatus,
              subprocess,
              enteredAt,
              "FIRST_SUBPROCESS_TASK",
              exitedAt,
              exitEvidence,
              objectiveAchieved,
              matchingTasks,
              ledgerWithin(context.ledger(), enteredAt, exitedAt)));
    }
    return measurements;
  }

  /** Confirma o objetivo persistido de um subprocesso sem inferir avanço por simples status. */
  public boolean objectiveAchieved(Product product, BusinessProcessDefinition subprocess) {
    MeasurementContext context = context(product);
    return persistedSubprocessObjectiveAchievedAt(
            subprocess, subprocessTasks(context.tasks(), subprocess))
        != null;
  }

  /**
   * Obtém o horário auditável em que todas as atividades criativas obrigatórias foram concluídas.
   */
  private Instant persistedSubprocessObjectiveAchievedAt(
      BusinessProcessDefinition subprocess, List<AgentTask> matchingTasks) {
    if ("landing-page-generation".equals(subprocess.getProcessCode())) {
      return approvedLandingAchievedAt(matchingTasks);
    }
    if (!"creative-production-approval".equals(subprocess.getProcessCode())) return null;
    Set<String> packageIds = new java.util.HashSet<>();
    List<Instant> completionTimes = new ArrayList<>();
    for (String activityId : CREATIVE_APPROVAL_ACTIVITIES) {
      AgentTask latest = latestActivityTask(matchingTasks, activityId);
      if (latest == null || !"COMPLETED".equals(latest.getStatus())) return null;
      String packageId = acceptedCreativePackageId(latest);
      Instant completionTime =
          latest.getDeliveredAt() != null ? latest.getDeliveredAt() : latest.getUpdatedAt();
      if (packageId == null || completionTime == null) return null;
      packageIds.add(packageId);
      completionTimes.add(completionTime);
    }
    return packageIds.size() == 1
        ? completionTimes.stream().max(Comparator.naturalOrder()).orElse(null)
        : null;
  }

  /**
   * Reconhece a landing aprovada somente quando Dédalo, Quality Review, Psique e Têmis fecharam a
   * mesma execução; publicação humana permanece fora deste objetivo.
   */
  private Instant approvedLandingAchievedAt(List<AgentTask> matchingTasks) {
    Map<String, List<AgentTask>> byExecution =
        matchingTasks.stream()
            .filter(task -> task.getSourceReference() != null)
            .collect(java.util.stream.Collectors.groupingBy(AgentTask::getSourceReference));
    return byExecution.values().stream()
        .map(this::approvedLandingExecutionAchievedAt)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }

  /** Valida decisões e evidências estruturadas dos três gates da mesma execução de landing. */
  private Instant approvedLandingExecutionAchievedAt(List<AgentTask> tasks) {
    Map<String, AgentTask> latestByActivity = new LinkedHashMap<>();
    tasks.stream()
        .filter(task -> LANDING_APPROVAL_ACTIVITIES.contains(task.getProcessActivityId()))
        .sorted(
            Comparator.comparing(
                    AgentTask::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(AgentTask::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
        .forEach(task -> latestByActivity.put(task.getProcessActivityId(), task));
    if (!LANDING_APPROVAL_ACTIVITIES.stream()
        .allMatch(
            activityId ->
                latestByActivity.containsKey(activityId)
                    && "COMPLETED".equals(latestByActivity.get(activityId).getStatus()))) {
      return null;
    }
    if (!approvedLandingHtml(latestByActivity.get("html"))
        || !approvedDecision(latestByActivity.get("customer"))
        || !approvedDecision(latestByActivity.get("commercial"))) {
      return null;
    }
    return latestByActivity.values().stream()
        .map(task -> task.getDeliveredAt() != null ? task.getDeliveredAt() : task.getUpdatedAt())
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }

  /** Exige a aprovação independente, o HTML final e o checkout preservado por Dédalo. */
  private boolean approvedLandingHtml(AgentTask task) {
    try {
      JsonNode evidence = objectMapper.readTree(task.getEvidenceJson());
      return "APPROVE_FOR_PUBLICATION".equals(evidence.path("approvalRecommendation").asText())
          && !evidence.path("landingHtml").asText().isBlank()
          && !evidence.path("checkoutUrl").asText().isBlank();
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      log.warn(
          "Evidência da landing inválida ao medir objetivo do subprocesso. taskId={}",
          task.getId(),
          ex);
      return false;
    }
  }

  /** Aceita somente parecer funcional explícito e aprovado de Psique ou Têmis. */
  private boolean approvedDecision(AgentTask task) {
    try {
      return "APPROVED"
          .equals(objectMapper.readTree(task.getResultJson()).path("decision").asText());
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      log.warn(
          "Parecer de landing inválido ao medir objetivo do subprocesso. taskId={}",
          task.getId(),
          ex);
      return false;
    }
  }

  /** Seleciona a execução mais recente de uma atividade específica do subprocesso. */
  private AgentTask latestActivityTask(List<AgentTask> tasks, String activityId) {
    return tasks.stream()
        .filter(task -> activityId.equals(task.getProcessActivityId()))
        .max(
            Comparator.comparing(
                    AgentTask::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(AgentTask::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null);
  }

  /** Valida a evidência humana, os ativos e a ausência de publicação ou gasto do mesmo pacote. */
  private String acceptedCreativePackageId(AgentTask task) {
    try {
      JsonNode evidence = objectMapper.readTree(task.getEvidenceJson());
      String packageId = evidence.path("creativePackageId").asText();
      boolean accepted =
          evidence.path("importedByHuman").asBoolean(false)
              && evidence.has("published")
              && !evidence.path("published").asBoolean(true)
              && evidence.has("externalMediaSpendUsd")
              && evidence.path("externalMediaSpendUsd").decimalValue().signum() == 0
              && evidence.path("assets").isArray()
              && !evidence.path("assets").isEmpty()
              && packageId.matches("[0-9a-f]{64}");
      return accepted ? packageId : null;
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      log.warn(
          "Evidência criativa inválida ao medir objetivo do subprocesso. taskId={} processCode={}",
          task.getId(),
          task.getProcessDefinition() == null ? null : task.getProcessDefinition().getProcessCode(),
          ex);
      return null;
    }
  }

  /** Mede um período explícito registrado pelo backend nas transições comerciais. */
  private ProductStageMeasurementResponse measureProcessPeriod(
      ProductProcessPeriod period,
      BusinessProcessDefinition process,
      String sequenceLabel,
      MeasurementContext context) {
    List<AgentTask> matchingTasks = processTasks(context.tasks(), process);
    Instant enteredAt = period.getEnteredAt();
    String entryEvidence = period.getEntryEvidence();
    if ("BACKFILLED_PRODUCT_UPDATE".equals(entryEvidence)) {
      Instant firstTaskAt = firstCreatedAt(matchingTasks);
      if (firstTaskAt != null && firstTaskAt.isBefore(enteredAt)) {
        enteredAt = firstTaskAt;
        entryEvidence = "BACKFILLED_EXECUTION_HISTORY";
      }
    }
    Instant exitedAt = period.getExitedAt();
    Instant effectiveEnteredAt = enteredAt;
    List<AgentTask> tasksWithin =
        matchingTasks.stream()
            .filter(task -> within(task.getCreatedAt(), effectiveEnteredAt, exitedAt))
            .toList();
    return measurement(
        "PROCESS",
        sequenceLabel,
        exitedAt == null ? "CURRENT" : "COMPLETED",
        process,
        enteredAt,
        entryEvidence,
        exitedAt,
        period.getExitEvidence(),
        period.isObjectiveAchieved(),
        tasksWithin,
        ledgerWithin(context.ledger(), enteredAt, exitedAt));
  }

  /** Mede um processo legado usando tarefas e a primeira evidência do processo seguinte. */
  private ProductStageMeasurementResponse measureDerivedProcess(
      BusinessProcessDefinition process,
      int sequenceNumber,
      BusinessProcessDefinition currentProcess,
      List<AgentTask> matchingTasks,
      List<BusinessProcessChainItem> orderedItems,
      List<ProductProcessPeriod> periods,
      MeasurementContext context) {
    Instant enteredAt = firstCreatedAt(matchingTasks);
    boolean current = process.getId().equals(currentProcess.getId());
    Instant nextTaskEntry =
        current
            ? null
            : orderedItems.stream()
                .filter(item -> item.getSequenceNumber() > sequenceNumber)
                .map(BusinessProcessChainItem::getProcessDefinition)
                .map(next -> firstCreatedAt(processTasks(context.tasks(), next)))
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    Instant nextPeriodEntry =
        current
            ? null
            : periods.stream()
                .filter(period -> period.getSequenceNumber() > sequenceNumber)
                .map(ProductProcessPeriod::getEnteredAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    Instant exitedAt = earliest(nextTaskEntry, nextPeriodEntry);
    String trackingStatus = current ? "CURRENT" : exitedAt == null ? "RECORDED" : "COMPLETED";
    return measurement(
        "PROCESS",
        String.valueOf(sequenceNumber),
        trackingStatus,
        process,
        enteredAt,
        "FIRST_PROCESS_EXECUTION",
        exitedAt,
        exitedAt == null
            ? null
            : exitedAt.equals(nextTaskEntry)
                ? "NEXT_PROCESS_EXECUTION_STARTED"
                : "NEXT_PROCESS_PERIOD_STARTED",
        exitedAt != null,
        matchingTasks,
        ledgerWithin(context.ledger(), enteredAt, exitedAt));
  }

  /** Monta uma medição vazia sem fabricar data ou custo quando não há evidência. */
  private ProductStageMeasurementResponse noEvidenceMeasurement(
      String stageType, String sequenceLabel, BusinessProcessDefinition process, String status) {
    return new ProductStageMeasurementResponse(
        stageType,
        sequenceLabel,
        status,
        process.getId(),
        process.getProcessCode(),
        process.getName(),
        null,
        "NOT_RECORDED",
        null,
        null,
        false,
        null,
        BigDecimal.ZERO.setScale(8),
        "NO_EXECUTIONS",
        0,
        0);
  }

  /** Consolida datas, dias corridos e cobertura financeira da etapa. */
  private ProductStageMeasurementResponse measurement(
      String stageType,
      String sequenceLabel,
      String trackingStatus,
      BusinessProcessDefinition process,
      Instant enteredAt,
      String entryEvidence,
      Instant exitedAt,
      String exitEvidence,
      boolean objectiveAchieved,
      List<AgentTask> tasks,
      List<StudioCostLedgerEntry> ledger) {
    BigDecimal knownCost = BigDecimal.ZERO;
    int costedExecutions = 0;
    int uncostedExecutions = 0;
    for (AgentTask task : tasks) {
      if (task.getEstimatedCostUsd() != null) {
        knownCost = knownCost.add(task.getEstimatedCostUsd());
        costedExecutions++;
      } else {
        uncostedExecutions++;
      }
    }
    for (StudioCostLedgerEntry entry : ledger) {
      BigDecimal cost =
          entry.getProviderCostUsd() != null
              ? entry.getProviderCostUsd()
              : entry.getEstimatedCostUsd();
      if (cost != null) {
        knownCost = knownCost.add(cost);
        costedExecutions++;
      } else if (entry.getCostEvidence() != null
          && entry.getCostEvidence().contains("ASSUMED_ZERO")) {
        costedExecutions++;
      } else {
        uncostedExecutions++;
      }
    }
    String coverage = costCoverage(costedExecutions, uncostedExecutions);
    Instant end = exitedAt == null ? Instant.now(clock) : exitedAt;
    Long elapsedDays =
        enteredAt == null || end.isBefore(enteredAt)
            ? null
            : Duration.between(enteredAt, end).toDays();
    return new ProductStageMeasurementResponse(
        stageType,
        sequenceLabel,
        trackingStatus,
        process.getId(),
        process.getProcessCode(),
        process.getName(),
        enteredAt,
        entryEvidence,
        exitedAt,
        exitEvidence,
        objectiveAchieved,
        elapsedDays,
        knownCost.setScale(8, RoundingMode.HALF_UP),
        coverage,
        costedExecutions,
        uncostedExecutions);
  }

  /** Classifica se o subtotal conhecido cobre todas as execuções da etapa. */
  private String costCoverage(int costedExecutions, int uncostedExecutions) {
    if (costedExecutions == 0 && uncostedExecutions == 0) return "NO_EXECUTIONS";
    if (costedExecutions == 0) return "NOT_REPORTED";
    return uncostedExecutions == 0 ? "COMPLETE" : "PARTIAL";
  }

  /** Carrega uma única vez tarefas e ledger pertencentes aos planos do produto. */
  private MeasurementContext context(Product product) {
    List<CommercialPlan> plans = commercialPlanRepository.findByProductId(product.getId());
    Map<Long, AgentTask> tasks = new LinkedHashMap<>();
    for (CommercialPlan plan : plans) {
      taskRepository
          .findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
              "commercial-plan:" + plan.getId() + "@")
          .forEach(task -> tasks.put(task.getId(), task));
    }
    Map<Long, StudioCostLedgerEntry> ledger = new LinkedHashMap<>();
    studioLedgerRepository
        .findByProductIdOrderByCreatedAtAsc(product.getId())
        .forEach(entry -> ledger.put(entry.getId(), entry));
    if (!plans.isEmpty()) {
      studioLedgerRepository
          .findByCommercialPlanIdInOrderByCreatedAtAsc(
              plans.stream().map(CommercialPlan::getId).toList())
          .forEach(entry -> ledger.put(entry.getId(), entry));
    }
    return new MeasurementContext(List.copyOf(tasks.values()), List.copyOf(ledger.values()));
  }

  /** Seleciona tarefas do macroprocesso e de todos os seus subprocessos. */
  private List<AgentTask> processTasks(List<AgentTask> tasks, BusinessProcessDefinition process) {
    return tasks.stream()
        .filter(task -> task.getProcessDefinition() != null)
        .filter(
            task ->
                process.getProcessCode().equals(task.getProcessDefinition().getProcessCode())
                    || process
                        .getProcessCode()
                        .equals(task.getProcessDefinition().getParentProcessCode()))
        .sorted(Comparator.comparing(AgentTask::getCreatedAt).thenComparing(AgentTask::getId))
        .toList();
  }

  /** Seleciona somente tarefas vinculadas ao código do subprocesso informado. */
  private List<AgentTask> subprocessTasks(
      List<AgentTask> tasks, BusinessProcessDefinition subprocess) {
    return tasks.stream()
        .filter(task -> task.getProcessDefinition() != null)
        .filter(
            task ->
                subprocess.getProcessCode().equals(task.getProcessDefinition().getProcessCode()))
        .sorted(Comparator.comparing(AgentTask::getCreatedAt).thenComparing(AgentTask::getId))
        .toList();
  }

  /** Localiza a primeira tarefa do próximo subprocesso que comprova a transição. */
  private Instant nextSubprocessEntry(
      List<AgentTask> tasks,
      List<BusinessProcessDefinition> subprocesses,
      int currentIndex,
      Instant enteredAt) {
    for (int index = currentIndex + 1; index < subprocesses.size(); index++) {
      Instant candidate = firstCreatedAt(subprocessTasks(tasks, subprocesses.get(index)));
      if (candidate != null && (enteredAt == null || !candidate.isBefore(enteredAt)))
        return candidate;
    }
    return null;
  }

  /** Escolhe a primeira evidência downstream sem transformar atualização em conclusão. */
  private Instant earliest(Instant first, Instant second) {
    if (first == null) return second;
    if (second == null) return first;
    return first.isBefore(second) ? first : second;
  }

  /** Localiza o primeiro instante de criação disponível. */
  private Instant firstCreatedAt(List<AgentTask> tasks) {
    return tasks.stream()
        .map(AgentTask::getCreatedAt)
        .filter(Objects::nonNull)
        .min(Comparator.naturalOrder())
        .orElse(null);
  }

  /** Seleciona tentativas do Estúdio realizadas dentro do intervalo da etapa. */
  private List<StudioCostLedgerEntry> ledgerWithin(
      List<StudioCostLedgerEntry> entries, Instant enteredAt, Instant exitedAt) {
    if (enteredAt == null) return List.of();
    return entries.stream().filter(entry -> within(ledgerAt(entry), enteredAt, exitedAt)).toList();
  }

  /** Escolhe o instante operacional mais preciso disponível no ledger. */
  private Instant ledgerAt(StudioCostLedgerEntry entry) {
    if (entry.getStartedAt() != null) return entry.getStartedAt();
    return entry.getCreatedAt();
  }

  /** Verifica pertencimento ao intervalo fechado na entrada e aberto na saída. */
  private boolean within(Instant value, Instant enteredAt, Instant exitedAt) {
    if (value == null || enteredAt == null || value.isBefore(enteredAt)) return false;
    return exitedAt == null || value.isBefore(exitedAt);
  }

  /** Informa se uma tarefa ainda mantém o subprocesso em execução. */
  private boolean isActive(AgentTask task) {
    return ACTIVE_TASK_STATUSES.contains(task.getStatus());
  }

  /** Agrupa as execuções e custos já atribuídos ao produto. */
  private record MeasurementContext(List<AgentTask> tasks, List<StudioCostLedgerEntry> ledger) {}
}
