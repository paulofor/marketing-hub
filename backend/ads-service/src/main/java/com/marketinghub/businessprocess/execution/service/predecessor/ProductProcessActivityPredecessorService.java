package com.marketinghub.businessprocess.execution.service.predecessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: validar a ordem do grafo BPM antes de comandos backend ou decisões humanas. */
@Service
@Slf4j
public class ProductProcessActivityPredecessorService {
  private final AgentTaskRepository taskRepository;
  private final BusinessProcessActivityInstanceRepository activityInstanceRepository;
  private final ObjectMapper objectMapper;

  /** Configura as fontes persistidas e o leitor do grafo usado para validar predecessoras. */
  public ProductProcessActivityPredecessorService(
      AgentTaskRepository taskRepository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      ObjectMapper objectMapper) {
    this.taskRepository = taskRepository;
    this.activityInstanceRepository = activityInstanceRepository;
    this.objectMapper = objectMapper;
  }

  /** Confirma que todas as atividades anteriores do caminho vigente atingiram o objetivo. */
  @Transactional(readOnly = true)
  public ProductProcessActivityPredecessorReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      String sourceReference) {
    if (process == null
        || process.getId() == null
        || activity == null
        || activity.getActivityId() == null
        || sourceReference == null
        || sourceReference.isBlank()) {
      return new ProductProcessActivityPredecessorReadiness(
          false, "A referência operacional da atividade ainda não foi definida.");
    }
    try {
      Graph graph = graph(process);
      Set<String> predecessors = graph.taskPredecessors(activity.getActivityId());
      if (predecessors.isEmpty()) {
        return new ProductProcessActivityPredecessorReadiness(
            true, "A atividade é o primeiro trabalho executável deste caminho.");
      }
      Map<String, BusinessProcessActivityInstance> latestInstances =
          latestInstances(process.getId(), sourceReference);
      Map<String, List<AgentTask>> tasksByActivity =
          taskRepository
              .findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
                  process.getId(), sourceReference)
              .stream()
              .filter(task -> task.getProcessActivityId() != null)
              .collect(
                  java.util.stream.Collectors.groupingBy(
                      AgentTask::getProcessActivityId,
                      LinkedHashMap::new,
                      java.util.stream.Collectors.toList()));
      for (String predecessorId : predecessors) {
        if (!completed(latestInstances.get(predecessorId), tasksByActivity.get(predecessorId))) {
          return new ProductProcessActivityPredecessorReadiness(
              false,
              "Conclua primeiro a atividade "
                  + graph.label(predecessorId)
                  + ". O backend preservou a ordem do processo.");
        }
      }
      return new ProductProcessActivityPredecessorReadiness(
          true, "Todas as atividades predecessoras possuem conclusão comprovada.");
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao validar predecessoras do comando BPM. processDefinitionId={} activityId={} sourceReference={}",
          process.getId(),
          activity.getActivityId(),
          sourceReference,
          ex);
      return new ProductProcessActivityPredecessorReadiness(
          false, "Não foi possível confirmar a ordem persistida desta atividade.");
    }
  }

  /** Mantém a ocorrência mais recente de cada atividade na referência operacional consultada. */
  private Map<String, BusinessProcessActivityInstance> latestInstances(
      Long processDefinitionId, String sourceReference) {
    Map<String, BusinessProcessActivityInstance> latest = new LinkedHashMap<>();
    activityInstanceRepository
        .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
            processDefinitionId, sourceReference)
        .forEach(
            instance ->
                latest.merge(
                    instance.getActivityDefinition().getActivityId(),
                    instance,
                    (current, replacement) ->
                        replacement.getOccurrenceNumber() >= current.getOccurrenceNumber()
                            ? replacement
                            : current));
    return latest;
  }

  /** Considera a instância autoridade e usa tarefas apenas para compatibilidade histórica. */
  private boolean completed(
      BusinessProcessActivityInstance instance, List<AgentTask> historicalTasks) {
    if (instance != null) {
      return "COMPLETED".equals(instance.getStatus()) && instance.isObjectiveAchieved();
    }
    if (historicalTasks == null || historicalTasks.isEmpty()) {
      return false;
    }
    Map<String, AgentTask> latestByOwner = new HashMap<>();
    historicalTasks.forEach(
        task ->
            latestByOwner.merge(
                task.getAssignedAgent().getAgentKey(),
                task,
                (current, replacement) ->
                    replacement.getId() > current.getId() ? replacement : current));
    return !latestByOwner.isEmpty()
        && latestByOwner.values().stream().allMatch(task -> "COMPLETED".equals(task.getStatus()));
  }

  /** Interpreta o diagrama persistido e prepara a navegação segura até as predecessoras. */
  private Graph graph(BusinessProcessDefinition process) {
    try {
      JsonNode diagram = objectMapper.readTree(process.getDiagramJson());
      Map<String, Node> nodes = new LinkedHashMap<>();
      for (JsonNode node : diagram.path("nodes")) {
        String id = node.path("id").asText();
        nodes.put(id, new Node(id, node.path("type").asText(), node.path("label").asText(id)));
      }
      Map<String, List<String>> incoming = new LinkedHashMap<>();
      Map<String, List<String>> outgoing = new LinkedHashMap<>();
      JsonNode connections =
          diagram.path("flows").isArray() ? diagram.path("flows") : diagram.path("edges");
      for (JsonNode edge : connections) {
        String source =
            edge.hasNonNull("from") ? edge.path("from").asText() : edge.path("source").asText();
        String target =
            edge.hasNonNull("to") ? edge.path("to").asText() : edge.path("target").asText();
        incoming.computeIfAbsent(target, ignored -> new ArrayList<>()).add(source);
        outgoing.computeIfAbsent(source, ignored -> new ArrayList<>()).add(target);
      }
      return new Graph(nodes, incoming, shortestDistances(nodes, outgoing));
    } catch (Exception ex) {
      log.error(
          "Falha ao interpretar o diagrama BPM para validar predecessoras. processDefinitionId={}",
          process.getId(),
          ex);
      throw new IllegalStateException("Diagrama BPM inválido para validar predecessoras.", ex);
    }
  }

  /** Calcula a menor distância desde o início para ignorar arestas de retorno de retrabalho. */
  private Map<String, Integer> shortestDistances(
      Map<String, Node> nodes, Map<String, List<String>> outgoing) {
    Map<String, Integer> distances = new HashMap<>();
    ArrayDeque<String> queue = new ArrayDeque<>();
    nodes.values().stream()
        .filter(node -> "START".equals(node.type()))
        .map(Node::id)
        .forEach(
            start -> {
              distances.put(start, 0);
              queue.add(start);
            });
    while (!queue.isEmpty()) {
      String current = queue.removeFirst();
      int nextDistance = distances.get(current) + 1;
      for (String target : outgoing.getOrDefault(current, List.of())) {
        Integer previous = distances.get(target);
        if (previous == null || nextDistance < previous) {
          distances.put(target, nextDistance);
          queue.addLast(target);
        }
      }
    }
    return distances;
  }

  /** Nó mínimo necessário para preservar tipo e rótulo do grafo. */
  private record Node(String id, String type, String label) {}

  /** Grafo imutável usado somente para localizar atividades anteriores relevantes. */
  private record Graph(
      Map<String, Node> nodes,
      Map<String, List<String>> incoming,
      Map<String, Integer> distanceFromStart) {

    /**
     * Localiza todas as atividades anteriores atravessando gateways sem seguir laços de retorno.
     */
    private Set<String> taskPredecessors(String activityId) {
      if (!nodes.containsKey(activityId)) {
        throw new IllegalArgumentException("Atividade não encontrada no diagrama publicado.");
      }
      Set<String> predecessors = new LinkedHashSet<>();
      Set<String> visited = new HashSet<>();
      ArrayDeque<String> queue = new ArrayDeque<>();
      queue.add(activityId);
      while (!queue.isEmpty()) {
        String current = queue.removeFirst();
        for (String source : incoming.getOrDefault(current, List.of())) {
          if (distanceFromStart.getOrDefault(source, Integer.MAX_VALUE)
              >= distanceFromStart.getOrDefault(current, Integer.MAX_VALUE)) {
            continue;
          }
          if (!visited.add(source)) {
            continue;
          }
          Node node = nodes.get(source);
          if (node != null && "TASK".equals(node.type())) {
            predecessors.add(source);
          } else {
            queue.addLast(source);
          }
        }
      }
      return predecessors;
    }

    /** Retorna o rótulo de negócio da atividade para orientar a correção. */
    private String label(String activityId) {
      return Objects.requireNonNullElse(
              nodes.get(activityId), new Node(activityId, "TASK", activityId))
          .label();
    }
  }
}
