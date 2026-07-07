package com.marketinghub.gerasalespage.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationStageAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationStageAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Valida o snapshot historico de publicacoes do GeraSalesPage v1. */
@ExtendWith(MockitoExtension.class)
class GeraSalesPagePublicationAuditServiceTest {
    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private GeraSalesPageStageExecutionRepository executionRepository;
    @Mock
    private GeraSalesPagePublicationAuditRepository publicationRepository;
    @Mock
    private GeraSalesPagePublicationStageAuditRepository publicationStageRepository;
    @Mock
    private LeadPortalFlowRepository leadPortalFlowRepository;
    @Mock
    private LeadPortalFlowPublisher leadPortalFlowPublisher;
    @Mock
    private LeadPortalPublicUrlResolver leadPortalPublicUrlResolver;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @InjectMocks
    private GeraSalesPagePublicationAuditService service;

    /** Deve salvar a pagina final junto dos prompts e schemas usados pelas etapas. */
    @Test
    void shouldSnapshotPublicationWithStagePromptsAndSchemas() {
        Experiment experiment = new Experiment();
        experiment.setId(53L);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp53.html");
        GeraSalesPageStageExecution offer = execution(
                "job-offer",
                GeraSalesPageStageCode.OFFER_BRIEF.code(),
                "{\"summary\":\"oferta\"}",
                Instant.parse("2026-07-01T10:00:00Z"));
        GeraSalesPageStageExecution publication = execution(
                "job-publication",
                GeraSalesPageStageCode.PUBLICATION_PACKAGE.code(),
                "{\"html\":\"" + salesPageHtml("Venda") + "\",\"checkoutUrl\":\"https://mp.test/checkout\"}",
                Instant.parse("2026-07-01T10:10:00Z"));

        when(publicationRepository.existsByPublicationJobId("job-publication")).thenReturn(false);
        when(experimentRepository.findById(53L)).thenReturn(Optional.of(experiment));
        when(executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(53L))
                .thenReturn(List.of(offer, publication));
        when(leadPortalFlowRepository.findBySlug("exp-53-gerasalespage-v1")).thenReturn(Optional.empty());
        when(leadPortalFlowRepository.save(any(LeadPortalFlow.class)))
                .thenAnswer(invocation -> {
                    LeadPortalFlow flow = invocation.getArgument(0);
                    flow.setId(53L);
                    return flow;
                });
        when(leadPortalPublicUrlResolver.resolve(any(LeadPortalFlow.class)))
                .thenReturn("https://oportunidadebrasil.shop/flows/exp-53-gerasalespage-v1");
        when(publicationRepository.save(any(GeraSalesPagePublicationAudit.class)))
                .thenAnswer(invocation -> {
                    GeraSalesPagePublicationAudit audit = invocation.getArgument(0);
                    audit.setId(10L);
                    return audit;
                });

        service.snapshotPublication(publication);

        ArgumentCaptor<GeraSalesPagePublicationAudit> audit =
                ArgumentCaptor.forClass(GeraSalesPagePublicationAudit.class);
        verify(publicationRepository).save(audit.capture());
        ArgumentCaptor<LeadPortalFlow> flow = ArgumentCaptor.forClass(LeadPortalFlow.class);
        verify(leadPortalFlowPublisher).publish(flow.capture());
        ArgumentCaptor<List<GeraSalesPagePublicationStageAudit>> stages = ArgumentCaptor.forClass(List.class);
        verify(publicationStageRepository).saveAll(stages.capture());
        assertThat(flow.getValue().getSlug()).isEqualTo("exp-53-gerasalespage-v1");
        assertThat(flow.getValue().getCustomFormHtml()).contains("Venda");
        assertThat(experiment.getFollowUpActionUrl())
                .isEqualTo("https://oportunidadebrasil.shop/flows/exp-53-gerasalespage-v1");
        assertThat(audit.getValue().getPublicationJobId()).isEqualTo("job-publication");
        assertThat(audit.getValue().getSalesPageUrl())
                .isEqualTo("https://oportunidadebrasil.shop/flows/exp-53-gerasalespage-v1");
        assertThat(audit.getValue().getCheckoutUrl()).isEqualTo("https://mp.test/checkout");
        assertThat(audit.getValue().getHtml()).contains("Venda");
        assertThat(audit.getValue().getHtml()).contains("data-track-section");
        assertThat(audit.getValue().getHtml()).contains("data-mh-sales-page-analytics");
        assertThat(audit.getValue().getHtml()).contains("section_view_time");
        assertThat(flow.getValue().getCustomFormHtml()).contains("data-track-section");
        assertThat(stages.getValue()).hasSize(2);
        assertThat(stages.getValue().getFirst().getPublicationAuditId()).isEqualTo(10L);
        assertThat(stages.getValue().getFirst().getStageCode()).isEqualTo("sales-page-offer-brief");
        assertThat(stages.getValue().getFirst().getPrompt()).isEqualTo("prompt job-offer");
        assertThat(stages.getValue().getFirst().getSchemaJson()).isEqualTo("{\"type\":\"object\"}");
    }

