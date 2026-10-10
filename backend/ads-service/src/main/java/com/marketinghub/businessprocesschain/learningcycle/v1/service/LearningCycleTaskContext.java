package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: entregar aos especialistas a memória do ciclo ao qual a tarefa pertence. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleTaskContext {
  private static final Pattern EXPERIMENT = Pattern.compile("^experiment:([0-9]{1,18})$");
  private static final Pattern PRODUCT = Pattern.compile("^product:([0-9]{1,18})(?:@[^\\s]+)?$");
  private final LearningSalesCycleRepository cycles;
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleJson json;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private LearningCycleImplementedInputContext implementedInput;

  /** Entrega memória e entrada implementada pela identidade exata, recusando tarefas antigas. */
  @Transactional(readOnly = true)
  public Optional<Map<String, Object>> resolve(String reference, Instant taskCreatedAt) {
    if (reference == null || taskCreatedAt == null) return Optional.empty();
    var experiment = EXPERIMENT.matcher(reference);
    var product = PRODUCT.matcher(reference);
    Optional<LearningSalesCycle> cycle = Optional.empty();
    if (experiment.matches()) cycle = cycles.findByExperimentId(Long.valueOf(experiment.group(1)));
    else if (product.matches()) {
      var active = cycles.findByProductIdAndOpenSlot(Long.valueOf(product.group(1)), 1);
      if (active.size() == 1) cycle = Optional.of(active.getFirst());
    }
    return cycle
        .filter(value -> !taskCreatedAt.isBefore(value.getCreatedAt()))
        .map(
            value -> {
              var context =
                  new java.util.LinkedHashMap<String, Object>(
                      Map.ofEntries(
                          Map.entry("agentValidationExecution", validationExecution()),
                          Map.entry("contractVersion", "LEARNING_SALES_CYCLE_V1"),
                          Map.entry("cycleId", value.getId()),
                          Map.entry("productId", value.getProductId()),
                          Map.entry("experimentId", value.getExperimentId()),
                          Map.entry("productVersion", value.getProductVersion()),
                          Map.entry("stage", value.getStage()),
                          Map.entry("brief", json.read(value.getBriefJson())),
                          Map.entry(
                              "inheritedLearning", json.read(value.getInheritedLearningJson())),
                          Map.entry(
                              "currentDecisions",
                              events.findByCycleIdOrderByRevisionAsc(value.getId()).stream()
                                  .map(
                                      event ->
                                          Map.of(
                                              "action",
                                              event.getAction(),
                                              "summary",
                                              event.getSummary(),
                                              "evidenceReference",
                                              event.getEvidenceReference(),
                                              "evidence",
                                              json.read(event.getEvidenceJson())))
                                  .toList())));
              if (implementedInput != null)
                implementedInput
                    .resolve(value)
                    .ifPresent(input -> context.put("implementedInput", input));
              return context;
            });
  }

  /**
   * Entrega o protocolo do backend antes do planejamento, sem multiplicar pareceres por matrizes.
   */
  private JsonNode validationExecution() {
    try (var input =
        getClass().getResourceAsStream("/contracts/pde-agent-validation-plan-v1.json")) {
      if (input == null) throw new IOException("Contrato multiagente ausente do classpath.");
      return json.read(new String(input.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException ex) {
      log.error("Ciclo: falha ao carregar protocolo de execução multiagente", ex);
      throw new IllegalStateException("Protocolo multiagente indisponível.", ex);
    }
  }
}
