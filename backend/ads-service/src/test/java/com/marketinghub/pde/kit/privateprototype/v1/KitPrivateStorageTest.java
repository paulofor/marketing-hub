package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.BinaryResult;
import com.marketinghub.repository.jpa.kit.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: prevenir perda de arquivos e auditoria por limite SQL ou callback repetido. */
class KitPrivateStorageTest {
  /** Partes de outro artefato, incompletas ou alteradas nunca podem produzir download. */
  @Test
  void validatesPartsAndPreservesLegacy() {
    var chunks = mock(KitPrivateArtifactChunkRepository.class);
    var storage = new KitPrivateArtifactStorage(chunks);
    var artifact = new KitPrivateArtifact();
    artifact.setId("a");
    byte[] bytes = new byte[2 * KitPrivateArtifactStorage.PART_BYTES + 7];
    new Random(42).nextBytes(bytes);
    artifact.setZipSha256(KitArtifactContract.hash(bytes));
    var saved = new ArrayList<KitPrivateArtifactChunk>();
    when(chunks.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              saved.add(call.getArgument(0));
              return call.getArgument(0);
            });
    when(chunks.findByArtifactIdOrderByPartNumberAsc("a")).thenReturn(saved);
    storage.store(artifact, bytes);
    assertThat(saved).hasSize(3);
    assertThat(artifact.getZipBytes()).isNull();
    assertThat(storage.read(artifact)).isEqualTo(bytes);
    saved.get(1).setArtifactId("other");
    assertThatThrownBy(() -> storage.read(artifact)).hasMessageContaining("inconsistentes");
    saved.get(1).setArtifactId("a");
    saved.remove(2);
    assertThatThrownBy(() -> storage.read(artifact)).hasMessageContaining("Integridade");
    artifact.setZipBytes(bytes);
    assertThat(storage.read(artifact)).isEqualTo(bytes);
  }

  /** Falhas de persistência permanecem recuperáveis; contrato inválido continua recusado. */
  @Test
  void distinguishesPersistenceFromInvalidContent() throws Exception {
    var fixture = fixture("RUNNING");
    doThrow(new DataAccessResourceFailureException("packet exceeds limit"))
        .when(fixture.storage)
        .store(any(), any());
    assertThatThrownBy(
            () -> fixture.service.completeBinary("artifact", fixture.result, fixture.bytes))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode().value()).isEqualTo(503));
    assertThat(fixture.artifact.getStatus()).isEqualTo("RUNNING");
    verify(fixture.artifacts, never()).saveAndFlush(any());
    reset(fixture.storage);
    when(fixture.contract.validate(any())).thenThrow(new java.io.IOException("invalid ZIP"));
    assertThatThrownBy(
            () -> fixture.service.completeBinary("artifact", fixture.result, fixture.bytes))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode().value()).isEqualTo(409));
    verifyNoInteractions(fixture.storage);
  }

  /** Reenvio autenticado preserva a falha original, não reabre ciclo e não duplica o pacote. */
  @Test
  void replaysOriginalClaimOnlyInOpenCycle() throws Exception {
    var f = fixture("FAILED");
    var finished = Instant.parse("2026-10-29T00:00:00Z");
    f.artifact.setFinishedAt(finished);
    f.artifact.setErrorMessage("Falha original preservada");
    f.cycle.setStatus("CLOSED");
    assertThatThrownBy(() -> f.service.completeBinary("artifact", f.result, f.bytes))
        .hasMessageContaining("encerrado");
    f.cycle.setStatus("OPEN");
    var wrong = new BinaryResult(UUID.randomUUID(), f.result.zipSha256(), 0, null);
    assertThatThrownBy(() -> f.service.completeBinary("artifact", wrong, f.bytes))
        .hasMessageContaining("tentativa reservada");
    f.service.completeBinary("artifact", f.result, f.bytes);
    f.service.completeBinary("artifact", f.result, f.bytes);
    assertThat(f.artifact.getStatus()).isEqualTo("READY");
    assertThat(f.artifact.getFinishedAt()).isEqualTo(finished);
    assertThat(f.artifact.getErrorMessage()).isEqualTo("Falha original preservada");
    assertThat(f.artifact.getRecoveredAt()).isNotNull();
    verify(f.storage).store(f.artifact, f.bytes);
  }

  /** Monta uma tentativa sintética sem segredo, provedor externo ou resultado comercial. */
  private Fixture fixture(String status) throws Exception {
    var artifacts = mock(KitPrivateArtifactRepository.class);
    var cycles = mock(LearningSalesCycleRepository.class);
    var storage = mock(KitPrivateArtifactStorage.class);
    var contract = mock(KitArtifactContract.class);
    var artifact = new KitPrivateArtifact();
    artifact.setId("artifact");
    artifact.setCycleId(17L);
    artifact.setStatus(status);
    artifact.setProfileCode("barber-v1");
    var claim = UUID.randomUUID();
    artifact.setClaimKey(claim.toString());
    var cycle = new LearningSalesCycle();
    cycle.setStatus("OPEN");
    when(cycles.findById(17L)).thenReturn(Optional.of(cycle));
    when(artifacts.locked("artifact")).thenReturn(Optional.of(artifact));
    when(contract.validate(any())).thenReturn(new ObjectMapper().createObjectNode());
    var service =
        new KitPrivateService(
            storage, null, artifacts, cycles, null, contract, new ObjectMapper(), "local-only");
    byte[] bytes = {1, 2, 3};
    return new Fixture(
        service,
        storage,
        artifacts,
        contract,
        artifact,
        cycle,
        new BinaryResult(claim, KitArtifactContract.hash(bytes), 0, null),
        bytes);
  }

  /** Responsabilidade: reunir somente dependências e dados da regressão local. */
  private record Fixture(
      KitPrivateService service,
      KitPrivateArtifactStorage storage,
      KitPrivateArtifactRepository artifacts,
      KitArtifactContract contract,
      KitPrivateArtifact artifact,
      LearningSalesCycle cycle,
      BinaryResult result,
      byte[] bytes) {}
}
