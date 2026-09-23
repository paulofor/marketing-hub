package com.marketinghub.businessprocess;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Responsabilidade: interpretar e ordenar a topologia causal de um grafo BPM versionado. */
public final class BusinessProcessGraphTopology {
  private final Map<String, JsonNode> nodes = new LinkedHashMap<>();
  private final Map<String, Set<String>> incoming = new HashMap<>();
  private final List<String> orderedNodeIds = new ArrayList<>();

  /** Valida o grafo e calcula sua ordem causal, ignorando somente retornos REWORK explícitos. */
  public BusinessProcessGraphTopology(JsonNode diagram) {
    for (var node : diagram.path("nodes")) {
      String id = node.path("id").asText();
      if (id.isBlank() || nodes.putIfAbsent(id, node) != null) {
        throw new IllegalStateException("O BPM contém identidade ausente ou duplicada.");
      }
      incoming.put(id, new LinkedHashSet<>());
    }
    if (nodes.isEmpty()) {
      throw new IllegalStateException("O processo não possui BPM executável.");
    }
    for (var flow : diagram.path("flows")) {
      String from = flow.path("from").asText();
      String to = flow.path("to").asText();
      if (!nodes.containsKey(from) || !nodes.containsKey(to)) {
        throw new IllegalStateException("O BPM contém fluxo sem origem ou destino válido.");
      }
      if (!"REWORK".equals(flow.path("kind").asText())) {
        incoming.get(to).add(from);
      }
    }
    var remaining = new LinkedHashSet<>(nodes.keySet());
    while (!remaining.isEmpty()) {
      var ready =
          remaining.stream()
              .filter(id -> Collections.disjoint(incoming.get(id), remaining))
              .toList();
      if (ready.isEmpty()) {
        throw new IllegalStateException("O BPM contém ciclo sem retorno REWORK explícito.");
      }
      orderedNodeIds.addAll(ready);
      remaining.removeAll(ready);
    }
  }

  /** Preserva entradas legadas sem grafo e ordena as demais atividades pelo fluxo causal. */
  public static <T> List<T> orderActivities(
      JsonNode diagram, List<T> activities, Function<T, String> activityId) {
    if (diagram == null || !diagram.path("nodes").isArray() || diagram.path("nodes").isEmpty()) {
      return List.copyOf(activities);
    }
    return new BusinessProcessGraphTopology(diagram).orderActivities(activities, activityId);
  }

  /**
   * Ordena atividades conhecidas pelo grafo e mantém registros históricos desconhecidos ao final.
   */
  public <T> List<T> orderActivities(List<T> activities, Function<T, String> activityId) {
    Map<String, Integer> positions = new HashMap<>();
    List<String> taskIds = orderedTaskIds();
    for (int index = 0; index < taskIds.size(); index++) {
      positions.put(taskIds.get(index), index);
    }
    return activities.stream()
        .sorted(
            Comparator.comparingInt(
                activity -> positions.getOrDefault(activityId.apply(activity), Integer.MAX_VALUE)))
        .toList();
  }

  /** Lista somente os nós executáveis na ordem causal calculada para o processo. */
  public List<String> orderedTaskIds() {
    return orderedNodeIds.stream()
        .filter(id -> "TASK".equals(nodes.get(id).path("type").asText()))
        .toList();
  }

  /** Recupera um nó do contrato versionado para regras que dependem de seus metadados. */
  public JsonNode node(String id) {
    return nodes.get(id);
  }

  /** Informa se o identificador pertence ao grafo versionado. */
  public boolean contains(String id) {
    return nodes.containsKey(id);
  }

  /** Expõe somente as predecessoras diretas do fluxo progressivo, sem retornos de retrabalho. */
  public Set<String> directPredecessors(String id) {
    return Set.copyOf(incoming.getOrDefault(id, Set.of()));
  }
}
