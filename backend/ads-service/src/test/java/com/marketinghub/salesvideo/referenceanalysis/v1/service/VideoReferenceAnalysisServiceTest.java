package com.marketinghub.salesvideo.referenceanalysis.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.repository.jpa.salesvideo.VideoReferenceAnalysisExecutionRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoReferenceRepository;
import com.marketinghub.salesvideo.VideoReference;
import com.marketinghub.salesvideo.VideoReferenceAnalysisExecution;
import com.marketinghub.salesvideo.VideoReferenceAnalysisStatus;
import com.marketinghub.salesvideo.VideoReferenceStatus;
import com.marketinghub.salesvideo.referenceanalysis.v1.service.complete.CompleteRequest;
import com.marketinghub.salesvideo.tenant.TenantContext;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida fila, lease, callbacks e isolamento da análise automática de referências. */
@ExtendWith(MockitoExtension.class)
class VideoReferenceAnalysisServiceTest {
  @Mock private VideoReferenceAnalysisExecutionRepository executionRepository;
  @Mock private VideoReferenceRepository referenceRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private VideoReferenceAnalysisService service;

  /** Configura tenant e persistência simulada antes de cada cenário. */
  @BeforeEach
  void setUp() {
    TenantContextHolder.set(new TenantContext("tenant-video", "editor@marketinghub.io", false));
    service =
        new VideoReferenceAnalysisService(executionRepository, referenceRepository, objectMapper);
  }

  /** Limpa o tenant após cada cenário para impedir contaminação entre testes. */
  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  /** Cria execução com snapshot completo e tentativa monotônica. */
  @Test
  void shouldEnqueueReferenceWithImmutableInput() {
    saveReturnsArgument();
    VideoReference reference = reference();
    given(executionRepository.findFirstByReferenceIdOrderByAttemptNumberDesc(31L))
        .willReturn(Optional.empty());

    VideoReferenceAnalysisExecution result = service.enqueue(reference);

    assertThat(result.getAttemptNumber()).isEqualTo(1);
    assertThat(result.getStatus()).isEqualTo(VideoReferenceAnalysisStatus.QUEUED);
    assertThat(result.getInputJson())
        .contains("Rio Antigo", "https://cdn.example/rio-antigo.mp4", "ritmo e continuidade");
  }

  /** Reserva uma única execução e marca a referência como em análise. */
  @Test
  void shouldClaimOnePendingExecutionWithCorrelation() {
    saveReturnsArgument();
    VideoReference reference = reference();
    VideoReferenceAnalysisExecution execution = execution(VideoReferenceAnalysisStatus.QUEUED);
    given(executionRepository.findClaimable(any(), any(), any(), any()))
        .willReturn(List.of(execution));
    given(executionRepository.sumKnownCostUsd()).willReturn(new BigDecimal("0.10"));
    given(executionRepository.countByStatus(VideoReferenceAnalysisStatus.RUNNING)).willReturn(0L);
    given(referenceRepository.findById(31L)).willReturn(Optional.of(reference));

    var pending =
        service.claimPending("worker-local", new BigDecimal("0.75"), new BigDecimal("0.25"));

    assertThat(pending).hasSize(1);
    assertThat(pending.getFirst().producerExecutionId()).isNotBlank();
    assertThat(execution.getStatus()).isEqualTo(VideoReferenceAnalysisStatus.RUNNING);
    assertThat(reference.getStatus()).isEqualTo(VideoReferenceStatus.ANALYZING);
  }

  /** Bloqueia a referência antes do consumo quando custo e reserva ultrapassam o teto. */
  @Test
  void shouldBlockBeforeExternalSpendWhenBudgetIsExhausted() {
    saveReturnsArgument();
    VideoReference reference = reference();
    VideoReferenceAnalysisExecution execution = execution(VideoReferenceAnalysisStatus.QUEUED);
    given(executionRepository.findClaimable(any(), any(), any(), any()))
        .willReturn(List.of(execution));
    given(executionRepository.sumKnownCostUsd()).willReturn(new BigDecimal("0.60"));
    given(executionRepository.countByStatus(VideoReferenceAnalysisStatus.RUNNING)).willReturn(0L);
    given(referenceRepository.findById(31L)).willReturn(Optional.of(reference));

    var pending =
        service.claimPending("worker-local", new BigDecimal("0.75"), new BigDecimal("0.25"));

    assertThat(pending).isEmpty();
    assertThat(execution.getStatus()).isEqualTo(VideoReferenceAnalysisStatus.BUDGET_BLOCKED);
    assertThat(execution.getError()).contains("limite US$ 0.75");
    assertThat(reference.getStatus()).isEqualTo(VideoReferenceStatus.REJECTED);
  }

