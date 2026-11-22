package com.marketinghub.businessprocess.automation.v1.service.continuity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: resolver versões adotadas pelo recibo durável, sem alterar a cadeia histórica.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessVersionContinuityLookup {
  private final ProcessRunEventRepository events;
  private final ObjectMapper json;

  @org.springframework.beans.factory.annotation.Autowired
  private com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository cycles;

  /** Responsabilidade: transportar apenas a identidade da passagem registrada. */
  public record Binding(
      Long sourceProcessId, Long targetProcessId, Long successorRunId, Long chainId) {}

  /** Lê recibos do contexto exato; auditoria inválida nunca libera execução. */
  @Transactional(readOnly = true)
  public List<Binding> bindings(Long productId, Long cycleId, String reference) {
    if (productId == null || cycleId == null || reference == null) return List.of();
    return events.continuations(productId, cycleId, reference).stream()
        .map(
            event -> {
              try {
                return json.treeToValue(
                    json.readTree(event.getDetailsJson()).path("binding"), Binding.class);
              } catch (Exception ex) {
                log.error(
                    "Recibo de continuidade inválido productId={} cycleId={} eventId={}",
                    productId,
                    cycleId,
                    event.getId(),
                    ex);
                throw new IllegalStateException(
                    "Recibo de continuidade inválido; execução protegida.", ex);
              }
            })
        .toList();
  }

  /** Resolve adoções sucessivas sem transferir autorizações para outro contexto. */
  public Long effective(Long productId, Long cycleId, String reference, Long sourceProcessId) {
    var recorded = bindings(productId, cycleId, reference);
    var visited = new java.util.HashSet<Long>();
    Long current = sourceProcessId;
    while (visited.add(current)) {
      Long selected = current;
      var next = recorded.stream().filter(b -> selected.equals(b.sourceProcessId())).findFirst();
      if (next.isEmpty()) return current;
      current = next.get().targetProcessId();
    }
    throw new IllegalStateException("Ciclo inválido nos recibos de continuidade.");
  }

  /** Permite somente o destino expressamente adotado na cadeia original do ciclo. */
  public boolean permits(
      Long productId, Long cycleId, String reference, Long chainId, Long processId) {
    return bindings(productId, cycleId, reference).stream()
        .anyMatch(b -> chainId.equals(b.chainId()) && processId.equals(b.targetProcessId()));
  }

  /** Expõe somente tarefas cuja prova foi explicitamente reaproveitada no recibo do destino. */
  public java.util.Set<Long> reusedTasks(
      Long productId, Long cycleId, String reference, Long target) {
    var ids = new java.util.HashSet<Long>();
    for (var event : events.continuations(productId, cycleId, reference)) {
      try {
        var detail = json.readTree(event.getDetailsJson());
        if (detail.path("binding").path("targetProcessId").asLong() != target) continue;
        for (var item : detail.path("reused"))
          for (var id : item.path("taskIds")) ids.add(id.asLong());
      } catch (Exception ex) {
        log.error("Falha ao ler provas reutilizadas eventId={}", event.getId(), ex);
        throw new IllegalStateException("Recibo de reuso inválido.", ex);
      }
    }
    return java.util.Set.copyOf(ids);
  }

  /** Resolve provas pela referência global validada, preservando o isolamento do ciclo. */
  public java.util.Set<Long> reusedTasks(String reference, Long target) {
    if (reference == null || !reference.matches("experiment:[1-9][0-9]*"))
      return java.util.Set.of();
    var cycle = cycles.findByExperimentId(Long.valueOf(reference.substring(11)));
    return cycle
        .map(c -> reusedTasks(c.getProductId(), c.getId(), reference, target))
        .orElse(java.util.Set.of());
  }

  /** Projeta a identidade adotada para diagnósticos, sem mudar tarefas nem seus resultados. */
  public java.util.List<com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot>
      project(
          String reference,
          java.util.List<com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot>
              history) {
    if (reference == null || !reference.matches("experiment:[1-9][0-9]*")) return history;
    var cycle = cycles.findByExperimentId(Long.valueOf(reference.substring(11)));
    if (cycle.isEmpty()) return history;
    var c = cycle.get();
    var destinations = new java.util.HashMap<Long, Long>();
    var reused = new java.util.HashMap<Long, java.util.Set<Long>>();
    return history.stream()
        .map(
            task -> {
              if (task.processDefinitionId() == null) return task;
              var destination =
                  destinations.computeIfAbsent(
                      task.processDefinitionId(),
                      source -> effective(c.getProductId(), c.getId(), reference, source));
              if (destination.equals(task.processDefinitionId())
                  || "COMPLETED".equals(task.status())
                      && !reused
                          .computeIfAbsent(
                              destination,
                              target -> reusedTasks(c.getProductId(), c.getId(), reference, target))
                          .contains(task.id())) return task;
              return new com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot(
                  task.id(),
                  destination,
                  task.processActivityId(),
                  task.status(),
                  task.blockerCategory(),
                  task.blockerAction(),
                  task.resultJson(),
                  task.executionError(),
                  task.updatedAt());
            })
        .toList();
  }
}
