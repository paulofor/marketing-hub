package com.marketinghub.oprmcoletormei.nichocnae.v3.cnaeintake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.oprm.nichocnae.PipelineNichoCnae;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseRequest;
import com.marketinghub.repository.jpa.oprm.market.OprmCnpjCnaeDimRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.PipelineNichoCnaeRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.math.BigDecimal;
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

    @Mock
    private OpenAiPricingService openAiPricingService;

    private BackendCnaeIntakeService service;

    @BeforeEach
    void setUp() {
        service = new BackendCnaeIntakeService(repository, cnaeRepository, pipelineNichoCnaeRepository, openAiPricingService);
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
        OprmNichoCnaeV3StageExecution nextExecution = executionCaptor.getAllValues().get(1);
        assertThat(nextExecution.getStageCode()).isEqualTo("persona-candidate-generator");
        assertThat(nextExecution.getInputPayload()).isEqualTo("{\"ok\":true}");
        verify(cnaeRepository).save(cnae);
    }

    /** Garante que a etapa inicial nasce com o nome completo do CNAE no input funcional. */
    @Test
    void startCreatesQualifiedCnaeInputPayload() {
        OprmCnpjCnaeDim cnae = cnae();
        when(cnaeRepository.findById("4781400")).thenReturn(Optional.of(cnae));
        when(repository.save(any(OprmNichoCnaeV3StageExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.start("4781400");

        ArgumentCaptor<OprmNichoCnaeV3StageExecution> executionCaptor = ArgumentCaptor.forClass(OprmNichoCnaeV3StageExecution.class);
        verify(repository).save(executionCaptor.capture());
        assertThat(executionCaptor.getValue().getStageCode()).isEqualTo("cnae-intake");
        assertThat(executionCaptor.getValue().getInputPayload()).contains("\"cnaeDescription\":\"Comércio varejista de artigos do vestuário\"");
    }

    /** Garante que o callback de response grava a resposta final limpa da OpenAI na auditoria. */
    @Test
    void recebeResponseStoresCleanFinalResponse() {
        OprmCnpjCnaeDim cnae = cnae();
        when(cnaeRepository.findById("4781400")).thenReturn(Optional.of(cnae));
        when(pipelineNichoCnaeRepository.save(any(PipelineNichoCnae.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(openAiPricingService.estimateFlexCost("gpt-5.2", 10, 20)).thenReturn(new BigDecimal("0.0003"));
        String rawResponse = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_text",
                          "text": "resposta funcional limpa"
                        }
                      ]
                    }
                  ]
                }
                """;

        service.recebeResponse(
                "4781400",
                "job-4781400",
                new OprmNichoCnaeV3RecebeResponseRequest(rawResponse, null, 10L, 20L, new BigDecimal("9.9999"), "gpt-5.2"));

        ArgumentCaptor<PipelineNichoCnae> pipelineCaptor = ArgumentCaptor.forClass(PipelineNichoCnae.class);
        verify(pipelineNichoCnaeRepository).save(pipelineCaptor.capture());
        assertThat(pipelineCaptor.getValue().getResponse()).isEqualTo(rawResponse);
        assertThat(pipelineCaptor.getValue().getRespostaFinal()).isEqualTo("resposta funcional limpa");
        assertThat(pipelineCaptor.getValue().getCusto()).isEqualByComparingTo("0.0003");
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
