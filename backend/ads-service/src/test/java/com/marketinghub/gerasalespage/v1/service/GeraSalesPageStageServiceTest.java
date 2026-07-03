package com.marketinghub.gerasalespage.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentAiPromptSchemaUsageService;
import com.marketinghub.aiprompt.AiPromptSchemaTemplate;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.aiprompt.AiPromptSchemaTemplateRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida controle de fila e rebuild do GeraSalesPage v1. */
@ExtendWith(MockitoExtension.class)
class GeraSalesPageStageServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private GeraSalesPageStageExecutionRepository executionRepository;
    @Mock
    private AiPromptSchemaTemplateRepository templateRepository;
    @Mock
    private GeraSalesPagePublicationAuditService publicationAuditService;
    @Mock
    private ExperimentAiPromptSchemaUsageService promptSchemaUsageService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private GeraSalesPageStageService service;

    /** Inicializa service com ObjectMapper real para validar parsing do quality review. */
    @BeforeEach
    void setUp() {
        service = new GeraSalesPageStageService(
                experimentRepository,
                executionRepository,
                templateRepository,
                publicationAuditService,
                promptSchemaUsageService,
                objectMapper);
    }

    /** Deve substituir execuções antigas e criar uma primeira etapa nova. */
    @Test
    void rebuildShouldReplacePreviousExecutionsAndStartFreshPipeline() {
        Experiment experiment = new Experiment();
        experiment.setId(53L);
        experiment.setFollowUpActionUrl("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc");
        GeraSalesPageStageExecution previous = GeraSalesPageStageExecution.builder()
                .idJob("old-job")
                .experimentId(53L)
                .stageCode(GeraSalesPageStageCode.PUBLICATION_PACKAGE.code())
                .status("CONCLUIDO")
                .executionRequestedAt(Instant.now().minusSeconds(60))
                .build();

        when(experimentRepository.findById(53L)).thenReturn(Optional.of(experiment));
        when(executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(53L)).thenReturn(List.of(previous));
        AiPromptSchemaTemplate activeTemplate = template(GeraSalesPageStageCode.OFFER_BRIEF.code());
        when(templateRepository.findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(
                "gera-sales-page-v1", GeraSalesPageStageCode.OFFER_BRIEF.code()))
                .thenReturn(Optional.of(activeTemplate));
        when(executionRepository.save(any(GeraSalesPageStageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeraSalesPageStartResponse response = service.rebuild(53L);

        assertThat(previous.getStatus()).isEqualTo("SUBSTITUIDO");
        assertThat(previous.getErrorMessage()).contains("substituída por rebuild");
        verify(executionRepository).saveAll(List.of(previous));
        ArgumentCaptor<GeraSalesPageStageExecution> newExecution =
                ArgumentCaptor.forClass(GeraSalesPageStageExecution.class);
        verify(executionRepository).save(newExecution.capture());
        assertThat(newExecution.getValue().getStageCode()).isEqualTo(GeraSalesPageStageCode.OFFER_BRIEF.code());
        assertThat(newExecution.getValue().getStatus()).isEqualTo("INICIADO");
        verify(promptSchemaUsageService).linkSalesPageTemplate(
                53L,
                activeTemplate,
                GeraSalesPageStageCode.OFFER_BRIEF.code(),
                newExecution.getValue().getIdJob());
        assertThat(response.stageCode()).isEqualTo(GeraSalesPageStageCode.OFFER_BRIEF.code());
    }

    /** Deve bloquear a publicação quando a revisão comercial reprova a página. */
    @Test
    void receiveResultShouldFailQualityReviewWhenNotApproved() {
        GeraSalesPageStageExecution execution = GeraSalesPageStageExecution.builder()
                .idJob("review-job")
                .experimentId(55L)
                .stageCode(GeraSalesPageStageCode.CHECKOUT_QUALITY_REVIEW.code())
                .status("AGUARDANDO_RETORNO_OPENAI")
                .executionRequestedAt(Instant.now())
                .build();
        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc("review-job"))
                .thenReturn(Optional.of(execution));

        service.receiveResult(
                "review-job",
                new GeraSalesPageResultRequest(
                        55L,
                        GeraSalesPageStageCode.CHECKOUT_QUALITY_REVIEW.code(),
                        "{\"approved\":false,\"blockers\":[\"amostra e produto pago estão confusos\"],\"recommendation\":\"refazer\"}",
                        "{\"raw\":true}",
                        120,
                        80,
                        java.math.BigDecimal.valueOf(0.01),
                        "openai-job",
                        null,
                        null));

        assertThat(execution.getStatus()).isEqualTo("FALHA");
        assertThat(execution.getErrorMessage()).contains("Quality review reprovou");
        assertThat(execution.getErrorDetail()).contains("amostra e produto pago");
        verify(publicationAuditService, never()).snapshotPublication(any());
        verify(executionRepository, never()).findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                55L,
                GeraSalesPageStageCode.PUBLICATION_PACKAGE.code());
    }

    /** Cria template mínimo de prompt/schema para etapa de teste. */
    private AiPromptSchemaTemplate template(String stageCode) {
        return AiPromptSchemaTemplate.builder()
                .templateKey("template-" + stageCode)
                .pipelineCode("gera-sales-page-v1")
                .stageCode(stageCode)
                .version("v1")
                .openAiModel("gpt-test")
                .schemaName("schema")
                .promptMarkdownContent("# Prompt")
                .schemaJson("{\"type\":\"object\"}")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