    /** Deve publicar Produto IA personalizado dentro do funil de coleta já vinculado ao experimento. */
    @Test
    void shouldPublishPersonalizedSampleSalesPageIntoLeadPortalFunnel() throws Exception {
        LeadPortalFlow flow = LeadPortalFlow.builder()
                .id(37L)
                .slug("product-ai-exp-55-personalized-sample")
                .name("Produto IA exp 55")
                .questions(List.of(
                        LeadPortalFlowQuestion.builder()
                                .title("Qual é o seu nome?")
                                .dataKey("nome")
                                .type(LeadPortalQuestionType.TEXT)
                                .required(true)
                                .placeholder("Seu nome")
                                .build(),
                        LeadPortalFlowQuestion.builder()
                                .title("Qual é o seu e-mail?")
                                .dataKey("email")
                                .type(LeadPortalQuestionType.EMAIL)
                                .required(true)
                                .placeholder("voce@email.com")
                                .build()))
                .build();
        Experiment experiment = new Experiment();
        experiment.setId(53L);
        experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
        experiment.setLeadPortalFlow(flow);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/checkout-antigo");

        GeraSalesPageStageExecution publication = execution(
                "job-publication",
                GeraSalesPageStageCode.PUBLICATION_PACKAGE.code(),
                "{\"html\":\"" + salesPageHtml("Venda Produto IA")
                        + "<iframe src='https://oportunidadebrasil.shop/flows/product-ai-exp-55-personalized-sample'></iframe>"
                        + "\",\"checkoutUrl\":\"https://pagamentopalf.site/checkout\"}",
                Instant.parse("2026-07-01T10:10:00Z"));

        when(publicationRepository.existsByPublicationJobId("job-publication")).thenReturn(false);
        when(experimentRepository.findById(53L)).thenReturn(Optional.of(experiment));
        when(executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(53L))
                .thenReturn(List.of(publication));
        when(leadPortalPublicUrlResolver.resolve(flow))
                .thenReturn("https://oportunidadebrasil.shop/flows/product-ai-exp-55-personalized-sample");
        when(publicationRepository.save(any(GeraSalesPagePublicationAudit.class)))
                .thenAnswer(invocation -> {
                    GeraSalesPagePublicationAudit audit = invocation.getArgument(0);
                    audit.setId(11L);
                    return audit;
                });

        service.snapshotPublication(publication);

        ArgumentCaptor<GeraSalesPagePublicationAudit> audit =
                ArgumentCaptor.forClass(GeraSalesPagePublicationAudit.class);
        verify(publicationRepository).save(audit.capture());
        verify(leadPortalFlowPublisher).publish(flow);
        assertThat(experiment.getFollowUpActionUrl())
                .isEqualTo("https://oportunidadebrasil.shop/flows/product-ai-exp-55-personalized-sample");
        assertThat(audit.getValue().getSalesPageUrl())
                .isEqualTo("https://oportunidadebrasil.shop/flows/product-ai-exp-55-personalized-sample");
        assertThat(audit.getValue().getCheckoutUrl()).isNull();
        assertThat(audit.getValue().getHtml()).contains("lead-portal-personalized-sample-form");

        Map<?, ?> customPayload = objectMapper.readValue(flow.getCustomFormHtml(), Map.class);
        assertThat(customPayload.get("htmlDocument").toString()).contains("Venda Produto IA");
        assertThat(customPayload.get("htmlDocument").toString()).doesNotContain("<iframe");
        assertThat(audit.getValue().getHtml()).doesNotContain("<iframe");
        Map<?, ?> formSpec = (Map<?, ?>) customPayload.get("formSpec");
        assertThat(formSpec.get("formId")).isEqualTo("lead-portal-personalized-sample-form");
        assertThat((List<?>) formSpec.get("fields")).hasSize(2);
    }

