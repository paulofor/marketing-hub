package com.marketinghub.oprmcoletormei.nichocnae.v3.cnaeintake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

/** Testa a etapa inicial do pipeline NichoCNAE v3 no backend. */
@ExtendWith(MockitoExtension.class)
class BackendCnaeIntakeServiceTest {
    @Mock
    private OprmNichoCnaeV3StageExecutionRepository repository;

    @Mock
    private OprmCnpjCnaeDimRepository cnaeRepository;

    @Mock
    private PipelineNichoCnaeRepository pipelineNichoCnaeRepository;

    private BackendCnaeIntakeService service;

    @BeforeEach
    void setUp() {
        service = new BackendCnaeIntakeService(repository, cnaeRepository, pipelineNichoCnaeRepository);
    }

    /** Garante que a conclusão com sucesso publica a próxima etapa no cadastro canônico de CNAE. */
    @Test
    void completeUpdatesCnaePipelineToNextStage() {
        OprmNichoCnaeV3StageExecution execution = execution();
        OprmCnpjCnaeDim cnae = cnae();
        when(repository.findById(10L)).thenReturn(Optional.of(execution));
        when(repository.save(any(OprmNichoCnaeV3StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.existsByJobIdAndStageCode("job-4781400", "persona-candidate-generator")).thenReturn(false);
        when(cnaeRepository.findById("4781400")).thenReturn(Optional.of(cnae));

        service.complete(10L, "{\"ok\":true}", "persona-candidate-generator");

        assertThat(cnae.getNichocnaePipelineStatus()).isEqualTo("INICIADO");
        assertThat(cnae.getNichocnaeCurrentStageCode()).isEqualTo("persona-candidate-generator");
        assertThat(cnae.getNichocnaePipelineUpdatedAt()).isNotNull();
        ArgumentCaptor<OprmNichoCnaeV3StageExecution> executionCaptor = ArgumentCaptor.forClass(OprmNichoCnaeV3StageExecution.class);
        verify(repository, times(2)).save(executionCaptor.capture());
        assertThat(executionCaptor.getAllValues().get(1).getStageCode()).isEqualTo("persona-candidate-generator");
        verify(cnaeRepository).save(cnae);
    }

    /** Monta uma execução pendente da etapa inicial. */
    private OprmNichoCnaeV3StageExecution execution() {
        OprmNichoCnaeV3StageExecution execution = new OprmNichoCnaeV3StageExecution();
        execution.setId(10L);
        execution.setJobId("job-4781400");
        execution.setCnaeCode("4781400");
        execution.setStageCode("cnae-intake");
        execution.setStatus(OprmNichoCnaeV3StageExecutionStatus.PENDING);
        execution.setAttemptNumber(1);
        execution.setKnowledgeVersion(1);
        execution.setCreatedAt(Instant.parse("2026-06-25T21:08:56Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-25T21:08:56Z"));
        return execution;
    }

    /** Monta o CNAE canônico afetado pelo avanço do pipeline. */
    private OprmCnpjCnaeDim cnae() {
        OprmCnpjCnaeDim cnae = new OprmCnpjCnaeDim();
        cnae.setCnaeCode("4781400");
        cnae.setDescription("Comércio varejista de artigos do vestuário");
        cnae.setUpdatedAt(Instant.parse("2026-06-25T21:08:56Z"));
        return cnae;
    }
}
