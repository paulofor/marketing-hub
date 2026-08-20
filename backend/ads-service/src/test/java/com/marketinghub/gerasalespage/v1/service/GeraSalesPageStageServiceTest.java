package com.marketinghub.gerasalespage.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.aiprompt.AiPromptSchemaTemplate;
import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentAiPromptSchemaUsageService;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.aiprompt.AiPromptSchemaTemplateRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

  @Mock private ExperimentRepository experimentRepository;
  @Mock private GeraSalesPageStageExecutionRepository executionRepository;
  @Mock private AiPromptSchemaTemplateRepository templateRepository;
  @Mock private DeliverablePackageRepository deliverablePackageRepository;
  @Mock private GeraSalesPagePublicationAuditService publicationAuditService;
  @Mock private ExperimentAiPromptSchemaUsageService promptSchemaUsageService;
  @Mock private CommercialPlanLandingAssetService landingAssetService;

  private ObjectMapper objectMapper = new ObjectMapper();

  private GeraSalesPageStageService service;

  /** Inicializa service com ObjectMapper real para validar parsing do quality review. */
  @BeforeEach
  void setUp() {
    service =
        new GeraSalesPageStageService(
            experimentRepository,
            executionRepository,
            templateRepository,
            deliverablePackageRepository,
            publicationAuditService,
            promptSchemaUsageService,
            landingAssetService,
            objectMapper);
  }

  /** Deve substituir execuções antigas e criar uma primeira etapa nova. */
  @Test
  void rebuildShouldReplacePreviousExecutionsAndStartFreshPipeline() {
    Experiment experiment = new Experiment();
    experiment.setId(53L);
    experiment.setFollowUpActionUrl(
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc");
    completeCommercialContract(experiment);
    GeraSalesPageStageExecution previous =
        GeraSalesPageStageExecution.builder()
            .idJob("old-job")
            .experimentId(53L)
            .stageCode(GeraSalesPageStageCode.PUBLICATION_PACKAGE.code())
            .status("CONCLUIDO")
            .executionRequestedAt(Instant.now().minusSeconds(60))
            .build();

    when(experimentRepository.findById(53L)).thenReturn(Optional.of(experiment));
    when(executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(53L))
        .thenReturn(List.of(previous));
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
    assertThat(newExecution.getValue().getStageCode())
        .isEqualTo(GeraSalesPageStageCode.OFFER_BRIEF.code());
    assertThat(newExecution.getValue().getStatus()).isEqualTo("INICIADO");
    verify(promptSchemaUsageService)
        .linkSalesPageTemplate(
            53L,
            activeTemplate,
            GeraSalesPageStageCode.OFFER_BRIEF.code(),
            newExecution.getValue().getIdJob());
    assertThat(response.stageCode()).isEqualTo(GeraSalesPageStageCode.OFFER_BRIEF.code());
  }

  /** Deve permitir Produto IA personalizado quando o destino é o funil aprovado de amostra. */
  @Test
  void startShouldAllowPersonalizedSampleFunnelWithoutDirectCheckout() {
    Experiment experiment = new Experiment();
    experiment.setId(57L);
    experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
    experiment.setFollowUpActionUrl(
        "https://oportunidadebrasil.shop/flows/decoraia-express-exp-57");
    experiment.setLeadPortalFlow(
        LeadPortalFlow.builder().id(39L).slug("decoraia-express-exp-57").approved(true).build());
    completeCommercialContract(experiment);

    when(experimentRepository.findById(57L)).thenReturn(Optional.of(experiment));
    AiPromptSchemaTemplate activeTemplate = template(GeraSalesPageStageCode.OFFER_BRIEF.code());
    when(templateRepository.findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(
            "gera-sales-page-v1", GeraSalesPageStageCode.OFFER_BRIEF.code()))
        .thenReturn(Optional.of(activeTemplate));
    when(executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
            57L, GeraSalesPageStageCode.OFFER_BRIEF.code()))
        .thenReturn(Optional.empty());
    when(executionRepository.save(any(GeraSalesPageStageExecution.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GeraSalesPageStartResponse response = service.start(57L);

    assertThat(response.stageCode()).isEqualTo(GeraSalesPageStageCode.OFFER_BRIEF.code());
    verify(promptSchemaUsageService)
        .linkSalesPageTemplate(
            57L, activeTemplate, GeraSalesPageStageCode.OFFER_BRIEF.code(), response.jobid());
  }

  /** Deve continuar bloqueando venda direta sem checkout real. */
  @Test
  void startShouldRejectDirectSalesPageWithoutCheckout() {
    Experiment experiment = new Experiment();
    experiment.setId(58L);
    experiment.setFollowUpActionUrl("#checkout");
    completeCommercialContract(experiment);
    when(experimentRepository.findById(58L)).thenReturn(Optional.of(experiment));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.start(58L))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("checkout");
  }

  /**
   * Deve incluir o pacote FEO real no pending para a página materializar os entregáveis vendidos.
   */
  @Test
  void pendingShouldIncludeLatestFeoDeliverablePackage() {
    Experiment experiment = new Experiment();
    experiment.setId(66L);
    experiment.setName("MUSA-H001-E004");
    experiment.setFollowUpActionUrl(
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=musa");
    completeCommercialContract(experiment);
    GeraSalesPageStageExecution execution =
        GeraSalesPageStageExecution.builder()
            .idJob("job-musa")
            .experimentId(66L)
            .stageCode(GeraSalesPageStageCode.OFFER_BRIEF.code())
            .status("INICIADO")
            .executionRequestedAt(Instant.now())
            .build();
    execution.setExperiment(experiment);
    Deliverable first =
        Deliverable.builder()
            .id(33L)
            .title("MDS/MUSA - Guia de assinatura olfativa acessivel por ocasiao")
            .description(
                "Plano de execucao - Tabela de 7 dias com ação, tempo estimado, evidência e"
                    + " ajuste.")
            .build();
    Deliverable second =
        Deliverable.builder()
            .id(34L)
            .title("entregaveis/kit-03-checklist-de-aplicacao-sem-travar.html")
            .description(
                "Checklist - Checklist marcável para executar sem esquecer pontos críticos.")
            .build();
    DeliverablePackage deliverablePackage =
        DeliverablePackage.builder()
            .id(7L)
            .name(
                "Pacote Final - Metodo MUSA - Arquitetura de Presenca Elegante Acessivel - FEO #3")
            .description("Kit operacional de transformação aplicável.")
            .deliverables(new LinkedHashSet<>(List.of(second, first)))
            .createdAt(Instant.parse("2026-07-15T00:54:38Z"))
            .build();

    when(templateRepository.findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(
            "gera-sales-page-v1", GeraSalesPageStageCode.OFFER_BRIEF.code()))
        .thenReturn(Optional.of(template(GeraSalesPageStageCode.OFFER_BRIEF.code())));
    when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            GeraSalesPageStageCode.OFFER_BRIEF.code(), "INICIADO"))
        .thenReturn(List.of(execution));
    when(deliverablePackageRepository.findByExperimentIdOrderByCreatedAtDesc(66L))
        .thenReturn(List.of(deliverablePackage));

    List<GeraSalesPagePendingResponse> pending =
        service.pending(GeraSalesPageStageCode.OFFER_BRIEF.code());

    Map<String, Object> packagePayload =
        castMap(pending.get(0).experiment().get("feoDeliverablePackage"));
    assertThat(packagePayload.get("id")).isEqualTo(7L);
    assertThat(packagePayload.get("name")).isEqualTo(deliverablePackage.getName());
    List<Map<String, Object>> deliverables = castList(packagePayload.get("deliverables"));
    assertThat(deliverables).hasSize(2);
    assertThat(deliverables.get(0).get("title")).isEqualTo(first.getTitle());
    assertThat(deliverables.get(0).get("publicTitle"))
        .isEqualTo("Guia de assinatura olfativa acessivel por ocasiao");
    assertThat(deliverables.get(1).get("title")).isEqualTo(second.getTitle());
    assertThat(deliverables.get(1).get("publicTitle"))
        .isEqualTo("checklist de aplicacao sem travar");
  }

  /** Deve bloquear o início quando a etapa Oferta ainda não preencheu contrato comercial. */
  @Test
  void rebuildShouldRejectExperimentWithoutCommercialContract() {
    Experiment experiment = new Experiment();
    experiment.setId(56L);
    experiment.setFollowUpActionUrl(
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc");
    when(experimentRepository.findById(56L)).thenReturn(Optional.of(experiment));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.rebuild(56L))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("contrato comercial completo");
  }

  /** Deve bloquear a publicação quando a revisão comercial reprova a página. */
  @Test
  void receiveResultShouldFailQualityReviewWhenNotApproved() {
    GeraSalesPageStageExecution execution =
        GeraSalesPageStageExecution.builder()
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
            "{\"approved\":false,\"blockers\":[\"amostra e produto pago estão"
                + " confusos\"],\"recommendation\":\"refazer\"}",
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
    verify(executionRepository, never())
        .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
            55L, GeraSalesPageStageCode.PUBLICATION_PACKAGE.code());
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

  /** Preenche o contrato comercial mínimo exigido antes da página de vendas. */
  private void completeCommercialContract(Experiment experiment) {
    experiment.setSinglePain("Cliente some depois da manutenção");
    experiment.setFreeReward("Preview visual da agenda preenchida");
    experiment.setFunnelPromise("Enxergar riscos e encaixes em 7 dias");
    experiment.setPrimaryCta("Comprar o Mapa 7D");
    experiment.setUnitPrice(BigDecimal.valueOf(29.90));
  }

  /** Converte payload genérico para mapa tipado usado nas asserções do teste. */
  @SuppressWarnings("unchecked")
  private Map<String, Object> castMap(Object value) {
    return (Map<String, Object>) value;
  }

  /** Converte payload genérico para lista tipada usada nas asserções do teste. */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> castList(Object value) {
    return (List<Map<String, Object>>) value;
  }
}
