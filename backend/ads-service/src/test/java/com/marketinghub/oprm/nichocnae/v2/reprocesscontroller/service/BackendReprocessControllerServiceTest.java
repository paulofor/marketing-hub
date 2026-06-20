package com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.completeStageExecution.ReprocessControllerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.createStageExecution.ReprocessControllerCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.failStageExecution.ReprocessControllerFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.pending.ReprocessControllerPendingResponse;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** Valida que o backend da etapa reprocess-controller atua somente como leitura e escrita para o executor. */
@ExtendWith(MockitoExtension.class)
class BackendReprocessControllerServiceTest {
    @Mock private OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;
    @Mock private OprmNicheCandidateRepository nicheCandidateRepository;

    /** Deve manter a etapa 9 sem pendências quando a feature flag da v2 estiver desligada. */
    @Test
    void pendingReturnsEmptyWhenFeatureFlagIsDisabled() {
        BackendReprocessControllerService service = service(false);
        List<ReprocessControllerPendingResponse> result = service.pending();
        assertThat(result).isEmpty();
        verify(stageExecutionRepository, never()).findByStageCodeAndStatusOrderByCreatedAtAsc(any(), any(), any());
    }

    /** Deve entregar ao executor o payload persistido sem recalcular regra comercial no backend. */
    @Test
    void pendingReturnsReprocessControllerExecutions() {
        BackendReprocessControllerService service = service(true);
        when(stageExecutionRepository.findByStageCodeAndStatusOrderByCreatedAtAsc(
                        eq("reprocess-controller"),
                        eq(OprmNichoCnaeV2StageExecutionStatus.PENDING),
                        any(Pageable.class)))
                .thenReturn(List.of(execution(700L)));
        List<ReprocessControllerPendingResponse> result = service.pending();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().stageExecutionId()).isEqualTo("700");
        assertThat(result.getFirst().inputPayload()).contains("gateDecision");
    }

    /** Deve persistir exatamente a decisão enviada pelo executor, sem aplicar regra de reprocessamento no backend. */
    @Test
    void completePersistsExecutorDecisionOnly() {
        BackendReprocessControllerService service = service(true);
        OprmNichoCnaeV2StageExecution execution = execution(701L);
        when(stageExecutionRepository.findByIdAndStageCode(701L, "reprocess-controller"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.complete(
                701L,
                new ReprocessControllerCompletionRequest(
                        "COGNITIVE_REPROCESS",
                        "adaptive-query-planner",
                        6,
                        "adaptive-query-planner",
                        "{\"executionMode\":\"COGNITIVE_REPROCESS\"}"));
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.executionMode()).isEqualTo("COGNITIVE_REPROCESS");
        assertThat(response.rewindToStage()).isEqualTo("adaptive-query-planner");
        assertThat(response.nextStageCode()).isEqualTo("adaptive-query-planner");
    }

    /** Deve gravar pendência da etapa 9 quando o executor solicitar reprocessamento cognitivo. */
    @Test
    void createPersistsPendingExecutionRequestedByExecutor() {
        BackendReprocessControllerService service = service(true);
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution saved = invocation.getArgument(0);
            saved.setId(702L);
            return saved;
        });
        var response = service.create(new ReprocessControllerCreateRequest(
                "job-72", 72L, 69L, "4781400", 3, 5, false, "{\"gateDecision\":\"NEEDS_MORE_RESEARCH\"}"));
        assertThat(response.stageExecutionId()).isEqualTo("702");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.stageCode()).isEqualTo("reprocess-controller");
    }

    /** Deve preservar retry técnico sem mudar tentativa cognitiva quando a falha da etapa 9 for infraestrutura. */
    @Test
    void failCreatesTechnicalRetryForInfrastructureFailure() {
        BackendReprocessControllerService service = service(true);
        OprmNichoCnaeV2StageExecution execution = execution(703L);
        when(stageExecutionRepository.findByIdAndStageCode(703L, "reprocess-controller"))
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
                new ReprocessControllerFailureRequest(
                        OprmNichoCnaeV2FailureType.INFRASTRUCTURE, "TIMEOUT no callback do executor", "{}"));
        assertThat(response.status()).isEqualTo("TECHNICAL_RETRY_SCHEDULED");
        assertThat(response.retryStageExecutionId()).isEqualTo("704");
        assertThat(response.attemptNumber()).isEqualTo(3);
        assertThat(response.technicalRetryNumber()).isEqualTo(1);
    }

    /** Cria o service testável com flag explícita para a v2. */
    private BackendReprocessControllerService service(boolean v2Enabled) {
        return new BackendReprocessControllerService(stageExecutionRepository, nicheCandidateRepository, v2Enabled);
    }

    /** Monta execução persistida mínima da etapa reprocess-controller. */
    private OprmNichoCnaeV2StageExecution execution(Long id) {
        OprmNichoCnaeV2StageExecution execution = new OprmNichoCnaeV2StageExecution();
        execution.setId(id);
        execution.setJobId("job-72");
        execution.setResearchCycleId(72L);
        execution.setSourceNicheId(69L);
        execution.setCnaeCode("4781400");
        execution.setStageCode("reprocess-controller");
        execution.setAttemptNumber(3);
        execution.setTechnicalRetryNumber(0);
        execution.setKnowledgeVersion(5);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        execution.setInputPayload("{\"gateDecision\":\"NEEDS_MORE_RESEARCH\"}");
        execution.setMaterializationEnabled(false);
        execution.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return execution;
    }
}
