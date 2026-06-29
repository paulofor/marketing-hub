package com.marketinghub.oprmcoletormei.nichocnae.v3.sourcesearcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida regras backend da etapa source-searcher do NichoCNAE v3. */
@ExtendWith(MockitoExtension.class)
class BackendSourceSearcherV3ServiceTest {
    @Mock
    private OprmNichoCnaeV3StageExecutionRepository repository;

    @Mock
    private OprmCnpjCnaeDimRepository cnaeRepository;

    @Mock
    private PipelineNichoCnaeRepository pipelineNichoCnaeRepository;

    @Mock
    private OpenAiPricingService openAiPricingService;

    private BackendSourceSearcherV3Service service;

    @BeforeEach
    void setUp() {
        service = new BackendSourceSearcherV3Service(repository, cnaeRepository, pipelineNichoCnaeRepository, openAiPricingService);
    }

    /** Marca o CNAE como concluído quando source-searcher encerra sem próxima etapa, removendo pendência antiga. */
    @Test
    void completeWithoutNextStageMarksCnaeAsCompleted() {
        OprmNichoCnaeV3StageExecution execution = sourceSearcherExecution();
        OprmCnpjCnaeDim cnae = cnae();
        when(repository.findById(280L)).thenReturn(Optional.of(execution));
        when(repository.save(any(OprmNichoCnaeV3StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cnaeRepository.findById("4781400")).thenReturn(Optional.of(cnae));

        service.complete(280L, "{\"nextStageCode\":\"\"}", "");

        assertThat(cnae.getNichocnaeCurrentStageCode()).isEqualTo("source-searcher");
        assertThat(cnae.getNichocnaePipelineStatus()).isEqualTo("CONCLUIDO");
        assertThat(cnae.getNichocnaePipelineUpdatedAt()).isNotNull();
        verify(cnaeRepository).save(cnae);
    }

    /** Marca o CNAE como falha quando source-searcher é cancelado ou reporta erro, removendo pendência antiga. */
    @Test
    void failMarksCnaeAsFailed() {
        OprmNichoCnaeV3StageExecution execution = sourceSearcherExecution();
        OprmCnpjCnaeDim cnae = cnae();
        when(repository.findById(280L)).thenReturn(Optional.of(execution));
        when(repository.save(any(OprmNichoCnaeV3StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cnaeRepository.findById("4781400")).thenReturn(Optional.of(cnae));

        service.fail(280L, "source-searcher excedeu 120000ms");

        assertThat(cnae.getNichocnaeCurrentStageCode()).isEqualTo("source-searcher");
        assertThat(cnae.getNichocnaePipelineStatus()).isEqualTo("FALHA");
        assertThat(cnae.getNichocnaePipelineUpdatedAt()).isNotNull();
        verify(cnaeRepository).save(cnae);
    }

    /** Monta execução pendente de source-searcher para validar transições de status. */
    private OprmNichoCnaeV3StageExecution sourceSearcherExecution() {
        OprmNichoCnaeV3StageExecution execution = new OprmNichoCnaeV3StageExecution();
        execution.setId(280L);
        execution.setJobId("job-4781400");
        execution.setCnaeCode("4781400");
        execution.setStageCode("source-searcher");
        execution.setStatus(OprmNichoCnaeV3StageExecutionStatus.PENDING);
        execution.setCreatedAt(Instant.parse("2026-06-29T19:49:39Z"));
        execution.setUpdatedAt(Instant.parse("2026-06-29T19:49:39Z"));
        return execution;
    }

    /** Monta CNAE iniciado em source-searcher como estava no caso real investigado. */
    private OprmCnpjCnaeDim cnae() {
        OprmCnpjCnaeDim cnae = new OprmCnpjCnaeDim();
        cnae.setCnaeCode("4781400");
        cnae.setNichocnaeCurrentStageCode("source-searcher");
        cnae.setNichocnaePipelineStatus("INICIADO");
        cnae.setUpdatedAt(Instant.parse("2026-06-29T19:49:39Z"));
        return cnae;
    }
}
