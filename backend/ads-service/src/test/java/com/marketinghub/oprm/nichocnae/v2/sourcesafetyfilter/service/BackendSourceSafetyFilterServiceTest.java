package com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.completeStageExecution.SourceSafetyFilterCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.createStageExecution.SourceSafetyFilterCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.failStageExecution.SourceSafetyFilterFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.pending.SourceSafetyFilterPendingResponse;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** Valida os contratos backend da etapa source-safety-filter do pipeline OPRM NichoCNAE v2. */
@ExtendWith(MockitoExtension.class)
class BackendSourceSafetyFilterServiceTest {
    @Mock private OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;

    /** Deve manter a etapa 2 sem pendências quando a feature flag da v2 estiver desligada. */
    @Test
    void pendingReturnsEmptyWhenFeatureFlagIsDisabled() {
        BackendSourceSafetyFilterService service = service(false);

        List<SourceSafetyFilterPendingResponse> result = service.pending();

        assertThat(result).isEmpty();
        verify(stageExecutionRepository, never()).findByStageCodeAndStatusOrderByCreatedAtAsc(any(), any(), any());
    }

    /** Deve expor pendência da etapa 2 com payload recebido da etapa anterior para filtragem segura. */
    @Test
    void pendingReturnsSourceSafetyExecutions() {
        BackendSourceSafetyFilterService service = service(true);
        when(stageExecutionRepository.findByStageCodeAndStatusOrderByCreatedAtAsc(
                        eq("source-safety-filter"), eq(OprmNichoCnaeV2StageExecutionStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(execution(200L)));

        List<SourceSafetyFilterPendingResponse> result = service.pending();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().stageExecutionId()).isEqualTo("200");
        assertThat(result.getFirst().inputPayload()).contains("candidateUrls");
    }

    /** Deve persistir a próxima etapa recebida do executor sem calcular decisão de negócio no backend. */
    @Test
    void completeRoutesToAdaptiveQueryPlannerWhenSafetyAllows() {
        BackendSourceSafetyFilterService service = service(true);
        OprmNichoCnaeV2StageExecution execution = execution(201L);
        when(stageExecutionRepository.findByIdAndStageCode(201L, "source-safety-filter"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.complete(
                201L,
                new SourceSafetyFilterCompletionRequest(
                        "ALLOW", 2, 1, "{\"allowedUrls\":[\"https://gov.br/exemplo\"]}", "adaptive-query-planner"));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.nextStageCode()).isEqualTo("adaptive-query-planner");
        assertThat(response.allowedUrlCount()).isEqualTo(2);
        assertThat(response.rejectedUrlCount()).isEqualTo(1);
    }

    /** Deve gravar pendência da etapa 2 a partir de um comando explícito do executor externo. */
    @Test
    void createPersistsPendingExecutionRequestedByExecutor() {
        BackendSourceSafetyFilterService service = service(true);
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution saved = invocation.getArgument(0);
            saved.setId(300L);
            return saved;
        });

        var response = service.create(new SourceSafetyFilterCreateRequest(
                "job-300", 70L, 69L, "9602501", 2, 4, false, "{\"candidateUrls\":[]}"));

        assertThat(response.stageExecutionId()).isEqualTo("300");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.stageCode()).isEqualTo("source-safety-filter");
    }

    /** Deve preservar retry técnico sem mudar tentativa cognitiva quando a falha da etapa 2 for infraestrutura. */
    @Test
    void failCreatesTechnicalRetryForInfrastructureFailure() {
        BackendSourceSafetyFilterService service = service(true);
        OprmNichoCnaeV2StageExecution execution = execution(202L);
        when(stageExecutionRepository.findByIdAndStageCode(202L, "source-safety-filter"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(203L);
            }
            return saved;
        });

        var response = service.fail(
                202L,
                new SourceSafetyFilterFailureRequest(
                        OprmNichoCnaeV2FailureType.INFRASTRUCTURE,
                        "TIMEOUT",
                        "timeout lendo fila",
                        "{}"));

        assertThat(response.status()).isEqualTo("TECHNICAL_RETRY_SCHEDULED");
        assertThat(response.retryStageExecutionId()).isEqualTo("203");
        assertThat(response.attemptNumber()).isEqualTo(1);
        assertThat(response.technicalRetryNumber()).isEqualTo(1);
    }

    /** Cria o service testável com flag explícita para a v2. */
    private BackendSourceSafetyFilterService service(boolean v2Enabled) {
        return new BackendSourceSafetyFilterService(stageExecutionRepository, v2Enabled);
    }

    /** Monta execução persistida mínima da etapa source-safety-filter. */
    private OprmNichoCnaeV2StageExecution execution(Long id) {
        OprmNichoCnaeV2StageExecution execution = new OprmNichoCnaeV2StageExecution();
        execution.setId(id);
        execution.setJobId("nichocnae-v2-candidate-69");
        execution.setSourceNicheId(69L);
        execution.setCnaeCode("9602501");
        execution.setStageCode("source-safety-filter");
        execution.setAttemptNumber(1);
        execution.setTechnicalRetryNumber(0);
        execution.setKnowledgeVersion(1);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        execution.setInputPayload("{\"candidateUrls\":[\"https://gov.br/exemplo\",\"https://adult.example/x\"]}");
        execution.setMaterializationEnabled(false);
        execution.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return execution;
    }
}
