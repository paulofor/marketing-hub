package com.marketinghub.oprm.nichocnae.v2.candidatetournament.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.completeStageExecution.CandidateTournamentCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.createStageExecution.CandidateTournamentCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.failStageExecution.CandidateTournamentFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.pending.CandidateTournamentPendingResponse;
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

/** Valida os contratos backend da etapa candidate-tournament do pipeline OPRM NichoCNAE v2. */
@ExtendWith(MockitoExtension.class)
class BackendCandidateTournamentServiceTest {
    @Mock private OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;
    @Mock private OprmNicheCandidateRepository nicheCandidateRepository;

    /** Deve manter a etapa 4 sem pendências quando a feature flag da v2 estiver desligada. */
    @Test
    void pendingReturnsEmptyWhenFeatureFlagIsDisabled() {
        BackendCandidateTournamentService service = service(false);

        List<CandidateTournamentPendingResponse> result = service.pending();

        assertThat(result).isEmpty();
        verify(stageExecutionRepository, never()).findByStageCodeAndStatusOrderByCreatedAtAsc(any(), any(), any());
    }

    /** Deve expor pendência da etapa 4 com o snapshot de conhecimento recebido do executor. */
    @Test
    void pendingReturnsCandidateTournamentExecutions() {
        BackendCandidateTournamentService service = service(true);
        when(stageExecutionRepository.findByStageCodeAndStatusOrderByCreatedAtAsc(
                        eq("candidate-tournament"),
                        eq(OprmNichoCnaeV2StageExecutionStatus.PENDING),
                        any(Pageable.class)))
                .thenReturn(List.of(execution(400L)));

        List<CandidateTournamentPendingResponse> result = service.pending();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().stageExecutionId()).isEqualTo("400");
        assertThat(result.getFirst().inputPayload()).contains("candidates");
    }

    /** Deve persistir a próxima etapa recebida do executor sem calcular torneio ou regra de negócio no backend. */
    @Test
    void completePersistsExecutorDecisionOnly() {
        BackendCandidateTournamentService service = service(true);
        OprmNichoCnaeV2StageExecution execution = execution(401L);
        when(stageExecutionRepository.findByIdAndStageCode(401L, "candidate-tournament"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.complete(
                401L,
                new CandidateTournamentCompletionRequest(
                        "FINALISTS_SELECTED", 4, 2, "{\"finalists\":[]}", "source-fetcher"));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.nextStageCode()).isEqualTo("source-fetcher");
        assertThat(response.tournamentDecision()).isEqualTo("FINALISTS_SELECTED");
        assertThat(response.candidateCount()).isEqualTo(4);
        assertThat(response.finalistCount()).isEqualTo(2);
    }

    /** Deve gravar pendência da etapa 4 quando o executor solicitar reprocessamento cognitivo para query planner. */
    @Test
    void createPersistsPendingExecutionRequestedByExecutor() {
        BackendCandidateTournamentService service = service(true);
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution saved = invocation.getArgument(0);
            saved.setId(402L);
            return saved;
        });

        var response = service.create(new CandidateTournamentCreateRequest(
                "job-72", 72L, 69L, "4781400", 2, 4, false, "{\"candidates\":[]}"));

        assertThat(response.stageExecutionId()).isEqualTo("402");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.stageCode()).isEqualTo("candidate-tournament");
    }

    /** Deve preservar retry técnico sem mudar tentativa cognitiva quando a falha da etapa 4 for infraestrutura. */
    @Test
    void failCreatesTechnicalRetryForInfrastructureFailure() {
        BackendCandidateTournamentService service = service(true);
        OprmNichoCnaeV2StageExecution execution = execution(403L);
        when(stageExecutionRepository.findByIdAndStageCode(403L, "candidate-tournament"))
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
                new CandidateTournamentFailureRequest(
                        OprmNichoCnaeV2FailureType.INFRASTRUCTURE, "TIMEOUT comparando candidatos", "{}"));

        assertThat(response.status()).isEqualTo("TECHNICAL_RETRY_SCHEDULED");
        assertThat(response.retryStageExecutionId()).isEqualTo("404");
        assertThat(response.attemptNumber()).isEqualTo(2);
        assertThat(response.technicalRetryNumber()).isEqualTo(1);
    }

    /** Cria o service testável com flag explícita para a v2. */
    private BackendCandidateTournamentService service(boolean v2Enabled) {
        return new BackendCandidateTournamentService(stageExecutionRepository, nicheCandidateRepository, v2Enabled);
    }

    /** Monta execução persistida mínima da etapa candidate-tournament. */
    private OprmNichoCnaeV2StageExecution execution(Long id) {
        OprmNichoCnaeV2StageExecution execution = new OprmNichoCnaeV2StageExecution();
        execution.setId(id);
        execution.setJobId("job-72");
        execution.setResearchCycleId(72L);
        execution.setSourceNicheId(69L);
        execution.setCnaeCode("4781400");
        execution.setStageCode("candidate-tournament");
        execution.setAttemptNumber(2);
        execution.setTechnicalRetryNumber(0);
        execution.setKnowledgeVersion(4);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        execution.setInputPayload("{\"candidates\":[{\"candidateId\":\"a\"}]}");
        execution.setMaterializationEnabled(false);
        execution.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return execution;
    }
}
