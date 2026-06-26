package com.marketinghub.oprm.nichocnae.v3.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v3.personaroutinematerializer.gateway.PersonaRoutineMaterializerNicheGateway;
import com.marketinghub.oprm.nichocnae.v3.personaroutinematerializer.service.BackendPersonaRoutineMaterializerService;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Testa a prévia e a confirmação manual do progresso NichoCNAE v3. */
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
        service = new BackendNichoCnaeV3ProgressService(repository, nicheGateway, materializerService, new ObjectMapper());
    }

    /** Deve bloquear a confirmação quando o quality-gate traz apenas payload técnico sem informações de mercado. */
    @Test
    void latestByCnaeBlocksFinalizationWhenFunctionalEvidenceIsMissing() {
        OprmNichoCnaeV3StageExecution intake = execution(1L, "cnae-intake", OprmNichoCnaeV3StageExecutionStatus.COMPLETED, "{}");
        OprmNichoCnaeV3StageExecution qualityGate = execution(9L, "quality-gate", OprmNichoCnaeV3StageExecutionStatus.COMPLETED, "{\"stage\":\"quality-gate\",\"status\":\"QUALIDADE_APROVADA\",\"jobId\":\"nichocnae-v3-4781400\"}");
        when(repository.findTop1ByCnaeCodeAndStageCodeOrderByCreatedAtDesc("4781400", "cnae-intake")).thenReturn(Optional.of(intake));
        when(repository.findByJobIdOrderByCreatedAtAsc("nichocnae-v3-4781400")).thenReturn(List.of(intake, qualityGate));
        when(nicheGateway.findPersonaRoutineMaterializedNiche(anyString(), anyString())).thenReturn(Optional.empty());

        NichoCnaeV3JobProgressResponse response = service.latestByCnae("4781400");

        assertThat(response.finalizationReview()).isNotNull();
        assertThat(response.finalizationReview().canConfirmFinalization()).isFalse();
        assertThat(response.finalizationReview().blockingReason()).contains("rotina observada", "tarefas diárias", "persona", "evidências");
        assertThat(response.finalizationReview().enrichedNicheInformation()).contains("Evidências: Não informadas.");
        assertThat(response.finalizationReview().enrichedNicheInformation()).doesNotContain("jobId");
    }

    /** Deve rejeitar a confirmação para impedir materialização de nicho vazio ou técnico. */
    @Test
    void confirmFinalizationRejectsMissingFunctionalEvidence() {
        OprmNichoCnaeV3StageExecution intake = execution(1L, "cnae-intake", OprmNichoCnaeV3StageExecutionStatus.COMPLETED, "{}");
        OprmNichoCnaeV3StageExecution qualityGate = execution(9L, "quality-gate", OprmNichoCnaeV3StageExecutionStatus.COMPLETED, "{\"stage\":\"quality-gate\",\"status\":\"QUALIDADE_APROVADA\"}");
        when(repository.findTop1ByCnaeCodeAndStageCodeOrderByCreatedAtDesc("4781400", "cnae-intake")).thenReturn(Optional.of(intake));
        when(repository.findByJobIdAndStageCode("nichocnae-v3-4781400", "quality-gate")).thenReturn(Optional.of(qualityGate));

        assertThatThrownBy(() -> service.confirmFinalization("4781400"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não trouxe informações funcionais suficientes");
        verify(materializerService, never()).create(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /** Monta uma execução de etapa v3 para cenários de progresso. */
    private OprmNichoCnaeV3StageExecution execution(Long id, String stageCode, OprmNichoCnaeV3StageExecutionStatus status, String outputPayload) {
        OprmNichoCnaeV3StageExecution execution = new OprmNichoCnaeV3StageExecution();
        execution.setId(id);
        execution.setJobId("nichocnae-v3-4781400");
        execution.setCnaeCode("4781400");
        execution.setStageCode(stageCode);
        execution.setStatus(status);
        execution.setOutputPayload(outputPayload);
        execution.setAttemptNumber(1);
        execution.setKnowledgeVersion(1);
        execution.setCreatedAt(Instant.parse("2026-06-25T21:08:56Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-25T21:08:56Z"));
        return execution;
    }
}
