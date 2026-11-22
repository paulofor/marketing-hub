package com.marketinghub.agentcostwaiver.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentcostwaiver.v1.service.decisions.CostWaiver;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleJson;
import com.marketinghub.repository.jdbc.learningcycle.CyclePreparationCostRepository;
import com.marketinghub.repository.jpa.agentcostwaiver.AgentCostWaiverEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/** Protege escopo fixo, idempotência e auditoria da dispensa sem executar agentes. */
class AgentCostWaiverServiceTest {
  private final AgentCostWaiverEventRepository waiverEvents =
      mock(AgentCostWaiverEventRepository.class);
  private final CyclePreparationCostRepository costs = mock(CyclePreparationCostRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final LearningCycleJson json =
      new LearningCycleJson(new ObjectMapper().findAndRegisterModules());
  private final AgentCostWaiverService service =
      new AgentCostWaiverService(
          costs,
          events,
          json,
          mock(LearningSalesCycleRepository.class),
          mock(ProductRepository.class),
          java.time.Clock.systemUTC(),
          waiverEvents);

  /** Reproduz a seleção parcial para identidades distintas e reabre o recibo sem duplicação. */
  @ParameterizedTest
  @ValueSource(longs = {4, 99004})
  void recordsFixedSelectionAndReplays(long productId) {
    var cycle = cycle(productId);
    var request = request(List.of(246L));
    when(costs.waiverTasks(productId)).thenReturn(List.of(task(246L, true), task(247L, true)));
    service.record(cycle, request, Instant.parse("2026-11-22T12:00:00Z"));
    var captured = ArgumentCaptor.forClass(LearningSalesCycleEvent.class);
    verify(events).saveAndFlush(captured.capture());
    var event = captured.getValue();
    assertThat(json.read(event.getEvidenceJson()).path("taskIds").toString()).isEqualTo("[246]");
    assertThat(
            json.read(event.getEvidenceJson())
                .path("tasksAtDecision")
                .get(0)
                .path("costUsd")
                .isNull())
        .isTrue();
    assertThat(cycle.getRevision()).isEqualTo(5);
    assertThat(cycle.getStage()).isEqualTo("PREPARATION");
    when(events.findByCycleIdAndRequestKey(22L, request.requestKey().toString()))
        .thenReturn(Optional.of(event));
    service.record(cycle, request, Instant.now());
    verify(events, times(1)).saveAndFlush(any());
    when(waiverEvents.findByProductId(productId)).thenReturn(List.of(event));
    when(costs.waiverTasks(productId))
        .thenReturn(
            List.of(
                new CostWaiver.Task(246L, 90L, "CANCELLED", null, true, false),
                task(247L, true),
                task(999L, true)));
    var reopened =
        new AgentCostWaiverService(
                costs,
                events,
                json,
                mock(LearningSalesCycleRepository.class),
                mock(ProductRepository.class),
                java.time.Clock.systemUTC(),
                waiverEvents)
            .read(productId);
    assertThat(reopened.blockingTasks()).isEqualTo(2);
    assertThat(reopened.history().getFirst().taskIds()).containsExactly(246L);
    assertThat(reopened.tasks().getFirst().costUsd()).isNull();
    assertThat(reopened.history().getFirst().operatorName()).isEqualTo("Operador teste");
  }

  /** Rejeita tarefas fora do escopo, seleção duplicada, custo já informado e revisão obsoleta. */
  @Test
  void rejectsInvalidSelections() {
    var cycle = cycle(4);
    when(costs.waiverTasks(4L)).thenReturn(List.of(task(246L, true), task(247L, false)));
    for (var ids : List.of(List.of(999L), List.of(247L), List.of(246L, 246L), List.<Long>of()))
      assertThatThrownBy(() -> service.record(cycle, request(ids), Instant.now()))
          .isInstanceOf(RuntimeException.class);
    cycle.setRevision(9);
    assertThatThrownBy(() -> service.record(cycle, request(List.of(246L)), Instant.now()))
        .hasMessageContaining("ciclo mudou");
    verify(events, never()).saveAndFlush(any());
  }

  /** Custo recuperado continua no histórico sem ser tratado como nova pendência. */
  @Test
  void recoveredCostRemainsVisible() {
    when(costs.waiverTasks(4L))
        .thenReturn(
            List.of(
                new CostWaiver.Task(
                    246L, 90L, "CANCELLED", new java.math.BigDecimal("12.50"), true, false)));
    assertThat(service.read(4L).blockingTasks()).isZero();
    assertThat(service.read(4L).tasks().getFirst().costUsd()).isEqualByComparingTo("12.50");
  }

  /** Exige confirmação na fronteira HTTP e preserva o contexto do operador. */
  @Test
  void validatesHttpContract() throws Exception {
    var api = mock(AgentCostWaiverService.class);
    var mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new com.marketinghub.agentcostwaiver.v1.controller.AgentCostWaiverController(api))
            .build();
    String route = "/api/agent-cost-waivers/v1/products/4/cycles/22";
    var request = request(List.of(246L));
    when(api.decide(eq(4L), eq(22L), any()))
        .thenReturn(new CostWaiver.View(List.of(), List.of(), 0));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(route)
                .contentType("application/json")
                .content(json.write(request)))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    var invalid =
        new CostWaiver.Request(
            request.requestKey(), 27L, 4L, List.of(246L), "Teste", "Motivo", false);
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(route)
                .contentType("application/json")
                .content(json.write(invalid)))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isBadRequest());
    verify(api, times(1)).decide(eq(4L), eq(22L), any());
  }

  /** Cria uma identidade sintética com revisão conhecida. */
  private LearningSalesCycle cycle(long productId) {
    var c = new LearningSalesCycle();
    c.setId(22L);
    c.setProductId(productId);
    c.setChainDefinitionId(27L);
    c.setRevision(4);
    c.setStatus("OPEN");
    c.setStage("PREPARATION");
    return c;
  }

  /** Cria decisão confirmada sem orçamento nem comando de execução. */
  private CostWaiver.Request request(List<Long> ids) {
    return new CostWaiver.Request(
        UUID.randomUUID(),
        27L,
        4L,
        ids,
        "Operador teste",
        "Custos históricos irrecuperáveis",
        true);
  }

  /** Cria pendência sintética com elegibilidade explícita. */
  private CostWaiver.Task task(long id, boolean selectable) {
    return new CostWaiver.Task(id, 90L, "CANCELLED", null, false, selectable);
  }
}
