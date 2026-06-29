package com.marketinghub.oprmcoletormei.nichocnae.v3.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.gateway.PersonaRoutineMaterializerNicheGateway;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.service.BackendPersonaRoutineMaterializerService;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Testa a leitura de progresso e a trava de finalização do pipeline NichoCNAE v3. */
@ExtendWith(MockitoExtension.class)
class BackendNichoCnaeV3ProgressServiceTest {
    @Mock
    private OprmNichoCnaeV3StageExecutionRepository repository;

    @Mock
    private PersonaRoutineMaterializerNicheGateway nicheGateway;

    @Mock
    private BackendPersonaRoutineMaterializerService materializerService;

    private BackendNichoCnaeV3ProgressService service;

    @BeforeEach
    void setUp() {
        service = new BackendNichoCnaeV3ProgressService(
                repository, nicheGateway, materializerService, new ObjectMapper());
    }

    /** Garante que gate concluído, mas bloqueado funcionalmente, não abre conferência final. */
    @Test
    void latestByCnaeDoesNotExposeFinalizationReviewWhenQualityGateBlocked() {
        OprmNichoCnaeV3StageExecution intake = execution(1L, "cnae-intake", OprmNichoCnaeV3StageExecutionStatus.COMPLETED, "{}");
        OprmNichoCnaeV3StageExecution qualityGate = execution(
                9L,
                "quality-gate",
                OprmNichoCnaeV3StageExecutionStatus.COMPLETED,
                "{\"stage\":\"quality-gate\",\"status\":\"QUALIDADE_BLOQUEADA\",\"approved\":false,\"decisionReason\":\"Faltam tarefas e fonte rastreável.\"}");
        when(repository.findTop1ByCnaeCodeAndStageCodeOrderByCreatedAtDesc("4781400", "cnae-intake"))
                .thenReturn(Optional.of(intake));
        when(repository.findByJobIdOrderByCreatedAtAsc("job-4781400"))
                .thenReturn(List.of(intake, qualityGate));

        NichoCnaeV3JobProgressResponse response = service.latestByCnae("4781400");

        assertThat(response.finalizationReview()).isNull();
        verify(nicheGateway, never()).findPersonaRoutineMaterializedNiche(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /** Garante que a confirmação manual não libera etapa final quando o gate reprovou o material. */
    @Test
    void confirmFinalizationRejectsBlockedQualityGate() {
        OprmNichoCnaeV3StageExecution intake = execution(1L, "cnae-intake", OprmNichoCnaeV3StageExecutionStatus.COMPLETED, "{}");
        OprmNichoCnaeV3StageExecution qualityGate = execution(
                9L,
                "quality-gate",
                OprmNichoCnaeV3StageExecutionStatus.COMPLETED,
                "{\"stage\":\"quality-gate\",\"status\":\"QUALIDADE_BLOQUEADA\",\"approved\":false}");
        when(repository.findTop1ByCnaeCodeAndStageCodeOrderByCreatedAtDesc("4781400", "cnae-intake"))
                .thenReturn(Optional.of(intake));
        when(repository.findByJobIdAndStageCode("job-4781400", "quality-gate"))
                .thenReturn(Optional.of(qualityGate));

        assertThatThrownBy(() -> service.confirmFinalization("4781400"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(materializerService, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    /** Monta execução persistida mínima para leitura do progresso v3. */
    private OprmNichoCnaeV3StageExecution execution(
            Long id, String stageCode, OprmNichoCnaeV3StageExecutionStatus status, String outputPayload) {
        OprmNichoCnaeV3StageExecution execution = new OprmNichoCnaeV3StageExecution();
        execution.setId(id);
        execution.setJobId("job-4781400");
        execution.setCnaeCode("4781400");
        execution.setStageCode(stageCode);
        execution.setStatus(status);
        execution.setAttemptNumber(1);
        execution.setKnowledgeVersion(1);
        execution.setInputPayload("{}");
        execution.setOutputPayload(outputPayload);
        execution.setCreatedAt(Instant.parse("2026-06-29T08:00:00Z").plusSeconds(id));
        execution.setUpdatedAt(Instant.parse("2026-06-29T08:01:00Z").plusSeconds(id));
        return execution;
    }
}
