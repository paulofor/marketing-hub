package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.bindImplementation.Bind;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: comprovar vínculo explícito sem inferência, homologação ou gasto implícitos.
 */
class KitImplementationBindingTest {
  /** Executa o caso original e outra identidade, preservando desenho, versão e estado. */
  @ParameterizedTest
  @ValueSource(longs = {7, 707})
  void bindsInstalledCapabilityWithoutChangingTaskOrApprovingProduct(long productId) {
    var f = new Fixture(productId);
    var plan = f.service.plan(productId, 15L);
    var input =
        new Bind(
            UUID.randomUUID(),
            2,
            productId,
            15L,
            829L,
            plan.architectureSha256(),
            "beauty-v1",
            "Operador de QA");
    f.service.bind(input);
    var captor = org.mockito.ArgumentCaptor.forClass(LearningSalesCycleEvent.class);
    verify(f.events).saveAndFlush(captor.capture());
    var event = captor.getValue();
    assertThat(event.getAction()).isEqualTo("BIND_KIT_IMPLEMENTATION");
    assertThat(event.getEvidenceJson())
        .contains("\"implementationValidated\":false", "\"commercialSpendAuthorized\":false");
    assertThat(f.cycle.getStage()).isEqualTo("ADJUSTMENT");
    assertThat(f.cycle.getRevision()).isEqualTo(3);
    assertThat(f.service.resolved(f.cycle, f.task)).isEmpty();
    when(f.events.findByCycleIdAndActionOrderByRevisionDesc(15L, event.getAction()))
        .thenReturn(List.of(event));
    assertThat(f.service.resolved(f.cycle, f.task)).contains("beauty-v1");
    var registered = f.service.registrationEvidence(f.cycle);
    assertThat(f.service.currentProof(f.cycle, registered)).isTrue();
    registered.put("implementationArchitectureSha256", "0".repeat(64));
    assertThat(f.service.currentProof(f.cycle, registered)).isFalse();
    assertThat(f.service.resolved(f.cycle, f.snapshot(830, "BLOCKED"))).isEmpty();
    when(f.events.findByCycleIdAndRequestKey(15L, input.requestKey().toString()))
        .thenReturn(Optional.of(event));
    f.service.bind(input);
    verify(f.events, times(1)).saveAndFlush(any());
    verify(f.tasks, never()).save(any());
  }

  /** Uma família explicitamente declarada no desenho dispensa seleção repetida do operador. */
  @Test
  void reusesExplicitBeautyDeclarationWithoutAnotherOperatorHandoff() {
    var f = new Fixture(7);
    var explicit =
        new AgentTaskFunctionalSnapshot(
            829L,
            126L,
            "pde-commercial-plan-offer",
            "productArchitecture",
            "landing-generator",
            "COMPLETED",
            Instant.EPOCH,
            Instant.EPOCH,
            f.task
                .resultJson()
                .replace(
                    "\"productArchitecture\":{",
                    "\"productArchitecture\":{\"kitProfileCode\":\"beauty-v1\","));
    when(f.tasks.findFunctionalSnapshots(anyString(), anySet(), any()))
        .thenReturn(List.of(explicit));
    var proof = f.service.registrationEvidence(f.cycle);
    assertThat(f.service.currentProof(f.cycle, proof)).isTrue();
    verify(f.events, never()).saveAndFlush(any());
  }

