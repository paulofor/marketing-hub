package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution.CandidateGeneratorCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution.CandidateGeneratorCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution.CandidateGeneratorFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution.CandidateGeneratorFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.pending.CandidateGeneratorPendingResponse;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** Valida o primeiro incremento backend da etapa candidate-generator do pipeline OPRM NichoCNAE v2. */
@ExtendWith(MockitoExtension.class)
class BackendCandidateGeneratorServiceTest {
    @Mock private OprmNicheCandidateRepository nicheCandidateRepository;

    @Mock private OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;

    /** Deve manter a v2 sem pendências quando a feature flag estiver desligada durante calibração. */
    @Test
    void pendingReturnsEmptyWhenFeatureFlagIsDisabled() {
        BackendCandidateGeneratorService service = service(false, false);

        List<CandidateGeneratorPendingResponse> result = service.pending();

        assertThat(result).isEmpty();
        verify(nicheCandidateRepository, never()).findNextPendingRoutineResearchCandidatePreview(any(Pageable.class));
    }

    /** Ciclo 69: deve criar execução inicial sem liberar materialização automática na calibração. */
    @Test
    void pendingCreatesInitialImmutableExecutionForCycle69WithoutAutomaticMaterialization() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNicheCandidate candidate = candidate(69L, "9602501");
        List<OprmNichoCnaeV2StageExecution> stored = new ArrayList<>();
        when(stageExecutionRepository.findByStageCodeAndStatusOrderByCreatedAtAsc(
                        eq("candidate-generator"), eq(OprmNichoCnaeV2StageExecutionStatus.PENDING), any(Pageable.class)))
                .thenAnswer(invocation -> stored.stream()
                        .filter(execution -> execution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.PENDING)
                        .toList());
        when(nicheCandidateRepository.findNextPendingRoutineResearchCandidatePreview(any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(nicheCandidateRepository.findById(69L)).thenReturn(Optional.of(candidate));
        when(stageExecutionRepository.existsBySourceNicheIdAndStageCode(69L, "candidate-generator"))
                .thenReturn(false);
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution execution = invocation.getArgument(0);
            execution.setId(1001L);
            stored.add(execution);
            return execution;
        });

