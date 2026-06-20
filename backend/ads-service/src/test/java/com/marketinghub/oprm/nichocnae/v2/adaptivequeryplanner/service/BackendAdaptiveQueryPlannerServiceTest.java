package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.completeStageExecution.AdaptiveQueryPlannerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.createStageExecution.AdaptiveQueryPlannerCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.failStageExecution.AdaptiveQueryPlannerFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.pending.AdaptiveQueryPlannerPendingResponse;
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

/** Valida os contratos backend da etapa adaptive-query-planner do pipeline OPRM NichoCNAE v2. */
@ExtendWith(MockitoExtension.class)
class BackendAdaptiveQueryPlannerServiceTest {
    @Mock private OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;
    @Mock private OprmNicheCandidateRepository nicheCandidateRepository;

    /** Deve manter a etapa 3 sem pendências quando a feature flag da v2 estiver desligada. */
    @Test
    void pendingReturnsEmptyWhenFeatureFlagIsDisabled() {
        BackendAdaptiveQueryPlannerService service = service(false);

        List<AdaptiveQueryPlannerPendingResponse> result = service.pending();

        assertThat(result).isEmpty();
        verify(stageExecutionRepository, never()).findByStageCodeAndStatusOrderByCreatedAtAsc(any(), any(), any());
    }

    /** Deve expor pendência da etapa 3 com o snapshot de conhecimento recebido do executor. */
    @Test
    void pendingReturnsAdaptiveQueryPlannerExecutions() {
        BackendAdaptiveQueryPlannerService service = service(true);
        when(stageExecutionRepository.findByStageCodeAndStatusOrderByCreatedAtAsc(
                        eq("adaptive-query-planner"),
                        eq(OprmNichoCnaeV2StageExecutionStatus.PENDING),
                        any(Pageable.class)))
                .thenReturn(List.of(execution(400L)));

        List<AdaptiveQueryPlannerPendingResponse> result = service.pending();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().stageExecutionId()).isEqualTo("400");
        assertThat(result.getFirst().inputPayload()).contains("evidenceGaps");
    }

    /** Deve persistir a próxima etapa recebida do executor sem calcular plano ou regra de negócio no backend. */
    @Test
    void completePersistsExecutorDecisionOnly() {
        BackendAdaptiveQueryPlannerService service = service(true);
        OprmNichoCnaeV2StageExecution execution = execution(401L);
        when(stageExecutionRepository.findByIdAndStageCode(401L, "adaptive-query-planner"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.complete(
                401L,
                new AdaptiveQueryPlannerCompletionRequest(
                        "PLAN_READY", 4, 3, 2, "{\"plannedQueries\":[]}", "source-searcher"));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.nextStageCode()).isEqualTo("source-searcher");
        assertThat(response.plannedQueryCount()).isEqualTo(4);
        assertThat(response.reusedQueryCount()).isEqualTo(3);
        assertThat(response.skippedQueryCount()).isEqualTo(2);
    }

    /** Deve gravar pendência da etapa 3 quando o executor solicitar reprocessamento cognitivo para query planner. */
    @Test
    void createPersistsPendingExecutionRequestedByExecutor() {
        BackendAdaptiveQueryPlannerService service = service(true);
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution saved = invocation.getArgument(0);
            saved.setId(402L);
            return saved;
        });

        var response = service.create(new AdaptiveQueryPlannerCreateRequest(
                "job-72", 72L, 69L, "4781400", 2, 4, false, "{\"evidenceGaps\":[]}"));

        assertThat(response.stageExecutionId()).isEqualTo("402");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.stageCode()).isEqualTo("adaptive-query-planner");
    }

    /** Deve preservar retry técnico sem mudar tentativa cognitiva quando a falha da etapa 3 for infraestrutura. */
    @Test
    void failCreatesTechnicalRetryForInfrastructureFailure() {
        BackendAdaptiveQueryPlannerService service = service(true);
        OprmNichoCnaeV2StageExecution execution = execution(403L);
        when(stageExecutionRepository.findByIdAndStageCode(403L, "adaptive-query-planner"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(404L);
            }
            return saved;
        });

        var response = service.fail(
                403L,
                new AdaptiveQueryPlannerFailureRequest(
                        OprmNichoCnaeV2FailureType.INFRASTRUCTURE, "TIMEOUT gerando plano", "{}"));

        assertThat(response.status()).isEqualTo("TECHNICAL_RETRY_SCHEDULED");
        assertThat(response.retryStageExecutionId()).isEqualTo("404");
        assertThat(response.attemptNumber()).isEqualTo(2);
        assertThat(response.technicalRetryNumber()).isEqualTo(1);
    }

    /** Cria o service testável com flag explícita para a v2. */
    private BackendAdaptiveQueryPlannerService service(boolean v2Enabled) {
        return new BackendAdaptiveQueryPlannerService(stageExecutionRepository, nicheCandidateRepository, v2Enabled);
    }

    /** Monta execução persistida mínima da etapa adaptive-query-planner. */
    private OprmNichoCnaeV2StageExecution execution(Long id) {
        OprmNichoCnaeV2StageExecution execution = new OprmNichoCnaeV2StageExecution();
        execution.setId(id);
        execution.setJobId("job-72");
        execution.setResearchCycleId(72L);
        execution.setSourceNicheId(69L);
        execution.setCnaeCode("4781400");
        execution.setStageCode("adaptive-query-planner");
        execution.setAttemptNumber(2);
        execution.setTechnicalRetryNumber(0);
        execution.setKnowledgeVersion(4);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        execution.setInputPayload("{\"evidenceGaps\":[\"MISSING_EXECUTOR_ROUTINE_EVIDENCE\"]}");
        execution.setMaterializationEnabled(false);
        execution.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return execution;
    }
}
