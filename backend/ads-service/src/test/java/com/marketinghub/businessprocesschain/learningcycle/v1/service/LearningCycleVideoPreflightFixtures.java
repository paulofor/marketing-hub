package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marketinghub.repository.jpa.salesvideo.*;
import com.marketinghub.salesvideo.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: simular resultados do fornecedor sem executar rede ou geração nos testes REST.
 */
@TestConfiguration
@Profile("learning-cycles-fixture")
@Import(LearningCycleVideoPreflightFixtures.Controller.class)
public class LearningCycleVideoPreflightFixtures {
  private static final Map<Long, Map<String, Object>> DATA = new ConcurrentHashMap<>();

  /**
   * Remove retornos sintéticos junto com o reset do ciclo para impedir vazamento entre cenários.
   */
  static void clear() {
    DATA.clear();
  }

  /** Simula o resultado da consulta cuja segregação SQL tem teste de contrato separado. */
  @Bean
  VideoProductionCycleRepository videoProductions() {
    var repo = mock(VideoProductionCycleRepository.class);
    when(repo.findLatestForLearningCycle(anyLong(), anyLong(), anyString(), anyString(), any()))
        .thenAnswer(
            call ->
                DATA.values().stream()
                    .filter(
                        d ->
                            Objects.equals(d.get("productId"), call.getArgument(0))
                                && Objects.equals(d.get("experimentId"), call.getArgument(1))
                                && Objects.equals(d.get("productVersion"), call.getArgument(2))
                                && Objects.equals(d.get("strategyRole"), call.getArgument(3))
                                && !((Instant) d.get("createdAt")).isBefore(call.getArgument(4)))
                    .sorted(
                        Comparator.<Map<String, Object>, Instant>comparing(
                                d -> (Instant) d.get("createdAt"))
                            .thenComparing(d -> (Long) d.get("id"))
                            .reversed())
                    .findFirst()
                    .map(
                        d -> {
                          var result = new VideoProductionCycle();
                          result.setId((Long) d.get("id"));
                          result.setVideoProjectId((Long) d.get("projectId"));
                          result.setStatus((String) d.get("status"));
                          return result;
                        }));
    return repo;
  }

  /** Simula auditoria do preflight, mantendo estados e identidades explícitos. */
  @Bean
  VideoProviderPreflightRepository videoPreflights() {
    var repo = mock(VideoProviderPreflightRepository.class);
    when(repo.findByVideoProductionCycleId(anyLong()))
        .thenAnswer(
            call -> {
              var d = DATA.get(call.getArgument(0));
              if (d == null) return Optional.empty();
              var result = new VideoProviderPreflight();
              result.setId((Long) d.get("id"));
              result.setVideoProductionCycleId((Long) d.get("id"));
              result.setStatus((String) d.getOrDefault("preflightStatus", "BLOCKED"));
              result.setFailureCode(
                  (String) d.getOrDefault("failureCode", "PROVIDER_ROUTER_CONFIG_MISSING"));
              return Optional.of(result);
            });
    return repo;
  }

  /** Responsabilidade: controlar somente os retornos sintéticos do fornecedor na fixture local. */
  @RestController
  @Profile("learning-cycles-fixture")
  static class Controller {
    /** Instala um resultado sem criar tarefa, reserva, mídia ou aprovação real. */
    @PostMapping("/fixture/video-preflights")
    Map<String, Object> seed(@RequestBody Map<String, Object> body) {
      var data = new HashMap<>(body);
      for (String key : List.of("id", "productId", "experimentId", "projectId"))
        data.put(key, ((Number) data.get(key)).longValue());
      if (!LearningCycleLocalApplication.EXPERIMENTS.containsKey(data.get("experimentId")))
        throw new IllegalArgumentException("Somente experimento local.");
      data.put("createdAt", Instant.parse((String) data.get("createdAt")));
      DATA.put((Long) data.get("id"), data);
      return Map.of("simulated", true, "id", data.get("id"));
    }
  }
}
