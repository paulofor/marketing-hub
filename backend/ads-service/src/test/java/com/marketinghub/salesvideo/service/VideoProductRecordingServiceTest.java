package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.marketinghub.repository.jpa.salesvideo.*;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.service.clipplan.VideoProductionSpendingPolicy;
import com.marketinghub.salesvideo.tenant.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: impedir prova cruzada ou análise paga ao vincular uma gravação funcional pelo
 * Estúdio.
 */
class VideoProductRecordingServiceTest {
  final ObjectMapper json = new ObjectMapper();
  final VideoProjectRepository projects = mock(VideoProjectRepository.class);
  final VideoProductRecordingRepository recordings = mock(VideoProductRecordingRepository.class);
  final VideoProductionSpendingPolicy spending = mock(VideoProductionSpendingPolicy.class);
  final VideoProductRecordingService service =
      new VideoProductRecordingService(recordings, projects, spending, json);
  final VideoProject project = new VideoProject();
  final Map<Long, VideoProductRecording> rows = new HashMap<>();
  final byte[] bytes = new byte[] {0, 0, 0, 24, 102, 116, 121, 112, 105, 115, 111, 109, 0, 0, 0, 0};

  /** Usa apenas cadastro sintético, com repositório de bytes simulado e nenhum cliente externo. */
  @BeforeEach
  void setup() {
    TenantContextHolder.set(new TenantContext("fixture-tenant", "fixture@sandbox.local", false));
    project.setId(91014L);
    project.setProductId(91004L);
    project.setExperimentId(91114L);
    project.setCampaignKey("target-video-version");
    project.setTenantId("fixture-tenant");
    when(projects.findByIdForUpdate(91014L)).thenReturn(Optional.of(project));
    when(projects.findById(91014L)).thenReturn(Optional.of(project));
    when(recordings.findByVideoProjectIdAndSha256(anyLong(), anyString()))
        .thenAnswer(
            a ->
                rows.values().stream()
                    .filter(
                        r ->
                            r.getVideoProjectId().equals(a.getArgument(0))
                                && r.getSha256().equals(a.getArgument(1)))
                    .findFirst());
    when(recordings.findById(anyLong()))
        .thenAnswer(a -> Optional.ofNullable(rows.get(a.getArgument(0))));
    when(recordings.saveAndFlush(any()))
        .thenAnswer(
            a -> {
              var row = (VideoProductRecording) a.getArgument(0);
              row.setId(91001L);
              rows.put(row.getId(), row);
              return row;
            });
  }

  /** Limpa o tenant de teste sem tocar contexto externo. */
  @AfterEach
  void clear() {
    TenantContextHolder.clear();
  }

  /** Reenvio reutiliza os mesmos bytes e o GET privado não publica nem chama fila de análise. */
  @Test
  void storesAndReusesOriginalBytesAndManifest() throws Exception {
    var manifest = manifest();
    var upload = file();
    var result = service.upload(91014L, upload, manifest.toString());
    assertThat(service.upload(91014L, upload, manifest.toString())).isEqualTo(result);
    verify(recordings, times(1)).saveAndFlush(any());
    verify(spending, times(1)).validateProductRecording(eq(project), eq(manifest));
    assertThat(service.read(91014L)).isEqualTo(bytes);
    assertThat(result)
        .containsEntry("qualityApproved", false)
        .containsEntry("commercialEvidenceClaimed", false)
        .containsEntry("sourcePrototypeVersion", "accepted-product-version")
        .containsEntry("productVersion", "target-video-version");
    assertThat(project.getReferencePerformanceUri()).isEqualTo("internal://video-recordings/91001");
    manifest.put("sourcePrototypeVersion", "another-source");
    assertThatThrownBy(() -> service.upload(91014L, upload, manifest.toString()))
        .hasMessageContaining("outro manifesto");
  }

  /** Isola tenant, produto, hash, cronologia e origem sem gravar prova inválida. */
  @Test
  void rejectsForeignAndForgedEvidence() throws Exception {
    var manifest = manifest();
    manifest.put("productId", 5);
    assertThatThrownBy(() -> service.upload(91014L, file(), manifest.toString()))
        .isInstanceOf(ResponseStatusException.class);
    verifyNoInteractions(spending, recordings);
    manifest.put("productId", 91004).put("sha256", "00");
    assertThatThrownBy(() -> service.upload(91014L, file(), manifest.toString()))
        .hasMessageContaining("integridade");
    TenantContextHolder.set(new TenantContext("another-tenant", "fixture@sandbox.local", false));
    assertThatThrownBy(() -> service.upload(91014L, file(), manifest.toString()))
        .hasMessageContaining("404");
  }

  /** Não permite que um nome de arquivo ou um estado final estático finja a ação de reabrir. */
  @Test
  void refusesMissingTemporalActionAndProviderGate() throws Exception {
    var manifest = manifest();
    ((com.fasterxml.jackson.databind.node.ArrayNode) manifest.path("actions")).remove(3);
    assertThatThrownBy(() -> service.upload(91014L, file(), manifest.toString()))
        .hasMessageContaining("salvar e reabrir");
    doThrow(new IllegalStateException("Recibo não vigente"))
        .when(spending)
        .validateProductRecording(any(), any());
    var valid = manifest();
    assertThatThrownBy(() -> service.upload(91014L, file(), valid.toString()))
        .hasMessageContaining("não vigente");
    verify(recordings, never()).saveAndFlush(any());
  }

  /** Monta manifesto sintético de quatro ações; o worker ainda precisa verificar o codec real. */
  private com.fasterxml.jackson.databind.node.ObjectNode manifest() throws Exception {
    var node =
        json.createObjectNode()
            .put("contractVersion", "PRODUCT_FUNCTIONAL_RECORDING_V1")
            .put("productId", 91004)
            .put("sourcePrototypeVersion", "accepted-product-version")
            .put("captureEnvironment", "LOCAL_SANDBOX")
            .put("trafficClass", "AGENT_VALIDATION")
            .put("syntheticData", true)
            .put("commercialEvidenceClaimed", false)
            .put("qualityApproved", false)
            .put("sourceFingerprint", "a".repeat(64))
            .put("durationSeconds", 11)
            .put("width", 540)
            .put("height", 1170)
            .put("videoCodec", "h264")
            .put(
                "sha256",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    var actions = node.putArray("actions");
    int at = 1;
    for (String action :
        List.of("INPUT_SUBMITTED", "RESULT_VISIBLE", "SAVE_AND_COMPLETE", "REOPEN_SAVED"))
      actions.addObject().put("action", action).put("seconds", at++);
    return node;
  }

  /**
   * Entrega MP4 mínimo somente ao teste de persistência; nenhuma validade audiovisual é simulada
   * como aprovação.
   */
  private MockMultipartFile file() {
    return new MockMultipartFile("file", "local.mp4", "video/mp4", bytes);
  }
}
