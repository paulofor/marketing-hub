package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.mockito.Mockito.*;

import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.ExperimentRunningGateRequirementDto;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: simular exclusivamente fontes comerciais externas para homologar o ciclo real.
 */
@TestConfiguration
@Profile("learning-cycles-fixture")
@Import({
  LearningCycleCommercialReadiness.class,
  LearningCycleCommercialReviewReadiness.class,
  LearningCycleCommercialFixtures.Controller.class
})
public class LearningCycleCommercialFixtures {
  private static final Map<Long, Set<String>> MISSING = new ConcurrentHashMap<>();
  private static final List<String> CODES =
      List.of(
          "LANDING_APPROVED",
          "CREATIVE_APPROVED",
          "CHECKOUT_READY",
          "INSTRUMENTATION_READY",
          "TARGETING_READY");

  /** Limpa apenas o test double para que um caso não altere a preparação de outro. */
  static void reset() {
    MISSING.clear();
  }

  /** Mantém o gate externo simulado e a política de preparação real na aplicação de teste. */
  @Bean
  ExperimentReadinessService commercialGate() {
    var gate = mock(ExperimentReadinessService.class);
    when(gate.summarize(anyLong()))
        .thenAnswer(
            call -> {
              long id = call.getArgument(0);
              var missing = MISSING.getOrDefault(id, Set.of());
              var requirements =
                  CODES.stream()
                      .map(
                          code ->
                              new ExperimentRunningGateRequirementDto(
                                  code,
                                  switch (code) {
                                    case "LANDING_APPROVED" -> "Destino comercial";
                                    case "CREATIVE_APPROVED" -> "Criativo aprovado";
                                    case "CHECKOUT_READY" -> "Checkout configurado";
                                    case "INSTRUMENTATION_READY" -> "Instrumentação";
                                    default -> "Público Meta";
                                  },
                                  !missing.contains(code),
                                  "Fonte comercial simulada do experimento " + id,
                                  "Preparar insumo local " + code))
                      .toList();
              return new ExperimentReadinessSummaryDto(
                  false,
                  0,
                  false,
                  0,
                  false,
                  false,
                  0,
                  7,
                  List.of(),
                  List.of(),
                  false,
                  requirements);
            });
    return gate;
  }

  /** Responsabilidade: controlar dependências sintéticas sem escrever dados produtivos. */
  @RestController
  @Profile("learning-cycles-fixture")
  static class Controller {
    /** Alterna insumos de uma identidade reservada exclusivamente à sandbox. */
    @PostMapping("/fixture/commercial-preparation/{experimentId}")
    Map<String, Object> configure(
        @PathVariable Long experimentId, @RequestBody List<String> missing) {
      if (!Set.of(91001L, 91002L).contains(experimentId) || !CODES.containsAll(missing))
        throw new IllegalArgumentException("Identidade ou requisito fora da fixture local.");
      MISSING.put(experimentId, Set.copyOf(missing));
      return Map.of("simulated", true, "experimentId", experimentId, "missing", missing);
    }
  }
}
