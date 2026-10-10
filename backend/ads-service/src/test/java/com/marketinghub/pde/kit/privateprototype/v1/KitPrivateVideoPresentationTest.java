package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleVideoBinding;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitPrivateService;
import com.marketinghub.repository.jpa.kit.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: comprovar apresentação audiovisual autenticada e isolada sem impedir o kit. */
class KitPrivateVideoPresentationTest {
  private final ObjectMapper json = new ObjectMapper();
  private final KitPrivateSessionRepository sessions = mock(KitPrivateSessionRepository.class);
  private final KitPrivateArtifactRepository artifacts = mock(KitPrivateArtifactRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningCycleVideoBinding videos = mock(LearningCycleVideoBinding.class);
  private final KitPrivateService service =
      new KitPrivateService(sessions, artifacts, cycles, null, null, json, "local-only");
  private final KitPrivateSession session = new KitPrivateSession();
  private final LearningSalesCycle cycle = new LearningSalesCycle();

  /** Preserva um acesso sintético já emitido no mesmo contexto, sem composição ou inferência. */
  @BeforeEach
  void setup() {
    session.setId("fixture-session");
    session.setProductId(7L);
    session.setCycleId(12L);
    session.setExperimentId(105L);
    session.setPrototypeVersion("private-v3");
    session.setExpiresAt(Instant.now().plusSeconds(3600));
    session.setPayloadJson("{\"status\":\"INPUT\",\"profileCode\":\"nails-v1\",\"events\":[]}");
    cycle.setId(12L);
    cycle.setProductId(7L);
    cycle.setExperimentId(105L);
    cycle.setProductVersion("private-v3");
    when(sessions.findBySessionHash(anyString())).thenReturn(Optional.of(session));
    when(cycles.findById(12L)).thenReturn(Optional.of(cycle));
    ReflectionTestUtils.setField(service, "videoBinding", videos);
  }

  /** Apresenta o recibo corrente sem modificar aceites, entrada ou arquivos da sessão. */
  @Test
  void presentsCurrentReceiptWithoutWrites() {
    var receipt = json.createObjectNode().put("integrationFingerprint", "a".repeat(64));
    when(videos.presentation(12L, "private-v3")).thenReturn(Optional.of(receipt));
    var response = service.session("synthetic-session-token");
    assertThat(response.videoIntegration()).isEqualTo(receipt);
    assertThat(response.status()).isEqualTo("INPUT");
    verify(sessions, never()).save(any());
    verifyNoInteractions(artifacts);
  }

  /** Recusa contexto divergente antes de procurar o vínculo audiovisual. */
  @ParameterizedTest
  @ValueSource(strings = {"product", "experiment", "version", "cycle"})
  void preservesSessionWithoutForeignReceipt(String changed) {
    switch (changed) {
      case "product" -> cycle.setProductId(9202L);
      case "experiment" -> cycle.setExperimentId(9203L);
      case "version" -> cycle.setProductVersion("other-version");
      default -> when(cycles.findById(12L)).thenReturn(Optional.empty());
    }
    assertThat(service.session("synthetic-session-token").videoIntegration()).isNull();
    verifyNoInteractions(videos);
  }

  /** Mantém a experiência acessível quando a integração é ausente ou deixou de ser válida. */
  @Test
  void recoversWithoutInvalidIntegration() {
    when(videos.presentation(12L, "private-v3"))
        .thenThrow(new IllegalStateException("Mídia alterada"));
    var response = service.session("synthetic-session-token");
    assertThat(response.videoIntegration()).isNull();
    assertThat(response.status()).isEqualTo("INPUT");
    assertThat(response.productId()).isEqualTo(7L);
    verify(sessions, never()).save(any());
  }

  /** Uma versão sem integração continua utilizável sem produzir ou aprovar mídias. */
  @Test
  void preservesPreviouslyValidSessionWithoutReceipt() {
    when(videos.presentation(12L, "private-v3")).thenReturn(Optional.empty());
    assertThat(service.session("synthetic-session-token").videoIntegration()).isNull();
    verifyNoInteractions(artifacts);
  }
}
