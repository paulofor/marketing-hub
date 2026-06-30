package com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import com.marketinghub.repository.jpa.oprm.market.OprmCnpjCnaeDimRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.PipelineNichoCnaeRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Testa o avanço backend da etapa persona-tournament do NichoCNAE v3. */
@ExtendWith(MockitoExtension.class)
class BackendPersonaTournamentServiceTest {
    @Mock
    private OprmNichoCnaeV3StageExecutionRepository repository;

    @Mock
    private OprmCnpjCnaeDimRepository cnaeRepository;

    @Mock
    private PipelineNichoCnaeRepository pipelineNichoCnaeRepository;

    @Mock
    private OpenAiPricingService openAiPricingService;

    private BackendPersonaTournamentService service;

    /** Inicializa o service canônico da etapa para cada teste. */
    @BeforeEach
    void setUp() {
        service = new BackendPersonaTournamentService(repository, cnaeRepository, pipelineNichoCnaeRepository, openAiPricingService);
    }

    /** Garante que o caminho rápido publica materialização sem depender de source-searcher. */
    @Test
    void completeAllowsFastLaneToPersonaRoutineMaterializer() {
        OprmNichoCnaeV3StageExecution execution = execution();
        OprmCnpjCnaeDim cnae = cnae();
        when(repository.findById(73L)).thenReturn(Optional.of(execution));
        when(repository.save(any(OprmNichoCnaeV3StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.existsByJobIdAndStageCode("job-8219999", "persona-routine-materializer")).thenReturn(false);
        when(cnaeRepository.findById("8219999")).thenReturn(Optional.of(cnae));

        service.complete(73L, "{\"winnerPersona\":{\"name\":\"Prestador de apoio administrativo MEI\"}}", "persona-routine-materializer");

        assertThat(cnae.getNichocnaePipelineStatus()).isEqualTo("INICIADO");
        assertThat(cnae.getNichocnaeCurrentStageCode()).isEqualTo("persona-routine-materializer");
        ArgumentCaptor<OprmNichoCnaeV3StageExecution> executionCaptor = ArgumentCaptor.forClass(OprmNichoCnaeV3StageExecution.class);
        verify(repository, times(2)).save(executionCaptor.capture());
        assertThat(executionCaptor.getAllValues().get(1).getStageCode()).isEqualTo("persona-routine-materializer");
        assertThat(executionCaptor.getAllValues().get(1).getInputPayload()).contains("Prestador de apoio administrativo MEI");
        verify(cnaeRepository).save(cnae);
    }

    /** Monta uma execução pendente da etapa persona-tournament. */
    private OprmNichoCnaeV3StageExecution execution() {
        OprmNichoCnaeV3StageExecution execution = new OprmNichoCnaeV3StageExecution();
        execution.setId(73L);
        execution.setJobId("job-8219999");
        execution.setCnaeCode("8219999");
        execution.setStageCode("persona-tournament");
        execution.setStatus(OprmNichoCnaeV3StageExecutionStatus.PENDING);
        execution.setAttemptNumber(1);
        execution.setKnowledgeVersion(1);
        execution.setCreatedAt(Instant.parse("2026-06-29T23:15:00Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-29T23:15:00Z"));
        return execution;
    }

    /** Monta o CNAE canônico afetado pelo avanço rápido do pipeline. */
    private OprmCnpjCnaeDim cnae() {
        OprmCnpjCnaeDim cnae = new OprmCnpjCnaeDim();
        cnae.setCnaeCode("8219999");
        cnae.setDescription("Preparação de documentos e serviços de apoio administrativo");
        cnae.setUpdatedAt(Instant.parse("2026-06-29T23:15:00Z"));
        return cnae;
    }
}
