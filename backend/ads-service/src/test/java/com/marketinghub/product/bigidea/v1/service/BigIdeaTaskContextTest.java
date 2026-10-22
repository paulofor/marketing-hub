package com.marketinghub.product.bigidea.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.product.bigidea.v1.service.get.BigIdeaView;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Comprova fotografia imutável, contexto de executores e ausência histórica explícita. */
class BigIdeaTaskContextTest {
  /** Confere que atualização posterior não modifica alvo, contexto ou recuperação da tarefa. */
  @Test
  void pinsBeforeClaimAndProjectsTheSameSnapshot() {
    var service = mock(BigIdeaService.class);
    var targets = mock(AgentTaskTargetContextProvider.class);
    var json = new ObjectMapper().findAndRegisterModules();
    var context = new BigIdeaTaskContext(service, targets, json);
    var target =
        new AgentTaskTargetResponse(
            "experiment:903",
            903L,
            703L,
            "fixture",
            "Produto",
            "Sintético",
            "v1",
            null,
            null,
            null,
            null,
            null);
    when(targets.resolve("experiment:903")).thenReturn(Optional.of(target));
    var first =
        new BigIdeaView(
            1L,
            703L,
            1,
            BigIdeaServiceTest.content("Direção A"),
            "Inicial",
            "Operador",
            null,
            Instant.now());
    when(service.pin(703L, "experiment:903")).thenReturn(first);
    var task = new AgentTask();
    task.setSourceReference("experiment:903");
    context.pin(task);
    when(service.pin(703L, "experiment:903"))
        .thenThrow(new AssertionError("Não pode reler durante recuperação"));
    assertThat(context.snapshot(task).path("revision").path("revision").asInt()).isEqualTo(1);
    assertThat(context.project(task, target).pdeContext().path("productBigIdea"))
        .isEqualTo(context.snapshot(task));
    assertThat(context.snapshot(task).path("downstreamMayRedefine").asBoolean()).isFalse();
    assertThat(context.snapshot(task).path("commercialAuthorization").asBoolean()).isFalse();
    verify(service, times(1)).pin(703L, "experiment:903");
    assertThat(context.snapshot(new AgentTask()).path("status").asText())
        .isEqualTo("LEGACY_UNPINNED");
  }
}
