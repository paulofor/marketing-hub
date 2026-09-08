package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.learningcycle.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: prevenir mistura de produto, experimento e memória em tarefas dos agentes. */
class LearningCycleTaskContextTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final LearningCycleTaskContext context =
      new LearningCycleTaskContext(cycles, events, new LearningCycleJson(new ObjectMapper()));
  private final Instant now = Instant.parse("2026-09-08T15:00:00Z");

  /** Cria um ciclo com aprendizado inequívoco do experimento anterior. */
  private LearningSalesCycle cycle() {
    var cycle = new LearningSalesCycle();
    cycle.setId(1L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setProductVersion("v8");
    cycle.setStage("ADJUSTMENT");
    cycle.setCreatedAt(now);
    cycle.setBriefJson("{\"hypothesis\":\"Ação útil\"}");
    cycle.setInheritedLearningJson("{\"experimentId\":91,\"learning\":\"Explicar aplicação\"}");
    return cycle;
  }

  /** Entrega a memória do predecessor somente para o experimento explicitamente vinculado. */
  @Test
  void carriesPreviousLearningToNewExperimentTasks() {
    when(cycles.findByExperimentId(92L)).thenReturn(Optional.of(cycle()));
    var result = context.resolve("experiment:92", now.plusSeconds(1)).orElseThrow();
    assertEquals(92L, result.get("experimentId"));
    assertTrue(result.get("inheritedLearning").toString().contains("91"));
    assertTrue(context.resolve("experiment:90", now).isEmpty());
  }

  /** Preserva o contexto histórico de uma tarefa criada antes da abertura do ciclo. */
  @Test
  void neverRetargetsAnOldTask() {
    when(cycles.findByExperimentId(92L)).thenReturn(Optional.of(cycle()));
    assertTrue(context.resolve("experiment:92", now.minusSeconds(1)).isEmpty());
  }

  /** Resolve tarefas privadas do produto apenas quando há um único ciclo explícito. */
  @Test
  void privateProductTasksRequireUnambiguousActiveCycle() {
    when(cycles.findByProductIdAndOpenSlot(4L, 1)).thenReturn(List.of(cycle()));
    assertTrue(context.resolve("product:4@agent-validation-v1", now).isPresent());
    when(cycles.findByProductIdAndOpenSlot(4L, 1)).thenReturn(List.of(cycle(), cycle()));
    assertTrue(context.resolve("product:4@agent-validation-v1", now).isEmpty());
    assertTrue(context.resolve("product:5@agent-validation-v1", now).isEmpty());
  }

  /** Rejeita referências aproximadas, números inválidos e ausência da identidade temporal. */
  @Test
  void ignoresMalformedOrUnrelatedReferences() {
    for (String value :
        List.of(
            "experiment:92@other",
            "anything92",
            "product:4oops",
            "experiment:9999999999999999999999999"))
      assertTrue(context.resolve(value, now).isEmpty());
    assertTrue(context.resolve(null, now).isEmpty());
    assertTrue(context.resolve("experiment:92", null).isEmpty());
    verifyNoInteractions(cycles);
  }
}