  /** Persiste resultado e libera o aprendizado apenas com UUID da execução ativa. */
  @Test
  void shouldCompleteCorrelatedExecutionAndReference() {
    saveReturnsArgument();
    VideoReference reference = reference();
    VideoReferenceAnalysisExecution execution = execution(VideoReferenceAnalysisStatus.RUNNING);
    execution.setProducerExecutionId("producer-31");
    given(executionRepository.findById(81L)).willReturn(Optional.of(execution));
    given(referenceRepository.findById(31L)).willReturn(Optional.of(reference));
    ObjectNode output = objectMapper.createObjectNode().put("operationalDecision", "APOLLO_READY");
    ObjectNode artifacts = objectMapper.createObjectNode().put("sha256", "abc");

    var response =
        service.complete(
            81L,
            new CompleteRequest(
                "producer-31",
                "**Diagnóstico comercial**\nReceita aprovada",
                output,
                artifacts,
                objectMapper.createObjectNode().put("request", true),
                objectMapper.createObjectNode().put("response", true),
                "gpt-5.6",
                100L,
                20L,
                30L,
                new BigDecimal("0.010000"),
                "APOLLO_READY"));

    assertThat(response.status()).isEqualTo(VideoReferenceAnalysisStatus.COMPLETED);
    assertThat(response.costUsd()).isEqualByComparingTo("0.010000");
    assertThat(reference.getStatus()).isEqualTo(VideoReferenceStatus.ANALYZED);
    assertThat(reference.getAnalysisNotes()).contains("Receita aprovada");
  }

  /** Rejeita callback antigo para não sobrescrever a execução recuperada pelo lease. */
  @Test
  void shouldRejectStaleCallback() {
    VideoReferenceAnalysisExecution execution = execution(VideoReferenceAnalysisStatus.RUNNING);
    execution.setProducerExecutionId("producer-current");
    given(executionRepository.findById(81L)).willReturn(Optional.of(execution));

    assertThatThrownBy(
            () ->
                service.complete(
                    81L,
                    new CompleteRequest(
                        "producer-old",
                        "resumo",
                        objectMapper.createObjectNode(),
                        objectMapper.createObjectNode(),
                        objectMapper.createObjectNode(),
                        objectMapper.createObjectNode(),
                        "gpt-5.6",
                        null,
                        null,
                        null,
                        null,
                        "APOLLO_READY")))
        .hasMessageContaining("Callback antigo");
  }

  /** Impede que a contingência manual sobrescreva uma execução automática ativa. */
  @Test
  void shouldBlockManualContingencyWhileAutomaticAnalysisIsActive() {
    VideoReference reference = reference();
    VideoReferenceAnalysisExecution execution = execution(VideoReferenceAnalysisStatus.RUNNING);
    given(referenceRepository.findById(31L)).willReturn(Optional.of(reference));
    given(executionRepository.findFirstByTenantIdAndReferenceIdOrderByIdDesc("tenant-video", 31L))
        .willReturn(Optional.of(execution));

    assertThatThrownBy(() -> service.assertManualContingencyAllowed(31L))
        .hasMessageContaining("contingência manual");
  }

  /** Libera contingência manual somente após uma falha automática auditada. */
  @Test
  void shouldAllowManualContingencyAfterAutomaticFailure() {
    VideoReference reference = reference();
    VideoReferenceAnalysisExecution execution = execution(VideoReferenceAnalysisStatus.FAILED);
    given(referenceRepository.findById(31L)).willReturn(Optional.of(reference));
    given(executionRepository.findFirstByTenantIdAndReferenceIdOrderByIdDesc("tenant-video", 31L))
        .willReturn(Optional.of(execution));

    service.assertManualContingencyAllowed(31L);
  }

  /** Monta uma referência isolada usada nos cenários. */
  private VideoReference reference() {
    VideoReference reference =
        VideoReference.builder()
            .tenantId("tenant-video")
            .title("Rio Antigo")
            .sourceUrl("https://cdn.example/rio-antigo.mp4")
            .sourcePlatform("Upload")
            .primaryLearningGoal("Aprender ritmo e continuidade")
            .status(VideoReferenceStatus.QUEUED)
            .build();
    reference.setId(31L);
    return reference;
  }

  /** Monta uma execução persistida usada nos cenários. */
  private VideoReferenceAnalysisExecution execution(VideoReferenceAnalysisStatus status) {
    VideoReferenceAnalysisExecution execution = new VideoReferenceAnalysisExecution();
    execution.setId(81L);
    execution.setReferenceId(31L);
    execution.setTenantId("tenant-video");
    execution.setAttemptNumber(1);
    execution.setStatus(status);
    execution.setInputJson("{\"sourceUrl\":\"https://cdn.example/rio-antigo.mp4\"}");
    execution.setCreatedAt(Instant.now());
    execution.setUpdatedAt(Instant.now());
    return execution;
  }

  /** Faz o repositório simulado devolver a mesma entidade persistida nos cenários felizes. */
  private void saveReturnsArgument() {
    given(executionRepository.save(any(VideoReferenceAnalysisExecution.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }
}
