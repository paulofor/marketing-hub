package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.kit.privateprototype.v1.controller.KitPrivateController;
import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.Claim;
import com.marketinghub.repository.jpa.kit.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: impedir que um consumidor antigo destrua reservas de perfis não instalados. */
class KitCompositorRoutingTest {
  /** Reproduz o consumidor sem catálogo e mantém autenticação obrigatória na mesma rota. */
  @Test
  void legacyPollingReceivesNoWorkWithoutChangingAuthentication() throws Exception {
    var service = mock(KitPrivateService.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new KitPrivateController(service, "local-pde", "local-worker", ""))
            .build();
    mvc.perform(get("/api/pde/kit/private/v1/stage-executions/pending"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(service);
    mvc.perform(
            get("/api/pde/kit/private/v1/stage-executions/pending")
                .header("X-Payments-Auth", "local-worker"))
        .andExpect(status().isOk());
    verify(service).pending(Set.of());
    mvc.perform(
            get("/api/pde/kit/private/v1/stage-executions/pending")
                .header("X-Payments-Auth", "local-worker")
                .header(KitCompositorCapabilities.HEADER, "hair-v2,barber-v1"))
        .andExpect(status().isOk());
    verify(service).pending(Set.of("hair-v2", "barber-v1"));
    mvc.perform(
            get("/api/pde/kit/private/v1/stage-executions/pending")
                .header("X-Payments-Auth", "local-worker")
                .header(KitCompositorCapabilities.HEADER, "beauty-v2"))
        .andExpect(status().isConflict());
    verifyNoMoreInteractions(service);
  }

  /** Confere fila filtrada antes do limite, sem consultar ZIP nem filtrar resultados em memória. */
  @Test
  void passesInstalledProfilesToDatabaseAndSkipsEmptyCatalog() {
    var artifacts = mock(KitPrivateArtifactRepository.class);
    var service = service(artifacts, mock(LearningSalesCycleRepository.class));
    assertThat(service.pending(Set.of())).isEmpty();
    verifyNoInteractions(artifacts);
    var profiles = Set.of("hair-v2", "nails-v1");
    assertThat(service.pending(profiles)).isEmpty();
    verify(artifacts).pending(eq(profiles), argThat(p -> p.getPageSize() == 3));
    verifyNoMoreInteractions(artifacts);
  }

  /** Protege duas identidades e conserva claim idempotente do executor compatível. */
  @Test
  void staleOrUnsupportedClaimsLeaveReservationUntouched() {
    for (long product : List.of(7L, 8048L)) {
      var artifacts = mock(KitPrivateArtifactRepository.class);
      var cycles = mock(LearningSalesCycleRepository.class);
      var service = service(artifacts, cycles);
      var artifact = new KitPrivateArtifact();
      artifact.setId(UUID.randomUUID().toString());
      artifact.setProductId(product);
      artifact.setCycleId(product + 100);
      artifact.setExperimentId(product + 200);
      artifact.setPrototypeVersion("candidate-v2");
      artifact.setProfileCode("hair-v2");
      artifact.setInputJson("{}");
      artifact.setStatus("QUEUED");
      when(artifacts.locked(artifact.getId())).thenReturn(Optional.of(artifact));
      var claim = new Claim(UUID.randomUUID());
      for (Set<String> profiles : List.of(Set.<String>of(), Set.of("nails-v1"))) {
        assertThatThrownBy(() -> service.claim(artifact.getId(), claim, profiles))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("não declarou suporte");
        assertThat(artifact.getStatus()).isEqualTo("QUEUED");
        assertThat(artifact.getClaimKey()).isNull();
        assertThat(artifact.getStartedAt()).isNull();
      }
      verify(artifacts, never()).saveAndFlush(any());
      verifyNoInteractions(cycles);
      var cycle = new LearningSalesCycle();
      cycle.setStatus("OPEN");
      when(cycles.findById(artifact.getCycleId())).thenReturn(Optional.of(cycle));
      assertThat(service.claim(artifact.getId(), claim, Set.of("hair-v2")).id())
          .isEqualTo(artifact.getId());
      assertThat(artifact.getStatus()).isEqualTo("RUNNING");
      var started = artifact.getStartedAt();
      service.claim(artifact.getId(), claim, Set.of("hair-v2"));
      assertThat(artifact.getStartedAt()).isEqualTo(started);
      verify(artifacts, times(1)).saveAndFlush(artifact);
    }
  }

  /** Recusa códigos inexistentes, famílias, campos vazios e declarações excessivas. */
  @Test
  void validatesCapabilitiesWithoutInferringFromProductIdentity() {
    assertThat(KitCompositorCapabilities.parse(null)).isEmpty();
    assertThat(KitCompositorCapabilities.parse("nails-v1, hair-v2,nails-v1"))
        .containsExactlyInAnyOrder("nails-v1", "hair-v2");
    for (String invalid : List.of("unknown", "beauty-v2", "hair-v2,", "x".repeat(513)))
      assertThatThrownBy(() -> KitCompositorCapabilities.parse(invalid))
          .isInstanceOf(ResponseStatusException.class);
  }

  /** Monta a fronteira real de reserva com persistência simulada e sem credenciais externas. */
  private KitPrivateService service(
      KitPrivateArtifactRepository artifacts, LearningSalesCycleRepository cycles) {
    return new KitPrivateService(
        mock(KitPrivateArtifactStorage.class),
        mock(KitPrivateSessionRepository.class),
        artifacts,
        cycles,
        mock(KitPrototypeCapabilities.class),
        mock(KitArtifactContract.class),
        new ObjectMapper(),
        "local-fixture");
  }
}
