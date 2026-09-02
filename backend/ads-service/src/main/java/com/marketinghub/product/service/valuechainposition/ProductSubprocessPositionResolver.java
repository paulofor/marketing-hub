package com.marketinghub.product.service.valuechainposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskMeasurementSnapshot;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: resolver o progresso do produto nos subprocessos oficiais do processo atual.
 */
@Component
@Slf4j
public class ProductSubprocessPositionResolver {
  private static final String PUBLISHED_STATUS = "PUBLISHED";
  private static final Set<String> ACTIVE_TASK_STATUSES =
      Set.of("PENDING", "IN_PROGRESS", "BLOCKED");

  private final BusinessProcessDefinitionRepository processRepository;
  private final CommercialPlanRepository commercialPlanRepository;
  private final AgentTaskRepository taskRepository;
  private final ObjectMapper objectMapper;
  private final ProductStageMeasurementResolver stageMeasurementResolver;

  /** Configura as fontes persistidas usadas para localizar atividades e subprocessos. */
  public ProductSubprocessPositionResolver(
      BusinessProcessDefinitionRepository processRepository,
      CommercialPlanRepository commercialPlanRepository,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper) {
    this(processRepository, commercialPlanRepository, taskRepository, objectMapper, null);
  }

  /** Configura o resolvedor produtivo com a consolidação temporal e financeira. */
  @Autowired
  public ProductSubprocessPositionResolver(
      BusinessProcessDefinitionRepository processRepository,
      CommercialPlanRepository commercialPlanRepository,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper,
      ProductStageMeasurementResolver stageMeasurementResolver) {
    this.processRepository = processRepository;
    this.commercialPlanRepository = commercialPlanRepository;
    this.taskRepository = taskRepository;
    this.objectMapper = objectMapper;
    this.stageMeasurementResolver = stageMeasurementResolver;
  }

  /** Resolve a atividade atual e os subprocessos atual e seguinte sem inferência no frontend. */
  public ProductSubprocessPositionResponse resolve(
      Product product, BusinessProcessDefinition parentProcess) {
    return resolve(product, parentProcess, null);
  }

  /** Resolve subprocessos preservando a numeração hierárquica do processo pai. */
  public ProductSubprocessPositionResponse resolve(
      Product product, BusinessProcessDefinition parentProcess, Integer parentSequenceNumber) {
    ProductStageMeasurementContext context =
        stageMeasurementResolver == null ? null : stageMeasurementResolver.loadContext(product);
    return resolve(product, parentProcess, parentSequenceNumber, context);
  }

  /** Resolve subprocessos reutilizando as evidências já carregadas para o mesmo produto. */
  ProductSubprocessPositionResponse resolve(
      Product product,
      BusinessProcessDefinition parentProcess,
      Integer parentSequenceNumber,
      ProductStageMeasurementContext context) {
    List<BusinessProcessDefinition> subprocesses = orderedSubprocesses(parentProcess);
    if (subprocesses.isEmpty()) {
      return new ProductSubprocessPositionResponse(
          "NOT_APPLICABLE",
          0,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          List.of());
    }

    List<AgentTaskMeasurementSnapshot> tasks =
        (context == null ? productTasks(product) : context.commercialPlanTasks())
            .stream().filter(this::hasProcess).toList();
    AgentTaskMeasurementSnapshot latestChildTask = latestChildTask(tasks, subprocesses);
    AgentTaskMeasurementSnapshot activeChildTask =
        latestActiveChildTask(tasks, subprocesses, latestChildTask);
    BusinessProcessDefinition current = childDefinition(activeChildTask, subprocesses);
    BusinessProcessDefinition lastRecorded = childDefinition(latestChildTask, subprocesses);
    BusinessProcessDefinition next =
        current != null
            ? nextSubprocess(current, subprocesses)
            : nextSubprocess(lastRecorded, subprocesses);
    boolean currentAwaitingFirstExecution = false;
    String activityName = activeChildTask == null ? null : activeChildTask.processActivityName();
    String status =
        activeChildTask != null ? "IN_PROGRESS" : lastRecorded != null ? "RECORDED" : "PLANNED";

    if (current == null
        && lastRecorded != null
        && stageMeasurementResolver != null
        && objectiveAchieved(product, lastRecorded, context)) {
      if (next != null) {
        current = next;
        next = nextSubprocess(current, subprocesses);
        currentAwaitingFirstExecution = true;
        status = "PLANNED";
      } else {
        activityName = nextParentActivityName(parentProcess, lastRecorded.getProcessCode());
        status = "COMPLETED";
      }
    }

    if (current == null && lastRecorded == null) {
      ParentProgress parentProgress = parentProgress(tasks, parentProcess);
      activityName = parentProgress.activityName();
      current = childByCode(parentProgress.currentSubprocessCode(), subprocesses);
      next =
          current == null
              ? childByCode(parentProgress.nextSubprocessCode(), subprocesses)
              : nextSubprocess(current, subprocesses);
      status = current == null ? "PLANNED" : "IN_PROGRESS";
    }
    return response(
        product,
        status,
        subprocesses,
        activityName,
        current,
        next,
        parentSequenceNumber,
        currentAwaitingFirstExecution,
        context);
  }

