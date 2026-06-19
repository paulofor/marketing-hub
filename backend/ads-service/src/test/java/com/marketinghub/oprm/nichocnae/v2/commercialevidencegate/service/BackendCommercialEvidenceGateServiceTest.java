package com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.completeStageExecution.CommercialEvidenceGateCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.createStageExecution.CommercialEvidenceGateCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.failStageExecution.CommercialEvidenceGateFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.pending.CommercialEvidenceGatePendingResponse;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** Valida que o backend da etapa commercial-evidence-gate atua somente como leitura e escrita para o executor. */
@ExtendWith(MockitoExtension.class)
class BackendCommercialEvidenceGateServiceTest {
    @Mock private OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;

    /** Deve manter a etapa 7 sem pendências quando a feature flag da v2 estiver desligada. */
    @Test
    void pendingReturnsEmptyWhenFeatureFlagIsDisabled() {
        BackendCommercialEvidenceGateService service = service(false);
        List<CommercialEvidenceGatePendingResponse> result = service.pending();
        assertThat(result).isEmpty();
        verify(stageExecutionRepository, never()).findByStageCodeAndStatusOrderByCreatedAtAsc(any(), any(), any());
    }

    /** Deve entregar ao executor o payload persistido sem recalcular regra comercial no backend. */
    @Test
    void pendingReturnsCommercialEvidenceGateExecutions() {
        BackendCommercialEvidenceGateService service = service(true);
        when(stageExecutionRepository.findByStageCodeAndStatusOrderByCreatedAtAsc(
                        eq("commercial-evidence-gate"),
                        eq(OprmNichoCnaeV2StageExecutionStatus.PENDING),
                        any(Pageable.class)))
                .thenReturn(List.of(execution(700L)));
        List<CommercialEvidenceGatePendingResponse> result = service.pending();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().stageExecutionId()).isEqualTo("700");
        assertThat(result.getFirst().inputPayload()).contains("validatedClaims");
    }

    /** Deve persistir exatamente a decisão enviada pelo executor, sem aplicar regra E0-E5 no backend. */
    @Test
    void completePersistsExecutorDecisionOnly() {
        BackendCommercialEvidenceGateService service = service(true);
        OprmNichoCnaeV2StageExecution execution = execution(701L);
        when(stageExecutionRepository.findByIdAndStageCode(701L, "commercial-evidence-gate"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.complete(
                701L,
                new CommercialEvidenceGateCompletionRequest(
                        "E0_MODEL_HYPOTHESIS",
                        0.05,
                        true,
                        false,
                        0.0,
                        "MATERIALIZE",
                        "enriched-niche-materializer",
                        "{\"gateDecision\":\"MATERIALIZE\"}"));
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.evidenceLevel()).isEqualTo("E0_MODEL_HYPOTHESIS");
        assertThat(response.automaticMaterializationAllowed()).isTrue();
        assertThat(response.nextStageCode()).isEqualTo("enriched-niche-materializer");
    }

    /** Deve gravar pendência da etapa 7 quando o executor solicitar reprocessamento cognitivo. */
    @Test
    void createPersistsPendingExecutionRequestedByExecutor() {
        BackendCommercialEvidenceGateService service = service(true);
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution saved = invocation.getArgument(0);
            saved.setId(702L);
            return saved;
        });
        var response = service.create(new CommercialEvidenceGateCreateRequest(
                "job-72", 72L, 69L, "4781400", 3, 5, false, "{\"validatedClaims\":[]}"));
        assertThat(response.stageExecutionId()).isEqualTo("702");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.stageCode()).isEqualTo("commercial-evidence-gate");
    }

    /** Deve preservar retry técnico sem mudar tentativa cognitiva quando a falha da etapa 7 for infraestrutura. */
    @Test
    void failCreatesTechnicalRetryForInfrastructureFailure() {
        BackendCommercialEvidenceGateService service = service(true);
        OprmNichoCnaeV2StageExecution execution = execution(703L);
        when(stageExecutionRepository.findByIdAndStageCode(703L, "commercial-evidence-gate"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(704L);
            }
            return saved;
        });
        var response = service.fail(
                703L,
                new CommercialEvidenceGateFailureRequest(
                        OprmNichoCnaeV2FailureType.INFRASTRUCTURE, "TIMEOUT no callback do executor", "{}"));
        assertThat(response.status()).isEqualTo("TECHNICAL_RETRY_SCHEDULED");
        assertThat(response.retryStageExecutionId()).isEqualTo("704");
        assertThat(response.attemptNumber()).isEqualTo(3);
        assertThat(response.technicalRetryNumber()).isEqualTo(1);
    }

    /** Cria o service testável com flag explícita para a v2. */
    private BackendCommercialEvidenceGateService service(boolean v2Enabled) {
        return new BackendCommercialEvidenceGateService(stageExecutionRepository, v2Enabled);
    }

    /** Monta execução persistida mínima da etapa commercial-evidence-gate. */
    private OprmNichoCnaeV2StageExecution execution(Long id) {
        OprmNichoCnaeV2StageExecution execution = new OprmNichoCnaeV2StageExecution();
        execution.setId(id);
        execution.setJobId("job-72");
        execution.setResearchCycleId(72L);
        execution.setSourceNicheId(69L);
        execution.setCnaeCode("4781400");
        execution.setStageCode("commercial-evidence-gate");
        execution.setAttemptNumber(3);
        execution.setTechnicalRetryNumber(0);
        execution.setKnowledgeVersion(5);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        execution.setInputPayload("{\"validatedClaims\":[{\"claimType\":\"TASK\"}]}");
        execution.setMaterializationEnabled(false);
        execution.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return execution;
    }
}