  /** Recusa revisão, código ou identidade divergentes antes de gravar qualquer recibo. */
  @Test
  void rejectsStaleRevisionUnsupportedCapabilityAndCrossProduct() {
    var f = new Fixture(7);
    var plan = f.service.plan(7L, 15L);
    for (var input :
        List.of(
            new Bind(
                UUID.randomUUID(), 1, 7L, 15L, 829L, plan.architectureSha256(), "beauty-v1", "QA"),
            new Bind(
                UUID.randomUUID(), 2, 7L, 15L, 829L, plan.architectureSha256(), "unknown", "QA"),
            new Bind(
                UUID.randomUUID(),
                2,
                7L,
                15L,
                830L,
                plan.architectureSha256(),
                "beauty-v1",
                "QA"))) {
      assertThatThrownBy(() -> f.service.bind(input))
          .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    assertThatThrownBy(() -> f.service.plan(8L, 15L))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    verify(f.events, never()).saveAndFlush(any());
  }

  /** Um desenho sem contrato determinístico ou com bloqueio posterior não admite seleção. */
  @Test
  void refusesUnrelatedOrLatestBlockedArchitecture() {
    var f = new Fixture(7);
    when(f.tasks.findFunctionalSnapshots(anyString(), anySet(), any()))
        .thenReturn(List.of(f.snapshot(830, "BLOCKED")));
    assertThatThrownBy(() -> f.service.plan(7L, 15L))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    when(f.tasks.findFunctionalSnapshots(anyString(), anySet(), any()))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    831L,
                    126L,
                    "pde-commercial-plan-offer",
                    "productArchitecture",
                    "landing-generator",
                    "COMPLETED",
                    Instant.EPOCH,
                    Instant.EPOCH,
                    "{\"decision\":\"APPROVE\",\"productArchitecture\":{}}")));
    assertThatThrownBy(() -> f.service.plan(7L, 15L))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    verify(f.events, never()).saveAndFlush(any());
  }

  /** A seleção de compatibilidade não substitui um perfil explícito, válido ou desconhecido. */
  @ParameterizedTest
  @ValueSource(strings = {"nails-v1", "beauty-v1", "unsupported"})
  void refusesOverridingExplicitArchitectureProfile(String code) {
    var f = new Fixture(7);
    var explicit =
        new AgentTaskFunctionalSnapshot(
            829L,
            126L,
            "pde-commercial-plan-offer",
            "productArchitecture",
            "landing-generator",
            "COMPLETED",
            Instant.EPOCH,
            Instant.EPOCH,
            f.task
                .resultJson()
                .replace(
                    "\"productArchitecture\":{",
                    "\"productArchitecture\":{\"kitProfileCode\":\"" + code + "\","));
    when(f.tasks.findFunctionalSnapshots(anyString(), anySet(), any()))
        .thenReturn(List.of(explicit));
    var plan = f.service.plan(7L, 15L);
    assertThatThrownBy(
            () ->
                f.service.bind(
                    new Bind(
                        UUID.randomUUID(),
                        2,
                        7L,
                        15L,
                        829L,
                        plan.architectureSha256(),
                        "beauty-v1",
                        "QA")))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    verify(f.events, never()).saveAndFlush(any());
  }

  /** Responsabilidade: fornecer dependências locais, mantendo o contrato histórico do caso real. */
  private static final class Fixture {
    final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
    final LearningSalesCycleEventRepository events = mock(LearningSalesCycleEventRepository.class);
    final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    final LearningSalesCycle cycle = new LearningSalesCycle();
    final AgentTaskFunctionalSnapshot task = snapshot(829, "APPROVE");
    final KitImplementationBinding service =
        new KitImplementationBinding(cycles, events, tasks, new ObjectMapper());

    /** Isola identidades e persistência sem chamar modelo, fila ou rede. */
    Fixture(long productId) {
      cycle.setId(15L);
      cycle.setProductId(productId);
      cycle.setExperimentId(108L);
      cycle.setRevision(2);
      cycle.setProductVersion("capella-beleza-v1");
      cycle.setStage("ADJUSTMENT");
      cycle.setStatus("OPEN");
      cycle.setCreatedAt(Instant.EPOCH);
      when(cycles.findById(15L)).thenReturn(Optional.of(cycle));
      when(cycles.findLocked(productId, 15L)).thenReturn(Optional.of(cycle));
      when(tasks.findFunctionalSnapshots(anyString(), anySet(), any())).thenReturn(List.of(task));
    }

    /** Mantém o contrato mínimo explícito de fixtures sem homologação ou gasto. */
    AgentTaskFunctionalSnapshot snapshot(long id, String decision) {
      return new AgentTaskFunctionalSnapshot(
          id,
          126L,
          "pde-commercial-plan-offer",
          "productArchitecture",
          "landing-generator",
          "COMPLETED",
          Instant.EPOCH,
          Instant.EPOCH,
          "{\"decision\":\""
              + decision
              + "\",\"productArchitecture\":{\"staticResultFixtures\":{\"contractVersion\":\"PDE_STATIC_RESULT_FIXTURES_V1\",\"mode\":\"DETERMINISTIC_HOMOLOGATION_ONLY\",\"providerCallsAuthorized\":0,\"externalSideEffects\":false,\"commercialEvidenceEligible\":false}}}");
    }
  }
}
