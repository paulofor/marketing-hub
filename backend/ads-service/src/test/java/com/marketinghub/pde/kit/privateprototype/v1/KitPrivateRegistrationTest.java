package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.Capability;
import com.marketinghub.repository.jpa.kit.KitPrivateArtifactRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir prova declarada sem pacote aceito do mesmo contexto executável. */
class KitPrivateRegistrationTest {
  /** Confere a identidade original e outra identidade/perfil sem inferência por nome ou ID. */
  @Test
  void requiresAcceptedPackageForEachExactContext() {
    for (String profile : List.of("nails-v1", "barber-v1")) {
      var cycle = new LearningSalesCycle();
      cycle.setId(profile.equals("nails-v1") ? 7107L : 8218L);
      cycle.setProductId(profile.equals("nails-v1") ? 8107L : 9218L);
      cycle.setExperimentId(profile.equals("nails-v1") ? 9107L : 10218L);
      cycle.setProductVersion("private-test-" + profile);
      var capabilities = mock(KitPrototypeCapabilities.class);
      var artifacts = mock(KitPrivateArtifactRepository.class);
      String url = "https://private.example/mh-api/pde/kit/private/v1/prototype";
      when(capabilities.resolve(cycle)).thenReturn(new Capability(true, profile, "Teste", url));
      var service = new KitPrivateService(null, artifacts, null, capabilities, null, new ObjectMapper(), "local-test");
      var proof = new ObjectMapper().createObjectNode().put("profileCode", profile).put("privateAccessUrl", url);
      assertThatThrownBy(() -> service.validateRegistration(cycle, proof)).hasMessageContaining("pacote utilizável");
      when(artifacts.existsByCycleIdAndProductIdAndExperimentIdAndPrototypeVersionAndProfileCodeAndStatus(
          cycle.getId(), cycle.getProductId(), cycle.getExperimentId(), cycle.getProductVersion(), profile, "READY"))
          .thenReturn(true);
      service.validateRegistration(cycle, proof);
      proof.put("profileCode", "unsupported-v1");
      assertThatThrownBy(() -> service.validateRegistration(cycle, proof)).hasMessageContaining("perfil e à URL");
      proof.put("profileCode", profile).put("privateAccessUrl", "https://other.example/prototype");
      assertThatThrownBy(() -> service.validateRegistration(cycle, proof)).hasMessageContaining("perfil e à URL");
    }
  }
}
