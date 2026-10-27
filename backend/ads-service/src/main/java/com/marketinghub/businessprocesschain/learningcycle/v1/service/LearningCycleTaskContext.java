package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: entregar aos especialistas a memória do ciclo ao qual a tarefa pertence. */
@Component
@RequiredArgsConstructor
public class LearningCycleTaskContext {
  private static final com.fasterxml.jackson.databind.ObjectMapper CONTRACT_MAPPER =
      new com.fasterxml.jackson.databind.ObjectMapper();
  private static final Pattern EXPERIMENT = Pattern.compile("^experiment:([0-9]{1,18})$");
  private static final Pattern PRODUCT = Pattern.compile("^product:([0-9]{1,18})(?:@[^\\s]+)?$");
  private final LearningSalesCycleRepository cycles;
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleJson json;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private LearningCycleImplementedInputContext implementedInput;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.product.modeling.v1.service.ModelingHistoricalResearch
      historicalResearch;

  /**
   * Entrega memória, fontes históricas e entrada implementada sem criar adoção ou renovar aceite.
   */
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
              var exactInput =
                  implementedInput == null ? null : implementedInput.resolve(value).orElse(null);
              var context =
                  new java.util.LinkedHashMap<String, Object>(
                      Map.ofEntries(
                          Map.entry(
                              "agentValidationExecution",
                              LearningCycleValidationProtocol.resolve(
                                  CONTRACT_MAPPER, value, exactInput)),
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
              if (implementedInput != null) {
                if (exactInput != null) context.put("implementedInput", exactInput);
                context.put(
                    "availableImplementations", implementedInput.availableImplementations(value));
              }
              if (historicalResearch != null) {
                var facts = historicalResearch.forCycle(value, value.getProductId(), taskCreatedAt);
                if (!facts.isNull()) context.put("inheritedReferenceResearch", facts);
              }
              return context;
            });
  }
}
