package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.customeragent.memory.CustomerAgentMemoryProperties;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskVisualEvidenceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/** Responsabilidade: proteger persistência privada, idempotência e segregação da prova visual. */
@ExtendWith(MockitoExtension.class)
class AgentTaskVisualEvidenceServiceTest {
  @Mock private S3Client s3;
  @Mock private AgentTaskRepository taskRepository;
  @Mock private AgentTaskVisualEvidenceRepository evidenceRepository;
  @Mock private AgentTaskTargetContextProvider targetContextProvider;
  private AgentTaskVisualEvidenceService service;
  private AgentTask task;

  /** Configura bucket privado, relógio fixo e lease de Psique para cada teste. */
  @BeforeEach
  void setUp() {
    CustomerAgentMemoryProperties properties = new CustomerAgentMemoryProperties();
    properties.setBucket("customer-agent-test");
    properties.setPrefix("customer-agent-memory/v1");
    Agent agent = new Agent();
    agent.setAgentKey("customer-agent");
    task = new AgentTask();
    task.setId(258L);
    task.setAssignedAgent(agent);
    task.setStatus("IN_PROGRESS");
    task.setSourceReference("experiment:89@v6:customer");
    lenient()
        .when(targetContextProvider.resolve("experiment:89@v6:customer", null))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    "experiment:89@v6:customer",
                    89L,
                    9L,
                    "rigel",
                    "Agenda Cheia",
                    "Rigel",
                    "rigel-v2",
                    "https://rigel.example/jornada",
                    "https://checkout.example/rigel",
                    new java.math.BigDecimal("349.00"))));
    service =
        new AgentTaskVisualEvidenceService(
            properties,
            s3,
            taskRepository,
            evidenceRepository,
            targetContextProvider,
            Clock.fixed(Instant.parse("2026-08-29T10:05:00Z"), ZoneOffset.UTC));
  }

  /** Persiste PNG criptografado e expõe somente a rota governada vinculada à tarefa. */
  @Test
  void storesEncryptedSnapshotWithImmutableMetadata() throws Exception {
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));
    when(evidenceRepository.findByTaskIdAndCaptureSessionIdAndEvidenceKey(
            258L, "capture-abc", "page-1-fold-1"))
        .thenReturn(Optional.empty());
    when(evidenceRepository.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              AgentTaskVisualEvidence value = invocation.getArgument(0);
              value.setId(901L);
              return value;
            });

    AgentTaskVisualEvidenceResponse response =
        service.store("customer-agent", 258L, foldRequest(), png("fold-1"));

    assertThat(response.id()).isEqualTo(901L);
    assertThat(response.label()).isEqualTo("Página 1 · dobra 1");
    assertThat(response.contentUrl()).isEqualTo("/api/agent-tasks/258/visual-evidence/901/content");
    assertThat(response.sha256()).hasSize(64);
    ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3).putObject(request.capture(), any(RequestBody.class));
    assertThat(request.getValue().bucket()).isEqualTo("customer-agent-test");
    assertThat(request.getValue().key())
        .startsWith("customer-agent-memory/v1/task-visual-evidence/task-258/capture-abc/");
    assertThat(request.getValue().serverSideEncryption()).isEqualTo(ServerSideEncryption.AES256);
  }

  /** Impede um agente diferente de anexar pixels à tarefa reservada por Psique. */
  @Test
  void rejectsCrossAgentUploadBeforeStorage() {
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));

    assertThatThrownBy(() -> service.store("meta-ad-approver", 258L, foldRequest(), png("fold-1")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("403 FORBIDDEN");
    verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  /** Bloqueia arquivo que declara PNG sem carregar a assinatura real desse formato. */
  @Test
  void rejectsInvalidPixelsBeforeStorage() {
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));

    assertThatThrownBy(
            () ->
                service.store(
                    "customer-agent",
                    258L,
                    foldRequest(),
                    new MockMultipartFile("file", "fold.png", "image/png", "not-png".getBytes())))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Snapshot PNG inválido");
    verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  /** Impede que metadados forjados apontem a captura para uma rede privada. */
  @Test
  void rejectsPrivateSourceBeforeStorage() {
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));
    AgentTaskVisualEvidenceRequest original = foldRequest();
    AgentTaskVisualEvidenceRequest privateRequest =
        new AgentTaskVisualEvidenceRequest(
            original.captureSessionId(),
            original.evidenceKey(),
            original.evidenceType(),
            original.deviceProfile(),
            original.pageNumber(),
            original.foldNumber(),
            original.viewportWidth(),
            original.viewportHeight(),
            original.pageHeightPx(),
            original.scrollY(),
            "http://10.0.0.8/jornada",
            original.finalUrl(),
            original.capturedAt());

    assertThatThrownBy(() -> service.store("customer-agent", 258L, privateRequest, png("fold-1")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("URL solicitada inválida");
    verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  /** Impede anexar à tarefa de Rigel uma captura pública pertencente a outro produto. */
  @Test
  void rejectsSnapshotFromAnotherProduct() {
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));
    AgentTaskVisualEvidenceRequest original = foldRequest();
    AgentTaskVisualEvidenceRequest otherProduct =
        new AgentTaskVisualEvidenceRequest(
            original.captureSessionId(),
            original.evidenceKey(),
            original.evidenceType(),
            original.deviceProfile(),
            original.pageNumber(),
            original.foldNumber(),
            original.viewportWidth(),
            original.viewportHeight(),
            original.pageHeightPx(),
            original.scrollY(),
            "https://vega.example/jornada",
            "https://vega.example/jornada",
            original.capturedAt());

    assertThatThrownBy(() -> service.store("customer-agent", 258L, otherProduct, png("fold-1")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não pertence ao produto e à versão");
    verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  /** Impede reaproveitar a mesma chave e os mesmos pixels com URL ou posição adulterada. */
  @Test
  void rejectsIdempotencyKeyWithDifferentMetadata() throws Exception {
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));
    MockMultipartFile pixels = png("fold-1");
    AgentTaskVisualEvidenceRequest request = foldRequest();
    AgentTaskVisualEvidence existing = new AgentTaskVisualEvidence();
    existing.setTask(task);
    existing.setSha256(sha256(pixels.getBytes()));
    existing.setEvidenceType(request.evidenceType());
    existing.setDeviceProfile(request.deviceProfile());
    existing.setPageNumber(request.pageNumber());
    existing.setFoldNumber(request.foldNumber());
    existing.setViewportWidth(request.viewportWidth());
    existing.setViewportHeight(request.viewportHeight());
    existing.setPageHeightPx(request.pageHeightPx());
    existing.setScrollY(request.scrollY());
    existing.setSourceUrl(request.sourceUrl());
    existing.setFinalUrl("https://outro-produto.example/jornada");
    existing.setCapturedAt(request.capturedAt());
    existing.setSizeBytes(pixels.getSize());
    when(evidenceRepository.findByTaskIdAndCaptureSessionIdAndEvidenceKey(
            258L, request.captureSessionId(), request.evidenceKey()))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.store("customer-agent", 258L, request, pixels))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("conteúdo ou metadados diferentes");
    verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  /** Monta os metadados canônicos de uma dobra mobile da primeira página. */
  private AgentTaskVisualEvidenceRequest foldRequest() {
    return new AgentTaskVisualEvidenceRequest(
        "capture-abc",
        "page-1-fold-1",
        "FOLD",
        "IPHONE_15_PRO",
        1,
        1,
        393,
        852,
        1704,
        0,
        "https://rigel.example/jornada",
        "https://rigel.example/jornada",
        Instant.parse("2026-08-29T10:00:00Z"));
  }

  /** Produz bytes de teste com assinatura PNG válida e conteúdo variável. */
  private MockMultipartFile png(String value) {
    try {
      var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
      image.setRGB(0, 0, value.hashCode());
      var output = new java.io.ByteArrayOutputStream();
      ImageIO.write(image, "png", output);
      return new MockMultipartFile("file", "fold.png", "image/png", output.toByteArray());
    } catch (java.io.IOException ex) {
      throw new IllegalStateException("Falha ao criar PNG válido para o teste.", ex);
    }
  }

  /** Calcula o hash usado para simular uma repetição de upload já persistida. */
  private String sha256(byte[] bytes) throws Exception {
    return java.util.HexFormat.of()
        .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
