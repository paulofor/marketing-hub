package com.marketinghub.businessprocess.automation.v1.service.continuity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.ProcessRunEvent;
import com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir perda de continuidade quando uma versão adotada também é retirada. */
class ProcessVersionContinuityLookupTest {
  /** Resolve adoções sucessivas e rejeita ciclos no diário sem liberar outro produto. */
  @Test
  void resolvesSuccessiveAdoptionsAndRejectsCycles() {
    var events = mock(ProcessRunEventRepository.class);
    var lookup = new ProcessVersionContinuityLookup(events, new ObjectMapper());
    when(events.continuations(71L, 99L, "experiment:801"))
        .thenReturn(List.of(event(601, 701), event(501, 601)));
    assertThat(lookup.effective(71L, 99L, "experiment:801", 501L)).isEqualTo(701L);
    assertThat(lookup.effective(71L, 99L, "experiment:801", 601L)).isEqualTo(701L);
    assertThat(lookup.effective(72L, 99L, "experiment:801", 501L)).isEqualTo(501L);
    when(events.continuations(71L, 99L, "experiment:801"))
        .thenReturn(List.of(event(701, 501), event(601, 701), event(501, 601)));
    assertThatThrownBy(() -> lookup.effective(71L, 99L, "experiment:801", 501L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Ciclo inválido");
  }

  /** Projeta apenas provas aceitas no destino final; rejeição anterior continua como contexto. */
  @Test
  void projectsOnlyExplicitlyReusedProofsAcrossVersions() {
    var events = mock(ProcessRunEventRepository.class);
    var cycles =
        mock(com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository.class);
    var cycle = new com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle();
    cycle.setId(99L);
    cycle.setProductId(71L);
    when(cycles.findByExperimentId(801L)).thenReturn(java.util.Optional.of(cycle));
    var lookup = new ProcessVersionContinuityLookup(events, new ObjectMapper());
    org.springframework.test.util.ReflectionTestUtils.setField(lookup, "cycles", cycles);
    var latest = event(601, 701);
    latest.setDetailsJson(
        latest.getDetailsJson().replace("}}", "},\"reused\":[{\"taskIds\":[9001]}]}"));
    when(events.continuations(71L, 99L, "experiment:801"))
        .thenReturn(List.of(latest, event(501, 601)));
    var proof =
        new com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot(
            9001L, 501L, "journey", "COMPLETED", null, null, "proof", null);
    var unreviewed =
        new com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot(
            9002L, 501L, "access", "COMPLETED", null, null, "not-reused", null);
    var rejected =
        new com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot(
            9003L,
            501L,
            "prototypeCorrection",
            "BLOCKED",
            "TECHNICAL",
            "Correct",
            "rejection",
            null);
    var projected = lookup.project("experiment:801", List.of(proof, unreviewed, rejected));
    assertThat(projected.get(0).processDefinitionId()).isEqualTo(701L);
    assertThat(projected.get(0).resultJson()).isEqualTo("proof");
    assertThat(projected.get(1)).isSameAs(unreviewed);
    assertThat(projected.get(2).processDefinitionId()).isEqualTo(701L);
    assertThat(projected.get(2).status()).isEqualTo("BLOCKED");
    assertThat(proof.processDefinitionId()).isEqualTo(501L);
  }

  /** Entrega histórico somente ao destino correto, sem inseri-lo entre as provas aceitas. */
  @Test
  void exposesRevalidationOnlyForBoundTarget() {
    var events = mock(ProcessRunEventRepository.class);
    var cycles =
        mock(com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository.class);
    var cycle = new com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle();
    cycle.setId(99L);
    cycle.setProductId(71L);
    when(cycles.findByExperimentId(801L)).thenReturn(java.util.Optional.of(cycle));
    var lookup = new ProcessVersionContinuityLookup(events, new ObjectMapper());
    org.springframework.test.util.ReflectionTestUtils.setField(lookup, "cycles", cycles);
    var receipt = event(601, 701);
    receipt.setDetailsJson(
        receipt
            .getDetailsJson()
            .replace(
                "}}",
                "},\"outputs\":[{\"activityId\":\"journey\",\"accepted\":true}],\"revalidation\":[{\"taskIds\":[9001]}]}"));
    when(events.continuations(71L, 99L, "experiment:801")).thenReturn(List.of(receipt));
    assertThat(lookup.revalidationContext("experiment:801", 701L).path("outputs").size())
        .isEqualTo(1);
    assertThat(lookup.revalidationContext("experiment:801", 601L).isNull()).isTrue();
    assertThat(lookup.revalidationContext("experiment:802", 701L).isNull()).isTrue();
    assertThat(lookup.reusedTasks(71L, 99L, "experiment:801", 701L)).isEmpty();
  }

  /** Monta somente o vínculo sintético de versões, sem tarefas ou custos. */
  private ProcessRunEvent event(long source, long target) {
    var event = new ProcessRunEvent();
    event.setDetailsJson(
        "{\"binding\":{\"sourceProcessId\":"
            + source
            + ",\"targetProcessId\":"
            + target
            + ",\"successorRunId\":900,\"chainId\":81}}");
    return event;
  }
}