    /** Deve bloquear HTML publico com termos comerciais comuns em portugues sem acentuacao. */
    @Test
    void shouldBlockPublicationWithCommonUnaccentedPortugueseTerms() {
        Experiment experiment = new Experiment();
        experiment.setId(53L);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/checkout");
        GeraSalesPageStageExecution publication = execution(
                "job-publication",
                GeraSalesPageStageCode.PUBLICATION_PACKAGE.code(),
                "{\"html\":\"<html><body><h1>Voce nao precisa perder o almoco</h1>"
                        + "<p>Receba informacao no proximo endereco.</p></body></html>\","
                        + "\"checkoutUrl\":\"https://mp.test/checkout\"}",
                Instant.parse("2026-07-01T10:10:00Z"));

        when(publicationRepository.existsByPublicationJobId("job-publication")).thenReturn(false);
        when(experimentRepository.findById(53L)).thenReturn(Optional.of(experiment));
        when(executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(53L))
                .thenReturn(List.of(publication));

        assertThatThrownBy(() -> service.snapshotPublication(publication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("voce")
                .hasMessageContaining("nao")
                .hasMessageContaining("informacao")
                .hasMessageContaining("endereco");
    }

    /** Deve bloquear pagina que fala da oferta sem cenas visuais suficientes da transformacao. */
    @Test
    void shouldBlockPublicationWithoutEnoughTransformationVisualScenes() {
        Experiment experiment = new Experiment();
        experiment.setId(53L);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/checkout");
        GeraSalesPageStageExecution publication = execution(
                "job-publication",
                GeraSalesPageStageCode.PUBLICATION_PACKAGE.code(),
                "{\"html\":\"<html><body><main><h1>Venda com promessa forte</h1>"
                        + "<p>Texto correto, mas sem prova visual suficiente do depois.</p>"
                        + "<section data-transform-visual='after'><img src='https://cdn.test/depois.jpg' alt='Depois'></section>"
                        + "</main></body></html>\",\"checkoutUrl\":\"https://mp.test/checkout\"}",
                Instant.parse("2026-07-01T10:10:00Z"));

        when(publicationRepository.existsByPublicationJobId("job-publication")).thenReturn(false);
        when(experimentRepository.findById(53L)).thenReturn(Optional.of(experiment));
        when(executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(53L))
                .thenReturn(List.of(publication));

        assertThatThrownBy(() -> service.snapshotPublication(publication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("menos de 3 cenas visuais de transformacao");
    }

    /** Monta HTML de venda com cenas visuais suficientes para a publicacao. */
    private String salesPageHtml(String headline) {
        return "<html><body><main>"
                + "<header data-transform-visual='after'><h1>" + headline + "</h1>"
                + "<img src='https://cdn.test/depois-hero.jpg' alt='Depois da transformacao'></header>"
                + "<section data-transform-visual='pain'><h2>Antes</h2>"
                + "<img src='https://cdn.test/dor-atual.jpg' alt='Dor atual reconhecivel'></section>"
                + "<section data-transform-visual='preview'><h2>Preview</h2>"
                + "<img src='https://cdn.test/preview-produto.jpg' alt='Preview do produto'></section>"
                + "</main></body></html>";
    }

    /** Cria execução concluída mínima para snapshot de teste. */
    private GeraSalesPageStageExecution execution(String idJob, String stageCode, String response, Instant requestedAt) {
        return GeraSalesPageStageExecution.builder()
                .idJob(idJob)
                .experimentId(53L)
                .stageCode(stageCode)
                .status("CONCLUIDO")
                .executionRequestedAt(requestedAt)
                .completedAt(requestedAt.plusSeconds(5))
                .promptTemplateKey("template-" + stageCode)
                .prompt("prompt " + idJob)
                .promptMarkdownContent("# Prompt " + idJob)
                .schemaJson("{\"type\":\"object\"}")
                .openAiModel("gpt-test")
                .openAiRequestBody("{\"model\":\"gpt-test\"}")
                .modelResponse(response)
                .rawResponse("{\"raw\":true}")
                .inputTokens(10)
                .outputTokens(5)
                .build();
    }
}