  /** Confirma o objetivo sem recarregar o histórico quando o contexto já está disponível. */
  private boolean objectiveAchieved(
      Product product,
      BusinessProcessDefinition subprocess,
      ProductStageMeasurementContext context) {
    return context == null
        ? stageMeasurementResolver.objectiveAchieved(product, subprocess)
        : stageMeasurementResolver.objectiveAchieved(context, subprocess);
  }

  /** Localiza a primeira atividade do processo pai depois do subprocesso concluído. */
  private String nextParentActivityName(
      BusinessProcessDefinition parentProcess, String completedSubprocessCode) {
    List<JsonNode> nodes = orderedNodes(parentProcess);
    boolean completedFound = false;
    for (JsonNode node : nodes) {
      if (completedSubprocessCode.equals(node.path("subprocessCode").asText(null))) {
        completedFound = true;
        continue;
      }
      if (completedFound
          && "TASK".equals(node.path("type").asText())
          && !node.hasNonNull("subprocessCode")) {
        return node.path("label").asText(null);
      }
    }
    return null;
  }

  /** Ordena subprocessos pela posição em que são delegados no diagrama do processo pai. */
  private List<BusinessProcessDefinition> orderedSubprocesses(
      BusinessProcessDefinition parentProcess) {
    Map<String, BusinessProcessDefinition> byCode =
        processRepository
            .findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
                parentProcess.getProcessCode(), PUBLISHED_STATUS)
            .stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    BusinessProcessDefinition::getProcessCode,
                    process -> process,
                    (first, ignored) -> first,
                    LinkedHashMap::new));
    return orderedNodes(parentProcess).stream()
        .map(node -> node.path("subprocessCode").asText(null))
        .filter(byCode::containsKey)
        .map(byCode::get)
        .distinct()
        .toList();
  }

  /** Localiza tarefas pré-experimento pelo produto e por seus planos comerciais auditáveis. */
  private List<AgentTaskMeasurementSnapshot> productTasks(Product product) {
    List<AgentTaskMeasurementSnapshot> tasks = new ArrayList<>();
    for (CommercialPlan plan : commercialPlanRepository.findByProductId(product.getId())) {
      tasks.addAll(
          taskRepository
              .findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
                  "commercial-plan:" + plan.getId() + "@")
              .stream()
              .map(this::snapshot)
              .toList());
    }
    tasks.addAll(
        taskRepository
            .findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
                "product:" + product.getId() + "@")
            .stream()
            .map(this::snapshot)
            .toList());
    return tasks;
  }

  /** Converte a entidade completa apenas no caminho legado sem contexto compartilhado. */
  private AgentTaskMeasurementSnapshot snapshot(AgentTask task) {
    BusinessProcessDefinition process = task.getProcessDefinition();
    return new AgentTaskMeasurementSnapshot(
        task.getId(),
        process == null ? null : process.getId(),
        process == null ? null : process.getProcessCode(),
        process == null ? null : process.getParentProcessCode(),
        task.getProcessActivityId(),
        task.getProcessActivityName(),
        task.getSourceReference(),
        task.getStatus(),
        task.getActivityInstance() == null ? null : task.getActivityInstance().getStatus(),
        task.getCreatedAt(),
        task.getUpdatedAt(),
        task.getDeliveredAt(),
        task.getResultJson(),
        task.getEvidenceJson(),
        task.getEstimatedCostUsd());
  }

  /** Seleciona a tarefa mais recente registrada em qualquer subprocesso da composição atual. */
  private AgentTaskMeasurementSnapshot latestChildTask(
      List<AgentTaskMeasurementSnapshot> tasks, List<BusinessProcessDefinition> subprocesses) {
    Set<Long> childIds =
        subprocesses.stream()
            .map(BusinessProcessDefinition::getId)
            .collect(java.util.stream.Collectors.toSet());
    return tasks.stream()
        .filter(task -> childIds.contains(task.processDefinitionId()))
        .max(
            Comparator.comparing(AgentTaskMeasurementSnapshot::updatedAt)
                .thenComparing(AgentTaskMeasurementSnapshot::id))
        .orElse(null);
  }

  /**
   * Seleciona trabalho ativo somente na execução mais recente e usa a instância BPM como
   * autoridade.
   */
  private AgentTaskMeasurementSnapshot latestActiveChildTask(
      List<AgentTaskMeasurementSnapshot> tasks,
      List<BusinessProcessDefinition> subprocesses,
      AgentTaskMeasurementSnapshot latestRecordedTask) {
    if (latestRecordedTask == null) return null;
    Set<Long> childIds =
        subprocesses.stream()
            .map(BusinessProcessDefinition::getId)
            .collect(java.util.stream.Collectors.toSet());
    return tasks.stream()
        .filter(task -> childIds.contains(task.processDefinitionId()))
        .filter(
            task -> Objects.equals(latestRecordedTask.sourceReference(), task.sourceReference()))
        .filter(this::isOperationallyActive)
        .max(
            Comparator.comparing(AgentTaskMeasurementSnapshot::updatedAt)
                .thenComparing(AgentTaskMeasurementSnapshot::id))
        .orElse(null);
  }

  /** Faz o estado consolidado da instância BPM prevalecer sobre tentativas antigas da tarefa. */
  private boolean isOperationallyActive(AgentTaskMeasurementSnapshot task) {
    String status =
        task.activityInstanceStatus() == null ? task.status() : task.activityInstanceStatus();
    return ACTIVE_TASK_STATUSES.contains(status);
  }

  /** Calcula a posição interna do processo pai a partir da última tarefa auditada. */
  private ParentProgress parentProgress(
      List<AgentTaskMeasurementSnapshot> tasks, BusinessProcessDefinition parentProcess) {
    List<JsonNode> nodes = orderedNodes(parentProcess);
    AgentTaskMeasurementSnapshot latestParentTask =
        tasks.stream()
            .filter(task -> parentProcess.getId().equals(task.processDefinitionId()))
            .max(
                Comparator.comparing(AgentTaskMeasurementSnapshot::updatedAt)
                    .thenComparing(AgentTaskMeasurementSnapshot::id))
            .orElse(null);
    int currentIndex = firstWorkNodeIndex(nodes);
    if (latestParentTask != null) {
      int recordedIndex = indexOfNode(nodes, latestParentTask.processActivityId());
      currentIndex =
          ACTIVE_TASK_STATUSES.contains(latestParentTask.status())
              ? recordedIndex
              : Math.min(recordedIndex + 1, nodes.size() - 1);
    }
    JsonNode currentNode =
        currentIndex >= 0 && currentIndex < nodes.size() ? nodes.get(currentIndex) : null;
    String currentCode =
        currentNode == null ? null : currentNode.path("subprocessCode").asText(null);
    String nextCode = nextSubprocessCode(nodes, currentIndex, currentCode != null);
    return new ParentProgress(
        currentNode == null ? null : currentNode.path("label").asText(null), currentCode, nextCode);
  }

  /** Retorna os nós do diagrama em ordem de distância do início, preservando empates declarados. */
  private List<JsonNode> orderedNodes(BusinessProcessDefinition process) {
    try {
      JsonNode diagram = objectMapper.readTree(process.getDiagramJson());
      List<JsonNode> declaredNodes = new ArrayList<>();
      diagram.path("nodes").forEach(declaredNodes::add);
      Map<String, Integer> declaredOrder = new HashMap<>();
      for (int index = 0; index < declaredNodes.size(); index++) {
        declaredOrder.put(declaredNodes.get(index).path("id").asText(), index);
      }
      Map<String, List<String>> outgoing = new HashMap<>();
      Map<String, Integer> incomingCount = new HashMap<>();
      diagram
          .path("flows")
          .forEach(
              flow -> {
                String from = flow.path("from").asText();
                String to = flow.path("to").asText();
                outgoing.computeIfAbsent(from, ignored -> new ArrayList<>()).add(to);
                incomingCount.merge(to, 1, Integer::sum);
              });
      Map<String, Integer> distance = new HashMap<>();
      ArrayDeque<String> queue = new ArrayDeque<>();
      declaredNodes.stream()
          .map(node -> node.path("id").asText())
          .filter(id -> incomingCount.getOrDefault(id, 0) == 0)
          .forEach(
              id -> {
                distance.put(id, 0);
                queue.add(id);
              });
      Set<String> visited = new HashSet<>();
      while (!queue.isEmpty()) {
        String source = queue.removeFirst();
        if (!visited.add(source)) continue;
        for (String target : outgoing.getOrDefault(source, List.of())) {
          distance.merge(target, distance.getOrDefault(source, 0) + 1, Math::min);
          queue.addLast(target);
        }
      }
      return declaredNodes.stream()
          .sorted(
              Comparator.<JsonNode>comparingInt(
                      node -> distance.getOrDefault(node.path("id").asText(), Integer.MAX_VALUE))
                  .thenComparingInt(node -> declaredOrder.get(node.path("id").asText())))
          .toList();
    } catch (Exception ex) {
      log.error(
          "Falha ao interpretar diagrama da cadeia do produto. processCode={} processDefinitionId={}",
          process.getProcessCode(),
          process.getId(),
          ex);
      throw new IllegalStateException(
          "Não foi possível interpretar o processo " + process.getProcessCode(), ex);
    }
  }

  /** Localiza o primeiro nó de trabalho após o início do processo. */
  private int firstWorkNodeIndex(List<JsonNode> nodes) {
    for (int index = 0; index < nodes.size(); index++) {
      if (!"START".equals(nodes.get(index).path("type").asText())) return index;
    }
    return -1;
  }

  /** Localiza um nó pelo identificador persistido na tarefa. */
  private int indexOfNode(List<JsonNode> nodes, String activityId) {
    for (int index = 0; index < nodes.size(); index++) {
      if (nodes.get(index).path("id").asText().equals(activityId)) return index;
    }
    return firstWorkNodeIndex(nodes);
  }

  /** Encontra a próxima delegação especializada depois da posição atual. */
  private String nextSubprocessCode(
      List<JsonNode> nodes, int currentIndex, boolean skipCurrentSubprocess) {
    int start = Math.max(0, currentIndex + (skipCurrentSubprocess ? 1 : 0));
    for (int index = start; index < nodes.size(); index++) {
      String code = nodes.get(index).path("subprocessCode").asText(null);
      if (code != null) return code;
    }
    return null;
  }

  /** Localiza uma definição filha pelo código canônico. */
  private BusinessProcessDefinition childByCode(
      String code, List<BusinessProcessDefinition> subprocesses) {
    if (code == null) return null;
    return subprocesses.stream()
        .filter(process -> code.equals(process.getProcessCode()))
        .findFirst()
        .orElse(null);
  }

  /** Localiza a definição vinculada à tarefa auditada. */
  private BusinessProcessDefinition childDefinition(
      AgentTaskMeasurementSnapshot task, List<BusinessProcessDefinition> subprocesses) {
    return task == null
        ? null
        : subprocesses.stream()
            .filter(process -> process.getId().equals(task.processDefinitionId()))
            .findFirst()
            .orElse(null);
  }

  /** Localiza o subprocesso que sucede a definição atual na composição oficial. */
  private BusinessProcessDefinition nextSubprocess(
      BusinessProcessDefinition current, List<BusinessProcessDefinition> subprocesses) {
    if (current == null) return subprocesses.getFirst();
    int index = subprocesses.indexOf(current);
    return index >= 0 && index + 1 < subprocesses.size() ? subprocesses.get(index + 1) : null;
  }

  /** Confirma que a tarefa possui vínculo canônico com uma definição de processo. */
  private boolean hasProcess(AgentTaskMeasurementSnapshot task) {
    return task.processDefinitionId() != null;
  }

  /** Monta o contrato enxuto usado pelos cards administrativos. */
  private ProductSubprocessPositionResponse response(
      Product product,
      String status,
      List<BusinessProcessDefinition> subprocesses,
      String activityName,
      BusinessProcessDefinition current,
      BusinessProcessDefinition next,
      Integer parentSequenceNumber,
      boolean currentAwaitingFirstExecution,
      ProductStageMeasurementContext context) {
    Integer currentSequenceNumber = current == null ? null : subprocesses.indexOf(current) + 1;
    Integer nextSequenceNumber = next == null ? null : subprocesses.indexOf(next) + 1;
    return new ProductSubprocessPositionResponse(
        status,
        subprocesses.size(),
        activityName,
        current == null ? null : current.getId(),
        currentSequenceNumber,
        current == null ? null : current.getProcessCode(),
        current == null ? null : current.getName(),
        current == null ? null : current.getOutcomeDescription(),
        next == null ? null : next.getId(),
        nextSequenceNumber,
        next == null ? null : next.getProcessCode(),
        next == null ? null : next.getName(),
        next == null ? null : next.getOutcomeDescription(),
        stageMeasurementResolver == null
            ? List.of()
            : context == null
                ? currentAwaitingFirstExecution
                    ? stageMeasurementResolver.resolveSubprocessMeasurements(
                        product, subprocesses, current, parentSequenceNumber, true)
                    : stageMeasurementResolver.resolveSubprocessMeasurements(
                        product, subprocesses, current, parentSequenceNumber)
                : stageMeasurementResolver.resolveSubprocessMeasurements(
                    context,
                    subprocesses,
                    current,
                    parentSequenceNumber,
                    currentAwaitingFirstExecution));
  }

  /** Representa a posição calculada dentro do processo pai. */
  private record ParentProgress(
      String activityName, String currentSubprocessCode, String nextSubprocessCode) {}
}
