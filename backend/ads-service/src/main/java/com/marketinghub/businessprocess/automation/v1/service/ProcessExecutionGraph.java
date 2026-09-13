package com.marketinghub.businessprocess.automation.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import java.util.*;

/** Responsabilidade: interpretar dependências e retornos do BPM sem executar atividades. */
final class ProcessExecutionGraph {
  private final Map<String, JsonNode> nodes = new LinkedHashMap<>();
  private final Map<String, Set<String>> incoming = new HashMap<>();
  private final List<String> ordered = new ArrayList<>();

  /** Valida a topologia e ordena todos os nós pelo grafo, ignorando retornos explícitos. */
  ProcessExecutionGraph(JsonNode diagram) {
    for (var node : diagram.path("nodes")) {
      String id = node.path("id").asText();
      if (id.isBlank() || nodes.putIfAbsent(id, node) != null)
        throw new IllegalStateException("O BPM contém identidade ausente ou duplicada.");
      incoming.put(id, new LinkedHashSet<>());
    }
    if (nodes.isEmpty()) throw new IllegalStateException("O processo não possui BPM executável.");
    for (var flow : diagram.path("flows")) {
      String from = flow.path("from").asText(), to = flow.path("to").asText();
      if (!nodes.containsKey(from) || !nodes.containsKey(to))
        throw new IllegalStateException("O BPM contém fluxo sem origem ou destino válido.");
      if (!"REWORK".equals(flow.path("kind").asText())) incoming.get(to).add(from);
    }
    var remaining = new LinkedHashSet<>(nodes.keySet());
    while (!remaining.isEmpty()) {
      var ready =
          remaining.stream()
              .filter(id -> Collections.disjoint(incoming.get(id), remaining))
              .toList();
      if (ready.isEmpty())
        throw new IllegalStateException("O BPM contém ciclo sem retorno REWORK explícito.");
      ordered.addAll(ready);
      remaining.removeAll(ready);
    }
  }

  /** Confere cobertura de todas as definições, inclusive condicionais, antes de ordenar o fluxo. */
  List<ProductProcessActivityExecutionGroupResponse> ordered(
      List<ProductProcessActivityExecutionGroupResponse> activities) {
    var expected = new LinkedHashSet<String>();
    nodes.forEach(
        (id, node) -> {
          if ("TASK".equals(node.path("type").asText())) expected.add(id);
        });
    var supplied = new LinkedHashSet<String>();
    for (var activity : activities) {
      if (activity.activityDefinitionId() == null) continue;
      if (!expected.contains(activity.activityId()) || !supplied.add(activity.activityId()))
        throw new IllegalStateException(
            "Contrato de atividade fora do BPM ou duplicado: " + activity.activityId());
    }
    expected.removeAll(supplied);
    if (!expected.isEmpty())
      throw new IllegalStateException(
          "Atividades do BPM sem definição e contrato de execução: " + expected);
    return activities.stream()
        .filter(a -> a.activityDefinitionId() != null && nodes.containsKey(a.activityId()))
        .sorted(Comparator.comparingInt(a -> ordered.indexOf(a.activityId())))
        .toList();
  }

  /** Exige as predecessoras aplicáveis; omissões declaradas nunca se tornam objetivos atingidos. */
  boolean predecessorsSatisfied(
      String id, List<ProductProcessActivityExecutionGroupResponse> activities) {
    Map<String, ProductProcessActivityExecutionGroupResponse> byId = new HashMap<>();
    activities.forEach(a -> byId.put(a.activityId(), a));
    return predecessors(id, new HashSet<>()).stream()
        .allMatch(
            previous -> {
              var activity = byId.get(previous);
              return activity != null
                  && (!activity.selectedVersionActivity()
                      || activity.objectiveAchieved()
                      || Set.of("HISTORICAL", "NOT_APPLICABLE", "RECORDED")
                          .contains(activity.operationalState()));
            });
  }

  /** Identifica retorno funcional declarado e liberado pelo contrato da atividade responsável. */
  boolean recovery(
      ProductProcessActivityExecutionGroupResponse activity,
      List<ProductProcessActivityExecutionGroupResponse> activities) {
    var node = nodes.get(activity.activityId());
    if (node == null || !"ON_FUNCTIONAL_REJECTION".equals(node.path("activationMode").asText()))
      return false;
    var targets = new HashSet<String>();
    node.path("remediatesActivities").forEach(target -> targets.add(target.asText()));
    return activity.executionRequestAvailable()
        && activities.stream()
            .anyMatch(
                other ->
                    targets.contains(other.activityId())
                        && !other.objectiveAchieved()
                        && ("BLOCKED".equals(other.operationalState())
                            || other.recoveryAction() != null));
  }

  /** Reconhece parecer de etapa dependente ou explicitamente atendida pela correção no BPM. */
  boolean canSupplyCorrection(String activityId, String reviewerId) {
    if (predecessors(reviewerId, new HashSet<>()).contains(activityId)) return true;
    var activity = nodes.get(activityId);
    if (activity != null)
      for (var target : activity.path("remediatesActivities"))
        if (reviewerId.equals(target.asText())) return true;
    return false;
  }

  /** Coleta tarefas anteriores atravessando eventos e gates do grafo sem executar condições. */
  private Set<String> predecessors(String id, Set<String> visited) {
    Set<String> result = new LinkedHashSet<>();
    for (String from : incoming.getOrDefault(id, Set.of())) {
      if (!visited.add(from)) continue;
      if ("TASK".equals(nodes.get(from).path("type").asText())) result.add(from);
      result.addAll(predecessors(from, visited));
    }
    return result;
  }
}
