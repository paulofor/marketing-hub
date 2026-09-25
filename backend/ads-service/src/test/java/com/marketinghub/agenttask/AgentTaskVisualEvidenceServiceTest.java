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

  /** Valida no serviço real os PNGs produzidos pelo navegador oficial do worker na sandbox. */
  @Test
  @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(
      named = "PSIQUE_CAPTURE_EVIDENCE_OUTPUT",
      matches = ".+")
  void acceptsRealWorkerCaptureThroughBackendStorageContract() throws Exception {
    var json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    var capture =
        json.readTree(
            java.nio.file.Path.of(System.getenv("PSIQUE_CAPTURE_EVIDENCE_OUTPUT"), "capture.json")
                .toFile());
    var process = new com.marketinghub.businessprocess.BusinessProcessDefinition();
    process.setProcessCode("quartzo-commercial-preparation-v1");
    task.setProcessDefinition(process);
    task.setProcessActivityId("humanExperienceReview");
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));
    when(targetContextProvider.resolve(task.getSourceReference(), process.getProcessCode()))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    task.getSourceReference(),
                    504L,
                    208L,
                    "kit-receitas",
                    "Pacote sintético de receitas",
                    "Sintético",
                    "v4",
                    capture.path("pages").get(0).path("requestedUrl").asText(),
                    null,
                    null,
                    capture.path("pages").get(1).path("requestedUrl").asText(),
                    new java.math.BigDecimal("39.00"))));
    var ids = new java.util.concurrent.atomic.AtomicLong(1200);
    when(evidenceRepository.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              AgentTaskVisualEvidence evidence = invocation.getArgument(0);
              evidence.setId(ids.incrementAndGet());
              return evidence;
            });
    assertThat(capture.path("artifacts").size()).isEqualTo(4);
    for (var artifact : capture.path("artifacts")) {
      var request =
          new AgentTaskVisualEvidenceRequest(
              artifact.path("captureSessionId").asText(),
              artifact.path("evidenceKey").asText(),
              artifact.path("evidenceType").asText(),
              artifact.path("deviceProfile").asText(),
              artifact.path("pageNumber").asInt(),
              artifact.path("foldNumber").isNull() ? null : artifact.path("foldNumber").asInt(),
              artifact.path("viewportWidth").asInt(),
              artifact.path("viewportHeight").asInt(),
              artifact.path("pageHeightPx").asInt(),
              artifact.path("scrollY").asInt(),
              artifact.path("sourceUrl").asText(),
              artifact.path("finalUrl").asText(),
              Instant.parse(artifact.path("capturedAt").asText()));
      byte[] pixels =
          java.nio.file.Files.readAllBytes(
              java.nio.file.Path.of(artifact.path("localPath").asText()));
      var stored =
          service.store(
              "customer-agent",
              258L,
              request,
              new MockMultipartFile("file", "capture.png", "image/png", pixels));
      assertThat(stored.pageNumber()).isEqualTo(request.pageNumber());
      assertThat(stored.sha256())
          .isEqualTo(
              java.util.HexFormat.of()
                  .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(pixels)));
    }
  }

  /** Aceita checkout só na página prevista, no mesmo produto e com a preferência exata. */
  @Test
  void storesOnlyCanonicalQuartzoCheckoutOnSecondPage() throws Exception {
    var process = new com.marketinghub.businessprocess.BusinessProcessDefinition();
    process.setProcessCode("quartzo-commercial-preparation-v1");
    task.setProcessDefinition(process);
    task.setProcessActivityId("humanExperienceReview");
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));
    when(targetContextProvider.resolve(task.getSourceReference(), process.getProcessCode()))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    task.getSourceReference(),
                    406L,
                    204L,
                    "kit-independente",
                    "Kit",
                    "Produto sintético",
                    "v3",
                    "https://example.com/kit",
                    null,
                    null,
                    "https://example.com/checkout?pref_id=approved",
                    new java.math.BigDecimal("39.00"))));
    when(evidenceRepository.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              AgentTaskVisualEvidence evidence = invocation.getArgument(0);
              evidence.setId(901L);
              return evidence;
            });
    var accepted =
        service.store(
            "customer-agent",
            258L,
            checkoutRequest(2, "https://example.com/checkout?pref_id=approved"),
            png("checkout"));
    assertThat(accepted.pageNumber()).isEqualTo(2);
    assertThat(accepted.sourceUrl()).endsWith("pref_id=approved");
    for (var invalid :
        java.util.List.of(
            checkoutRequest(1, "https://example.com/checkout?pref_id=approved"),
            checkoutRequest(2, "https://example.com/checkout?pref_id=another-product"),
            checkoutRequest(3, "https://example.com/checkout?pref_id=approved"))) {
      assertThatThrownBy(() -> service.store("customer-agent", 258L, invalid, png("invalid")))
          .isInstanceOf(ResponseStatusException.class);
    }
  }

  /** Mantém os outros processos restritos à URL pública do próprio alvo. */
  @Test
  void doesNotEnableCheckoutForUnrelatedProcess() {
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));
    assertThatThrownBy(
            () ->
                service.store(
                    "customer-agent",
                    258L,
                    checkoutRequest(2, "https://checkout.example/rigel"),
                    png("unrelated")))
        .hasMessageContaining("snapshot não pertence");
    verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  /** Cria prova de checkout com metadados completos e sem dados de pagamento. */
  private AgentTaskVisualEvidenceRequest checkoutRequest(int page, String url) {
    return new AgentTaskVisualEvidenceRequest(
        "checkout-test",
        "page-" + page + "-fold-1",
        "FOLD",
        "IPHONE_15_PRO",
        page,
        1,
        393,
        852,
        852,
        0,
        url,
        url,
        Instant.parse("2026-08-29T10:00:00Z"));
  }

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
                    null,
                    null,
                    "https://checkout.example/rigel",
                    new java.math.BigDecimal("349.00"))));
    service =
        new AgentTaskVisualEvidenceService(
            properties,
            s3,
            taskRepository,
            evidenceRepository,
            targetContextProvider,
            new com.fasterxml.jackson.databind.ObjectMapper(),
            Clock.fixed(Instant.parse("2026-08-29T10:05:00Z"), ZoneOffset.UTC));
  }

  /** Persiste PNG criptografado e expõe somente a rota governada vinculada à tarefa. */
  @Test
  void storesDerivedCreativeWithoutLabelingItAsProductScreenshot() throws Exception {
    var process = new com.marketinghub.businessprocess.BusinessProcessDefinition();
    process.setProcessCode("creative-production-approval");
    task.setProcessDefinition(process);
    task.setProcessActivityId("nonAudiovisual");
    task.getAssignedAgent().setAgentKey("communication-director");
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));
    when(targetContextProvider.resolve(task.getSourceReference(), "creative-production-approval"))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    task.getSourceReference(),
                    89L,
                    9L,
                    "rigel",
                    "Agenda Cheia",
                    "Rigel",
                    "rigel-v2",
                    "https://rigel.example/jornada",
                    null,
                    null,
                    null,
                    null)));
    when(evidenceRepository.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              AgentTaskVisualEvidence image = invocation.getArgument(0);
              image.setId(910130L);
              return image;
            });
    var pixels =
        new java.awt.image.BufferedImage(1080, 1350, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var bytes = new java.io.ByteArrayOutputStream();
    ImageIO.write(pixels, "png", bytes);
    var request =
        new AgentTaskVisualEvidenceRequest(
            "creative-test",
            "creative-1",
            "CREATIVE_RENDER",
            "CREATIVE_1080X1350",
            1,
            null,
            1080,
            1350,
            1350,
            0,
            "https://rigel.example/jornada",
            "https://rigel.example/jornada",
            Instant.parse("2026-09-12T09:00:00Z"));
    var response =
        service.store(
            "communication-director",
            258L,
            request,
            new MockMultipartFile("file", "creative.png", "image/png", bytes.toByteArray()));
    assertThat(response.evidenceType()).isEqualTo("CREATIVE_RENDER");
    assertThat(response.label()).contains("Criativo estático").doesNotContain("dobra");
    assertThatThrownBy(
            () -> service.store("communication-director", 258L, request, png("wrong-size")))
        .hasMessageContaining("PNG inválido");
  }

  /**
   * Usa a autorização congelada na tarefa quando o cadastro comercial ainda não possui URL pública.
   */
  @Test
  void storesDerivedCreativeFromFrozenVisualAuthorization() throws Exception {
    var process = new com.marketinghub.businessprocess.BusinessProcessDefinition();
    process.setProcessCode("creative-production-approval");
    task.setProcessDefinition(process);
    task.setProcessActivityId("nonAudiovisual");
    task.getAssignedAgent().setAgentKey("communication-director");
    task.setSourceReference("experiment:9301");
    task.setEvidenceJson(
        """
        {
          "communicationInputReference": {
            "visualProofAuthorization": {
              "contractVersion": "COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1",
              "proofSourceReference": "product:1901@agent-validation-v1",
              "targetSourceReference": "experiment:9301",
              "prototypeVersion": "private-v3",
              "productId": 1901,
              "publicUrl": "https://private.example/experience",
              "gateInstanceId": 242
            },
            "approvedDestination": {
              "contractVersion": "PRIVATE_PDE_DESTINATION_V1",
              "prototypeVersion": "private-v3",
              "url": "https://private.example/experience"
            },
            "validationGate": {
              "productId": 1901,
              "prototypeVersion": "private-v3",
              "publicUrl": "https://private.example/experience",
              "paymentEnabled": false,
              "publicationAuthorized": false,
              "campaignAuthorized": false
            },
            "approvedVisualArtifacts": [{
              "result": {
                "contractVersion": "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
                "decision": "APPROVED",
                "sourceReference": "product:1901@agent-validation-v1",
                "productId": 1901,
                "prototypeVersion": "private-v3",
                "publicUrl": "https://private.example/experience",
                "artifacts": [{
                  "artifactId": 951,
                  "sourceUrl": "https://private.example/experience",
                  "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                }]
              }
            }]
          }
        }
        """);
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));
    when(evidenceRepository.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              AgentTaskVisualEvidence image = invocation.getArgument(0);
              image.setId(910131L);
              return image;
            });
    var pixels =
        new java.awt.image.BufferedImage(1080, 1350, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var bytes = new java.io.ByteArrayOutputStream();
    ImageIO.write(pixels, "png", bytes);
    var request =
        new AgentTaskVisualEvidenceRequest(
            "creative-frozen",
            "creative-1",
            "CREATIVE_RENDER",
            "CREATIVE_1080X1350",
            1,
            null,
            1080,
            1350,
            1350,
            0,
            "https://private.example/experience",
            "https://private.example/experience",
            Instant.parse("2026-09-25T16:00:00Z"));

    var response =
        service.store(
            "communication-director",
            258L,
            request,
            new MockMultipartFile("file", "creative.png", "image/png", bytes.toByteArray()));

    assertThat(response.id()).isEqualTo(910131L);
    verify(targetContextProvider, never())
        .resolve("experiment:9301", "creative-production-approval");
  }

  /** Rejeita uma URL que não corresponda à autorização congelada da tarefa criativa. */
  @Test
  void rejectsCreativeOutsideFrozenVisualAuthorization() throws Exception {
    var process = new com.marketinghub.businessprocess.BusinessProcessDefinition();
    process.setProcessCode("creative-production-approval");
    task.setProcessDefinition(process);
    task.setProcessActivityId("nonAudiovisual");
    task.getAssignedAgent().setAgentKey("communication-director");
    task.setSourceReference("experiment:9301");
    task.setEvidenceJson(
        """
        {"communicationInputReference":{
          "visualProofAuthorization":{"contractVersion":"COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1","proofSourceReference":"product:1901@agent-validation-v1","targetSourceReference":"experiment:9301","prototypeVersion":"private-v3","productId":1901,"publicUrl":"https://private.example/experience","gateInstanceId":242},
          "approvedDestination":{"contractVersion":"PRIVATE_PDE_DESTINATION_V1","prototypeVersion":"private-v3","url":"https://private.example/experience"},
          "validationGate":{"productId":1901,"prototypeVersion":"private-v3","publicUrl":"https://private.example/experience","paymentEnabled":false,"publicationAuthorized":false,"campaignAuthorized":false},
          "approvedVisualArtifacts":[{"result":{"contractVersion":"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1","decision":"APPROVED","sourceReference":"product:1901@agent-validation-v1","productId":1901,"prototypeVersion":"private-v3","publicUrl":"https://private.example/experience","artifacts":[{"artifactId":951,"sourceUrl":"https://private.example/experience","sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]}}]
        }}
        """);
    when(taskRepository.findById(258L)).thenReturn(Optional.of(task));
    var pixels =
        new java.awt.image.BufferedImage(1080, 1350, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var bytes = new java.io.ByteArrayOutputStream();
    ImageIO.write(pixels, "png", bytes);
    var request =
        new AgentTaskVisualEvidenceRequest(
            "creative-frozen",
            "creative-1",
            "CREATIVE_RENDER",
            "CREATIVE_1080X1350",
            1,
            null,
            1080,
            1350,
            1350,
            0,
            "https://other.example/experience",
            "https://other.example/experience",
            Instant.parse("2026-09-25T16:00:00Z"));

    assertThatThrownBy(
            () ->
                service.store(
                    "communication-director",
                    258L,
                    request,
                    new MockMultipartFile(
                        "file", "creative.png", "image/png", bytes.toByteArray())))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não pertence ao produto e à versão");
    verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
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
