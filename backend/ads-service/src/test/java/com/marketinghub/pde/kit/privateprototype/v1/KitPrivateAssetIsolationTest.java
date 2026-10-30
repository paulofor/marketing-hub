package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import com.marketinghub.repository.jpa.kit.*;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: conferir o acesso ao resultado por sessão, especialidade e cenário exatos. */
class KitPrivateAssetIsolationTest {
  /** Preserva perfis históricos e o novo caso multiperfil, recusando acesso cruzado. */
  @ParameterizedTest
  @CsvSource({
    "nails-v1,nails-v1,ADHERENT",
    "barber-v1,barber-v1,ADHERENT",
    "beauty-v1,hair-v1,ADHERENT:hair-v1"
  })
  void servesOwnArtifactAndRefusesAnotherSpecialtyOrExperiment(
      String family, String selected, String owner) throws Exception {
    var sessions = mock(KitPrivateSessionRepository.class);
    var artifacts = mock(KitPrivateArtifactRepository.class);
    var contract = mock(KitArtifactContract.class);
    var json = new ObjectMapper();
    var session = new KitPrivateSession();
    session.setId("private-test");
    session.setArtifactId("artifact-test");
    session.setCycleId(15L);
    session.setProductId(7L);
    session.setExperimentId(108L);
    session.setPrototypeVersion("beauty-test-v1");
    session.setScenarioCode("ADHERENT");
    session.setExpiresAt(Instant.now().plusSeconds(3600));
    session.setPayloadJson(
        json.createObjectNode()
            .put("profileCode", family)
            .set("input", json.createObjectNode().put("specialtyCode", selected))
            .toString());
    var artifact = new KitPrivateArtifact();
    artifact.setId("artifact-test");
    artifact.setCycleId(15L);
    artifact.setProductId(7L);
    artifact.setExperimentId(108L);
    artifact.setPrototypeVersion("beauty-test-v1");
    artifact.setFixtureOwner(owner);
    artifact.setProfileCode(selected);
    artifact.setStatus("READY");
    artifact.setZipBytes(new byte[] {1});
    when(sessions.findBySessionHash(anyString())).thenReturn(Optional.of(session));
    when(artifacts.findById("artifact-test")).thenReturn(Optional.of(artifact));
    when(contract.image(any(), eq("post-01.png"))).thenReturn(new byte[] {2});
    var service =
        new KitPrivateService(
            new com.marketinghub.pde.kit.privateprototype.v1.service.KitPrivateArtifactStorage(
                org.mockito.Mockito.mock(
                    com.marketinghub.repository.jpa.kit.KitPrivateArtifactChunkRepository.class)),
            sessions,
            artifacts,
            null,
            null,
            contract,
            json,
            "private-test");
    assertThat(service.asset("private-access", "post-01.png")).containsExactly((byte) 2);
    artifact.setProfileCode("different-v1");
    assertThatThrownBy(() -> service.asset("private-access", "post-01.png"))
        .hasMessageContaining("incompatível");
    artifact.setProfileCode(selected);
    artifact.setFixtureOwner("RECOVERY");
    assertThatThrownBy(() -> service.asset("private-access", "post-01.png"))
        .hasMessageContaining("incompatível");
    artifact.setFixtureOwner(owner);
    artifact.setExperimentId(109L);
    assertThatThrownBy(() -> service.asset("private-access", "post-01.png"))
        .hasMessageContaining("incompatível");
  }
}
