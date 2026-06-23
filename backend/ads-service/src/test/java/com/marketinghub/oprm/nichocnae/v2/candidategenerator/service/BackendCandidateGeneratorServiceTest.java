package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteraction;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution.CandidateGeneratorCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution.CandidateGeneratorCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution.CandidateGeneratorFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution.CandidateGeneratorFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.pending.CandidateGeneratorPendingResponse;
import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteractionRepository;
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

    @Mock private OprmNichoCnaeV2OpenAiInteractionRepository openAiInteractionRepository;

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
        when(stageExecutionRepository.countByCnaeCodeAndStatusIn(eq("4781400"), any())).thenReturn(0L);
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

    /** Deve impedir novo job manual quando já existe execução aberta para o mesmo CNAE. */
    @Test
    void createForCnaeBlocksParallelOpenJobForSameCnae() {
        BackendCandidateGeneratorService service = service(true, false);
        when(stageExecutionRepository.countByCnaeCodeAndStatusIn(eq("4781400"), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createForCnae("4781400"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Já existe job NichoCNAE v2 aberto para este CNAE");

        verify(nicheCandidateRepository, never()).findManualRoutineResearchCandidateByCnaeCode(any(), any(Pageable.class));
        verify(stageExecutionRepository, never()).save(any(OprmNichoCnaeV2StageExecution.class));
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
        completed.setOutputPayload(
                "{\"tournamentDecision\":\"NO_VIABLE_SUBNICHE\",\"candidateCount\":0,\"finalistCount\":0,\"aiCostUsd\":0.015}");
        completed.setUpdatedAt(Instant.parse("2026-06-19T11:00:00Z"));
        when(stageExecutionRepository.findByCnaeCodeOrderByUpdatedAtDesc("4781400"))
                .thenReturn(List.of(open, completed));

        var result = service.listJobsForCnae("4781400");

        assertThat(result.cnaeUsedAi()).isTrue();
        assertThat(result.cnaeAiCostUsd()).isEqualByComparingTo(new BigDecimal("0.015"));
        assertThat(result.openJobs()).hasSize(1);
        assertThat(result.openJobs().getFirst().jobId()).isEqualTo("job-aberto");
        assertThat(result.openJobs().getFirst().currentStageCode()).isEqualTo("source-safety-filter");
        assertThat(result.completedJobs()).hasSize(1);
        assertThat(result.completedJobs().getFirst().jobId()).isEqualTo("job-concluido");
        assertThat(result.completedJobs().getFirst().currentStageCode()).isNull();
        assertThat(result.completedJobs().getFirst().finalDecision()).isEqualTo("NO_VIABLE_SUBNICHE");
        assertThat(result.completedJobs().getFirst().finalDecisionLabel()).isEqualTo("Encerrado sem subnicho viável");
        assertThat(result.completedJobs().getFirst().finalDecisionReason())
                .isEqualTo("O torneio terminou sem finalistas viáveis; candidatos=0, finalistas=0.");
        assertThat(result.completedJobs().getFirst().outcomeStatus()).isEqualTo("FAILURE");
        assertThat(result.completedJobs().getFirst().outcomeMessage()).contains("Fracasso controlado");
        assertThat(result.completedJobs().getFirst().actionLabel()).isEqualTo("Pesquisar outro recorte");
        assertThat(result.completedJobs().getFirst().actionUrl()).isEqualTo("/oprm/cnaes/4781400");
        assertThat(result.completedJobs().getFirst().usedAi()).isTrue();
        assertThat(result.completedJobs().getFirst().aiCostUsd()).isEqualByComparingTo(new BigDecimal("0.015"));
    }


    /** Deve expor na resposta do backend quando job aberto repete etapas e está em ciclo operacional. */
    @Test
    void listJobsForCnaeShowsOpenJobLoopDetectedByBackend() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNichoCnaeV2StageExecution firstPlanner = execution(401L, 1, 0, 1);
        firstPlanner.setJobId("job-em-ciclo");
        firstPlanner.setCnaeCode("4781400");
        firstPlanner.setStageCode("adaptive-query-planner");
        firstPlanner.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        firstPlanner.setUpdatedAt(Instant.parse("2026-06-22T12:00:00Z"));
        OprmNichoCnaeV2StageExecution firstTournament = execution(402L, 1, 0, 1);
        firstTournament.setJobId("job-em-ciclo");
        firstTournament.setCnaeCode("4781400");
        firstTournament.setStageCode("candidate-tournament");
        firstTournament.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        firstTournament.setUpdatedAt(Instant.parse("2026-06-22T12:01:00Z"));
        OprmNichoCnaeV2StageExecution secondPlanner = execution(403L, 1, 0, 1);
        secondPlanner.setJobId("job-em-ciclo");
        secondPlanner.setCnaeCode("4781400");
        secondPlanner.setStageCode("adaptive-query-planner");
        secondPlanner.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        secondPlanner.setUpdatedAt(Instant.parse("2026-06-22T12:02:00Z"));
        OprmNichoCnaeV2StageExecution openTournament = execution(404L, 1, 0, 1);
        openTournament.setJobId("job-em-ciclo");
        openTournament.setCnaeCode("4781400");
        openTournament.setStageCode("candidate-tournament");
        openTournament.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        openTournament.setUpdatedAt(Instant.parse("2026-06-22T12:03:00Z"));
        when(stageExecutionRepository.findByCnaeCodeOrderByUpdatedAtDesc("4781400"))
                .thenReturn(List.of(openTournament, secondPlanner, firstTournament, firstPlanner));

        var result = service.listJobsForCnae("4781400");

        assertThat(result.openJobs()).hasSize(1);
        var job = result.openJobs().getFirst();
        assertThat(job.loopDetected()).isTrue();
        assertThat(job.loopLabel()).isEqualTo("Em ciclo de pesquisa");
        assertThat(job.loopReason()).contains("O job repetiu 2 etapas");
        assertThat(job.repeatedStageCount()).isEqualTo(2);
    }

    /** Deve direcionar sucesso parcial da v2 para o relatório do job, sem misturar com a tela legada do CNAE. */
    @Test
    void listJobsForCnaePointsPartialSuccessToJobReport() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNichoCnaeV2StageExecution completed = execution(501L, 1, 0, 1);
        completed.setJobId("nichocnae-v2-candidate-2-job-13");
        completed.setCnaeCode("4781400");
        completed.setStageCode("adaptive-query-planner");
        completed.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        completed.setOutputPayload("{\"planDecision\":\"ENOUGH_INFORMATION_FOR_NEXT_PIPELINE\",\"aiCostUsd\":0}");
        completed.setUpdatedAt(Instant.parse("2026-06-22T23:51:00Z"));
        when(stageExecutionRepository.findByCnaeCodeOrderByUpdatedAtDesc("4781400"))
                .thenReturn(List.of(completed));

        var result = service.listJobsForCnae("4781400");

        assertThat(result.completedJobs()).hasSize(1);
        var job = result.completedJobs().getFirst();
        assertThat(job.outcomeStatus()).isEqualTo("SUCCESS");
        assertThat(job.actionLabel()).isEqualTo("Ver resultado do pipeline");
        assertThat(job.actionUrl())
                .isEqualTo("/oprm/cnaes/4781400/pipeline-v2/jobs/nichocnae-v2-candidate-2-job-13");
    }

    /** Deve detalhar cronologicamente as etapas do job para relatório de fracasso. */
    @Test
    void detailJobReturnsStageTimelineForFailureReport() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNichoCnaeV2StageExecution first = execution(201L, 1, 0, 1);
        first.setJobId("job-fracasso");
        first.setCnaeCode("4781400");
        first.setStageCode("candidate-generator");
        first.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        first.setOutputPayload("{\"candidateCount\":4}");
        first.setNextStageCode("candidate-tournament");
        first.setUpdatedAt(Instant.parse("2026-06-21T10:00:00Z"));
        OprmNichoCnaeV2StageExecution last = execution(202L, 1, 0, 1);
        last.setJobId("job-fracasso");
        last.setCnaeCode("4781400");
        last.setStageCode("candidate-tournament");
        last.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        last.setOutputPayload(
                "{\"tournamentDecision\":\"NO_VIABLE_SUBNICHE\",\"candidateCount\":4,\"finalistCount\":0}");
        last.setUpdatedAt(Instant.parse("2026-06-21T10:05:00Z"));
        when(stageExecutionRepository.findByJobIdOrderByCreatedAtAsc("job-fracasso")).thenReturn(List.of(first, last));

        var result = service.detailJob("job-fracasso");

        assertThat(result.jobId()).isEqualTo("job-fracasso");
        assertThat(result.outcomeStatus()).isEqualTo("FAILURE");
        assertThat(result.finalDecision()).isEqualTo("NO_VIABLE_SUBNICHE");
        assertThat(result.stages()).hasSize(2);
        assertThat(result.stages().getFirst().stageCode()).isEqualTo("candidate-generator");
        assertThat(result.stages().getFirst().outputPayload()).contains("candidateCount");
        assertThat(result.stages().get(1).stageCode()).isEqualTo("candidate-tournament");
    }

    /** Deve manter a decisão real do torneio quando o reprocessamento encerra o job sem ganho de informação. */
    @Test
    void listJobsForCnaeExplainsNoViableSubnicheAfterReprocessControllerEnd() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNichoCnaeV2StageExecution tournament = execution(121L, 3, 0, 3);
        tournament.setJobId("job-reprocessado");
        tournament.setCnaeCode("4781400");
        tournament.setStageCode("candidate-tournament");
        tournament.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        tournament.setOutputPayload(
                "{\"tournamentDecision\":\"NO_VIABLE_SUBNICHE\",\"candidateCount\":0,\"finalistCount\":0}");
        tournament.setUpdatedAt(Instant.parse("2026-06-20T23:44:53Z"));
        OprmNichoCnaeV2StageExecution reprocessEnd = execution(122L, 3, 0, 3);
        reprocessEnd.setJobId("job-reprocessado");
        reprocessEnd.setCnaeCode("4781400");
        reprocessEnd.setStageCode("reprocess-controller");
        reprocessEnd.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        reprocessEnd.setOutputPayload(
                "{\"executionMode\":\"STOP_NO_INFORMATION_GAIN\",\"nextStageCode\":\"end\"}");
        reprocessEnd.setUpdatedAt(Instant.parse("2026-06-20T23:44:54Z"));
        when(stageExecutionRepository.findByCnaeCodeOrderByUpdatedAtDesc("4781400"))
                .thenReturn(List.of(reprocessEnd, tournament));

        var result = service.listJobsForCnae("4781400");

        assertThat(result.completedJobs()).hasSize(1);
        assertThat(result.completedJobs().getFirst().finalDecision()).isEqualTo("NO_VIABLE_SUBNICHE");
        assertThat(result.completedJobs().getFirst().finalDecisionLabel()).isEqualTo("Encerrado sem subnicho viável");
        assertThat(result.completedJobs().getFirst().finalDecisionReason())
                .isEqualTo("O torneio terminou sem finalistas viáveis; candidatos=0, finalistas=0.");
        assertThat(result.completedJobs().getFirst().outcomeStatus()).isEqualTo("FAILURE");
        assertThat(result.completedJobs().getFirst().outcomeMessage())
                .contains("este job terminou sem subnicho viável");
        assertThat(result.completedJobs().getFirst().actionLabel()).isEqualTo("Pesquisar outro recorte");
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

    /** Deve gravar auditoria OpenAI no próprio service canônico sem delegar a service auxiliar. */
    @Test
    void completePersistsOpenAiAuditInCanonicalService() {
        BackendCandidateGeneratorService service = new BackendCandidateGeneratorService(
                nicheCandidateRepository, stageExecutionRepository, openAiInteractionRepository, true, true);
        OprmNichoCnaeV2StageExecution execution = execution(92L, 1, 0, 1);
        when(stageExecutionRepository.findByIdAndStageCode(92L, "candidate-generator"))
                .thenReturn(Optional.of(execution));
        when(stageExecutionRepository.save(any(OprmNichoCnaeV2StageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(openAiInteractionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.complete(
                92L,
                new CandidateGeneratorCompletionRequest(
                        "MEI_AUDIENCE_READY",
                        "MATERIALIZAR_NICHO",
                        "{}",
                        "enriched-niche-materializer",
                        List.of(new OpenAiInteractionAuditRequest(
                                "gpt-4.1-mini",
                                "flex",
                                100,
                                40,
                                140,
                                new BigDecimal("0.001234"),
                                "resp_123",
                                "{\"request\":true}",
                                "{\"response\":true}",
                                "completed",
                                null))));

        ArgumentCaptor<Iterable<OprmNichoCnaeV2OpenAiInteraction>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(openAiInteractionRepository).saveAll(captor.capture());
        OprmNichoCnaeV2OpenAiInteraction saved = captor.getValue().iterator().next();
        assertThat(saved.getStageExecutionId()).isEqualTo(92L);
        assertThat(saved.getJobId()).isEqualTo("nichocnae-v2-candidate-92");
        assertThat(saved.getStageCode()).isEqualTo("candidate-generator");
        assertThat(saved.getServiceTier()).isEqualTo("flex");
        assertThat(saved.getTotalTokens()).isEqualTo(140);
        assertThat(saved.getCostUsd()).isEqualByComparingTo("0.001234");
        assertThat(saved.getRawRequest()).contains("request");
        assertThat(saved.getRawResponse()).contains("response");
    }


    /** Deve cancelar job aberto preso e liberar o CNAE para nova execução manual. */
    @Test
    void cancelJobMarksOpenExecutionsAsCanceled() {
        BackendCandidateGeneratorService service = service(true, false);
        OprmNichoCnaeV2StageExecution open = execution(301L, 1, 2, 1);
        open.setJobId("job-preso");
        open.setCnaeCode("7319002");
        open.setStageCode("source-safety-filter");
        open.setStatus(OprmNichoCnaeV2StageExecutionStatus.TECHNICAL_RETRY_SCHEDULED);
        when(stageExecutionRepository.findByJobIdOrderByCreatedAtAsc("job-preso")).thenReturn(List.of(open));
        when(stageExecutionRepository.findByJobIdAndStatusIn(eq("job-preso"), any())).thenReturn(List.of(open));
        when(stageExecutionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.cancelJob("job-preso");

        assertThat(result.jobId()).isEqualTo("job-preso");
        assertThat(result.cnaeCode()).isEqualTo("7319002");
        assertThat(result.canceledExecutions()).isEqualTo(1);
        assertThat(result.status()).isEqualTo("CANCELED");
        assertThat(open.getStatus()).isEqualTo(OprmNichoCnaeV2StageExecutionStatus.CANCELED);
        assertThat(open.getErrorMessage()).contains("Cancelado manualmente");
        verify(stageExecutionRepository).saveAll(List.of(open));
    }

    /** Cria o service testável com flags explícitas para a v2. */
    private BackendCandidateGeneratorService service(boolean v2Enabled, boolean materializationEnabled) {
        return new BackendCandidateGeneratorService(
                nicheCandidateRepository, stageExecutionRepository, openAiInteractionRepository, v2Enabled, materializationEnabled);
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
