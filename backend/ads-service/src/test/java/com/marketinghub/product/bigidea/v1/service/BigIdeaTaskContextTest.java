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
  /** Confere que o contexto real fixa a base antes de persistir e a projeta ao executor. */
  @Test
  void transportsValueBaselineAndSpecificationThroughExistingTarget() {
    var service = mock(BigIdeaService.class);
    var targets = mock(AgentTaskTargetContextProvider.class);
    var json = new ObjectMapper();
    var context = new BigIdeaTaskContext(service, targets, json);
    var delivery =
        mock(com.marketinghub.product.valuedelivery.v1.service.ProductValueDeliveryService.class);
    org.springframework.test.util.ReflectionTestUtils.setField(context, "valueDelivery", delivery);
    var task = new AgentTask();
    task.setSourceReference("experiment:903");
    var target =
        new AgentTaskTargetResponse(
            "experiment:903",
            903L,
            703L,
            "fixture",
            "Produto",
            "Teste",
            "v1",
            null,
            null,
            null,
            null,
            null);
    when(targets.resolve("experiment:903", null)).thenReturn(Optional.of(target));
    doAnswer(
            invocation -> {
              com.fasterxml.jackson.databind.node.ObjectNode snapshot = invocation.getArgument(1);
              snapshot.putObject("valueDeliveryBaseline").put("applicable", true);
              return null;
            })
        .when(delivery)
        .pin(eq(task), any(), eq(target));
    when(delivery.context(task, target))
        .thenReturn(
            json.createObjectNode().put("specificationReference", "VALUE_SPECIFICATION:fixture"));
    context.pin(task);
    assertThat(context.snapshot(task).path("valueDeliveryBaseline").path("applicable").asBoolean())
        .isTrue();
    assertThat(
            context
                .project(task, target)
                .pdeContext()
                .path("productValueDelivery")
                .path("specificationReference")
                .asText())
        .isEqualTo("VALUE_SPECIFICATION:fixture");
  }

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
    when(targets.resolve("experiment:903", null)).thenReturn(Optional.of(target));
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

  /** Resolve a Big Idea antes da candidata existir, sem reinterpretar fotografias históricas. */
  @Test
  void pinsProductUsingPlanningContext() {
    var service = mock(BigIdeaService.class);
    var targets = mock(AgentTaskTargetContextProvider.class);
    var json = new ObjectMapper();
    var context = new BigIdeaTaskContext(service, targets, json);
    var process = new com.marketinghub.businessprocess.BusinessProcessDefinition();
    process.setProcessCode("pde-commercial-plan-offer");
    var task = new AgentTask();
    task.setSourceReference("experiment:99112");
    task.setProcessDefinition(process);
    when(targets.resolve("experiment:99112", process.getProcessCode()))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    "experiment:99112",
                    99112L,
                    99007L,
                    "outra-identidade",
                    "Produto",
                    "Teste",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null)));
    context.pin(task);
    assertThat(context.snapshot(task).path("status").asText()).isEqualTo("MISSING");
    assertThat(context.snapshot(task).path("productId").asLong()).isEqualTo(99007L);
    verify(service).pin(99007L, "experiment:99112");
    verify(targets, never()).resolve(anyString());
    task.setProductBigIdeaJson("{\"status\":\"PRODUCT_NOT_CREATED\"}");
    assertThat(context.snapshot(task).path("status").asText()).isEqualTo("PRODUCT_NOT_CREATED");
  }
}