        List<CandidateGeneratorPendingResponse> result = service.pending();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().stageExecutionId()).isEqualTo("1001");
        assertThat(result.getFirst().jobId()).isEqualTo("nichocnae-v2-candidate-69-job-1");
        assertThat(result.getFirst().cnaeCode()).isEqualTo("9602501");
        assertThat(result.getFirst().cnaeDescription()).isEqualTo("Serviços de beleza");
        assertThat(result.getFirst().attemptNumber()).isEqualTo(1);
        assertThat(result.getFirst().technicalRetryNumber()).isZero();
        assertThat(result.getFirst().knowledgeVersion()).isEqualTo(1);
        assertThat(result.getFirst().materializationEnabled()).isFalse();
    }


    /** Deve gravar novo job manual para o CNAE selecionado sem executar o fluxo no backend. */
    @Test
    void createForCnaePersistsNewPendingJobForSelectedCnae() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNicheCandidate candidate = candidate(4781400L, "4781400");
        when(nicheCandidateRepository.findManualRoutineResearchCandidateByCnaeCode(eq("4781400"), any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(stageExecutionRepository.countBySourceNicheIdAndStageCode(4781400L, "candidate-generator"))
                .thenReturn(2L);
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution execution = invocation.getArgument(0);
            execution.setId(1200L);
            return execution;
        });

        var result = service.createForCnae("4781400");

        assertThat(result.stageExecutionId()).isEqualTo("1200");
        assertThat(result.jobId()).isEqualTo("nichocnae-v2-candidate-4781400-job-3");
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.cnaeCode()).isEqualTo("4781400");
    }


    /** Deve separar jobs do CNAE entre abertos com etapa atual e encerrados pelo histórico persistido. */
    @Test
    void listJobsForCnaeSeparatesOpenAndCompletedJobs() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNichoCnaeV2StageExecution open = execution(101L, 1, 0, 1);
        open.setJobId("job-aberto");
        open.setCnaeCode("4781400");
        open.setStageCode("source-safety-filter");
        open.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        open.setUpdatedAt(Instant.parse("2026-06-19T12:00:00Z"));
        OprmNichoCnaeV2StageExecution completed = execution(102L, 1, 0, 1);
        completed.setJobId("job-concluido");
        completed.setCnaeCode("4781400");
        completed.setStageCode("candidate-generator");
        completed.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        completed.setUpdatedAt(Instant.parse("2026-06-19T11:00:00Z"));
        when(stageExecutionRepository.findByCnaeCodeOrderByUpdatedAtDesc("4781400"))
                .thenReturn(List.of(open, completed));

        var result = service.listJobsForCnae("4781400");

        assertThat(result.openJobs()).hasSize(1);
        assertThat(result.openJobs().getFirst().jobId()).isEqualTo("job-aberto");
        assertThat(result.openJobs().getFirst().currentStageCode()).isEqualTo("source-safety-filter");
        assertThat(result.completedJobs()).hasSize(1);
        assertThat(result.completedJobs().getFirst().jobId()).isEqualTo("job-concluido");
        assertThat(result.completedJobs().getFirst().currentStageCode()).isNull();
    }

    /** Ciclo 74: deve transformar falha de infraestrutura em retry técnico sem nova tentativa cognitiva. */
    @Test
    void failCreatesTechnicalRetryForInfrastructureFailureInCycle74() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNichoCnaeV2StageExecution execution = execution(74L, 1, 0, 3);
        when(stageExecutionRepository.findByIdAndStageCode(74L, "candidate-generator"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> {
            OprmNichoCnaeV2StageExecution saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(75L);
            }
            return saved;
        });

        CandidateGeneratorFailureResponse result = service.fail(
                74L,
                new CandidateGeneratorFailureRequest(
                        OprmNichoCnaeV2FailureType.INFRASTRUCTURE,
                        "OPENAI_BROKEN_PIPE",
                        "broken pipe",
                        "{\"cycle\":74}"));

        assertThat(result.stageExecutionId()).isEqualTo("74");
        assertThat(result.status()).isEqualTo("TECHNICAL_RETRY_SCHEDULED");
        assertThat(result.retryStageExecutionId()).isEqualTo("75");
        assertThat(result.attemptNumber()).isEqualTo(1);
        assertThat(result.technicalRetryNumber()).isEqualTo(1);
        assertThat(execution.getFailureType()).isEqualTo(OprmNichoCnaeV2FailureType.INFRASTRUCTURE);
        ArgumentCaptor<OprmNichoCnaeV2StageExecution> executionCaptor =
                ArgumentCaptor.forClass(OprmNichoCnaeV2StageExecution.class);
        verify(stageExecutionRepository, org.mockito.Mockito.times(2)).save(executionCaptor.capture());
        OprmNichoCnaeV2StageExecution retry = executionCaptor.getAllValues().get(1);
        assertThat(retry.getAttemptNumber()).isEqualTo(1);
        assertThat(retry.getTechnicalRetryNumber()).isEqualTo(1);
        assertThat(retry.getKnowledgeVersion()).isEqualTo(3);
        assertThat(retry.getStatus()).isEqualTo(OprmNichoCnaeV2StageExecutionStatus.PENDING);
    }

    /** Deve classificar falha de qualidade sem agendar retry técnico automático. */
    @Test
    void failDoesNotRetryQualityFailure() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNichoCnaeV2StageExecution execution = execution(80L, 2, 0, 4);
        when(stageExecutionRepository.findByIdAndStageCode(80L, "candidate-generator"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateGeneratorFailureResponse result = service.fail(
                80L,
                new CandidateGeneratorFailureRequest(
                        OprmNichoCnaeV2FailureType.QUALITY,
                        "ROUTINE_TOO_GENERIC",
                        "síntese genérica",
                        null));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.retryStageExecutionId()).isNull();
        assertThat(execution.getFailureType()).isEqualTo(OprmNichoCnaeV2FailureType.QUALITY);
        assertThat(execution.getStatus()).isEqualTo(OprmNichoCnaeV2StageExecutionStatus.FAILED);
    }

    /** Deve bloquear a transição de materialização automática enquanto a feature flag da v2 estiver desligada. */
    @Test
    void completeBlocksMeiAudienceReadyMaterializationWhenCalibrationFlagIsDisabled() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNichoCnaeV2StageExecution execution = execution(90L, 1, 0, 1);
        when(stageExecutionRepository.findByIdAndStageCode(90L, "candidate-generator"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateGeneratorCompletionResponse result = service.complete(
                90L, new CandidateGeneratorCompletionRequest("MEI_AUDIENCE_READY", "MATERIALIZAR_NICHO", "{}"));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.nextStageCode()).isNull();
        assertThat(result.materializationEnabled()).isFalse();
    }

    /** Deve apenas persistir a próxima etapa recebida do executor quando a calibração liberar. */
    @Test
    void completeRoutesMeiAudienceReadyMaterializationWhenFlagIsEnabled() {
        BackendCandidateGeneratorService service = service(true, true);
        OprmNichoCnaeV2StageExecution execution = execution(91L, 1, 0, 1);
        execution.setMaterializationEnabled(true);
        when(stageExecutionRepository.findByIdAndStageCode(91L, "candidate-generator"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateGeneratorCompletionResponse result = service.complete(
                91L, new CandidateGeneratorCompletionRequest("MEI_AUDIENCE_READY", "MATERIALIZAR_NICHO", "{}", "enriched-niche-materializer"));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.nextStageCode()).isEqualTo("enriched-niche-materializer");
        assertThat(result.materializationEnabled()).isTrue();
    }

    /** Cria o service testável com flags explícitas para a v2. */
    private BackendCandidateGeneratorService service(boolean v2Enabled, boolean materializationEnabled) {
        return new BackendCandidateGeneratorService(
                nicheCandidateRepository, stageExecutionRepository, v2Enabled, materializationEnabled);
    }

    /** Monta candidato de nicho mínimo para geração de pendência da v2. */
    private OprmNicheCandidate candidate(Long id, String cnaeCode) {
        OprmNicheCandidate candidate = new OprmNicheCandidate();
        candidate.setId(id);
        candidate.setCnaeCode(cnaeCode);
        candidate.setCnaeDescription("Serviços de beleza");
        candidate.setCandidateNicheName("Manicure autônoma");
        candidate.setOpportunityScore(new BigDecimal("92.50"));
        candidate.setRoutineResearchStatus("PENDING");
        candidate.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        candidate.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return candidate;
    }

    /** Monta execução persistida mínima para callbacks de conclusão e falha. */
    private OprmNichoCnaeV2StageExecution execution(Long id, int attemptNumber, int technicalRetryNumber, int knowledgeVersion) {
        OprmNichoCnaeV2StageExecution execution = new OprmNichoCnaeV2StageExecution();
        execution.setId(id);
        execution.setJobId("nichocnae-v2-candidate-" + id);
        execution.setSourceNicheId(id);
        execution.setCnaeCode("9602501");
        execution.setStageCode("candidate-generator");
        execution.setAttemptNumber(attemptNumber);
        execution.setTechnicalRetryNumber(technicalRetryNumber);
        execution.setKnowledgeVersion(knowledgeVersion);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        execution.setMaterializationEnabled(false);
        execution.setCreatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-19T10:00:00Z"));
        return execution;
    }
}
