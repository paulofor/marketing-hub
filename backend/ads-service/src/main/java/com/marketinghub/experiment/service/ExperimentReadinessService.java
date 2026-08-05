package com.marketinghub.experiment.service;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.salespageab.service.ExperimentSalesPageAbTestService;
import com.marketinghub.experiment.video.service.ExperimentVideoAssetService;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.repository.jpa.experiment.salespagetype.ExperimentSalesPageTypeSelectionRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Consolida pendências básicas de preparação do experimento. */
@Service
public class ExperimentReadinessService {
  private final ExperimentService experimentService;
  private final CreativeRepository creativeRepository;
  private static final String STATUS_COMPLETED = "CONCLUIDO";
  private static final List<String> GERA_LANDING_REQUIRED_STAGES =
      List.of(
          "landing-page-wireframe",
          "landing-page-copy",
          "landing-page-image-planning",
          "landing-page-image-generation",
          "landing-page-design-preset",
          "landing-page-quality-review",
          "landing-page-deliverables");

  private final ExperimentTargetingSelectionRepository targetingSelectionRepository;
  private final ExperimentCampaignDestinationPolicy campaignDestinationPolicy;
  private static final List<TargetingElementType> PUBLISHABLE_TARGETING_TYPES =
      List.of(
          TargetingElementType.INTEREST,
          TargetingElementType.JOB_TITLE,
          TargetingElementType.BEHAVIOR);
  private static final Set<TargetingElementType> PUBLISHABLE_TARGETING_TYPE_SET =
      Set.copyOf(PUBLISHABLE_TARGETING_TYPES);
  private static final Set<String> PRODUCT_AI_PERSONALIZED_SAMPLE_REQUIRED_KEYS =
      Set.of("email", "negocio_projeto", "contexto_atual", "objetivo_visual", "dados_personalizacao");

  private final GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;
  private final ExperimentVideoAssetService experimentVideoAssetService;
  private final ExperimentSalesPageAbTestService salesPageAbTestService;
  private final ExperimentSalesPageTypeSelectionRepository salesPageTypeSelectionRepository;

  /** Cria o serviço com as fontes canônicas de prontidão do experimento. */
  public ExperimentReadinessService(
      ExperimentService experimentService,
      CreativeRepository creativeRepository,
      ExperimentTargetingSelectionRepository targetingSelectionRepository,
      GeraLandingStageExecutionRepository geraLandingStageExecutionRepository,
      ExperimentCampaignDestinationPolicy campaignDestinationPolicy,
      ExperimentVideoAssetService experimentVideoAssetService,
      ExperimentSalesPageAbTestService salesPageAbTestService,
      ExperimentSalesPageTypeSelectionRepository salesPageTypeSelectionRepository) {
    this.experimentService = experimentService;
    this.creativeRepository = creativeRepository;
    this.targetingSelectionRepository = targetingSelectionRepository;
    this.geraLandingStageExecutionRepository = geraLandingStageExecutionRepository;
    this.campaignDestinationPolicy = campaignDestinationPolicy;
    this.experimentVideoAssetService = experimentVideoAssetService;
    this.salesPageAbTestService = salesPageAbTestService;
    this.salesPageTypeSelectionRepository = salesPageTypeSelectionRepository;
  }

  /** Resume a prontidão do experimento usando apenas dados canônicos aprovados para publicação. */
  @Transactional(readOnly = true)
  public ExperimentReadinessSummaryDto summarize(Long experimentId) {
    Experiment experiment = experimentService.get(experimentId);
    long creativeCount =
        creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY);
    boolean hasCreatives = creativeCount > 0;

    long leadPortalFlowCount = hasReadyLeadPortalFlow(experiment) ? 1L : 0L;
    boolean hasLeadPortalFlow = leadPortalFlowCount > 0;

    boolean hasCompleteTargeting = hasConfiguredTargeting(experiment);
    List<TargetingElementType> missingTypes =
        hasCompleteTargeting ? List.of() : PUBLISHABLE_TARGETING_TYPES;
    long geraLandingCompletedStageCount = countCompletedGeraLandingStages(experimentId);
    boolean hasGeraLandingPipeline =
        geraLandingCompletedStageCount == GERA_LANDING_REQUIRED_STAGES.size();
    boolean purchaseIntent = campaignDestinationPolicy.requiresSalesPageBeforePurchase(experiment);
    boolean pdeMembershipFunnel =
        campaignDestinationPolicy.isPdeMembershipSubscriptionFunnel(experiment);
    boolean hasCommercialContract =
        campaignDestinationPolicy.hasCompleteCommercialContract(experiment);
    boolean hasPdeMembershipDestination =
        campaignDestinationPolicy.hasPdeMembershipDestination(experiment);
    boolean hasGeraSalesPagePipeline =
        campaignDestinationPolicy.hasCompletedGeraSalesPagePipeline(experimentId);
    boolean hasRequiredVideoBlockingRelease = hasRequiredVideoBlockingRelease(experiment);
    boolean requiresSalesPageAbTest =
        campaignDestinationPolicy.requiresSalesPageBeforePurchase(experiment);
    boolean hasReadySalesPageAbTest =
        !requiresSalesPageAbTest || salesPageAbTestService.hasReadyActiveTest(experimentId);
    Optional<GeraSalesPagePublicationAudit> salesPagePublication =
        campaignDestinationPolicy.latestSalesPagePublication(experimentId);

    List<ExperimentReadinessIssueDto> issues = new ArrayList<>();
    if (!hasCreatives) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.CREATIVE,
              "Nenhum criativo aprovado",
              "Este experimento ainda não possui criativos aprovados para publicação.",
              "Use a aba Criativos para gerar novas peças por IA ou cadastre um criativo manualmente.",
              List.of()));
    }
    if (!purchaseIntent && !pdeMembershipFunnel && !hasLeadPortalFlow) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.LEAD_PORTAL_FLOW,
              "Sem fluxo do portal do lead",
              "Ainda não há um fluxo do portal vinculado a este experimento.",
              "Solicite a geração de um fluxo ou associe um existente na aba Portal do Lead.",
              List.of()));
    }
    if (isPersonalizedSampleProductAi(experiment)
        && !hasProductAiPersonalizationFunnel(experiment)) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.PRODUCT_AI_FUNNEL,
              "Sem funil de coleta para personalização",
              "Produto IA com amostra personalizada precisa coletar dados do lead antes de prometer uma entrega exclusiva.",
              "Crie o funil pelo comando de Produto IA antes de liberar campanha ou venda.",
              List.of()));
    }
    if (hasRequiredVideoBlockingRelease) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.VIDEO_ASSET,
              "Vídeo obrigatório ainda não aprovado",
              "Este experimento possui vídeo obrigatório para o funil, mas o ativo ainda não está pronto e aprovado.",
              "Finalize a geração, revise o vídeo e aprove o ativo antes de liberar tráfego.",
              List.of()));
    }
    if (requiresSalesPageAbTest && !hasReadySalesPageAbTest) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.SALES_PAGE_AB_TEST,
              "Teste A/B de página incompleto",
              "Existe um teste A/B ativo para a página de venda, mas as duas variantes ainda não estão prontas para tráfego.",
              "Configure página, checkout, destino do anúncio, coletores e divisão de tráfego das duas variantes antes de liberar a campanha.",
              List.of()));
    }
    if (!hasCompleteTargeting) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.TARGETING,
              "Público não selecionado",
              "Ainda não há nenhum interesse, cargo ou comportamento publicável salvo para este experimento.",
              "Acesse a aba Segmentação, marque ao menos um interesse, cargo ou comportamento com ID oficial da Meta e salve o público.",
              List.copyOf(missingTypes)));
    }

    if (purchaseIntent && !hasCommercialContract) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.GERA_SALES_PAGE,
              "Contrato comercial incompleto",
              "Experimentos com intenção de compra precisam da etapa Oferta preenchida antes de página, criativos e campanha.",
              "Complete dor única, prova/preview, promessa, CTA e preço para o sistema gerar a página de venda como fonte soberana.",
              List.of()));
    }
    if (purchaseIntent
        && hasCommercialContract
        && (!hasGeraSalesPagePipeline || salesPagePublication.isEmpty())) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.GERA_SALES_PAGE,
              "Página de venda não foi criada pelo pipeline",
              "Experimentos com intenção de compra só podem ser liberados quando o GeraSalesPage v1 concluir e auditar a publicação da página de venda.",
              "Execute ou refaça o GeraSalesPage v1 e use a página de venda gerada pelo pipeline.",
              List.of()));
    }
    if (purchaseIntent
        && hasCommercialContract
        && salesPagePublication.isPresent()
        && !campaignDestinationPolicy.hasAdDestinationPointingToSalesPage(
            experiment, salesPagePublication.get())) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.GERA_SALES_PAGE,
              "Link do anúncio não aponta para a página de venda",
              "O destino atual da campanha precisa ser a página de venda auditada pelo GeraSalesPage, não o checkout direto nem outra URL.",
              "Atualize a URL do anúncio para a página de venda publicada e mantenha o checkout apenas nos CTAs da página.",
              List.of()));
    }
    if (purchaseIntent
        && hasCommercialContract
        && salesPagePublication.isPresent()
        && !campaignDestinationPolicy.hasRequiredSalesPageAnalyticsCollectors(
            salesPagePublication.get())) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.GERA_SALES_PAGE,
              "Página de venda sem coletores de métricas",
              "A página publicada não possui todos os coletores obrigatórios: page_view, page_load_metric, section_view_time e checkout_click.",
              "Republique a página pelo GeraSalesPage v1 atualizado antes de liberar tráfego pago.",
              List.of()));
    }
    if (pdeMembershipFunnel && !hasCommercialContract) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.GERA_SALES_PAGE,
              "Contrato comercial incompleto",
              "O funil PDE MUSA precisa da oferta preenchida antes de liberar tráfego para login, experiência gratuita e paywall interno.",
              "Complete dor única, prova/preview, promessa, CTA e preço do Clube MUSA.",
              List.of()));
    }
    if (pdeMembershipFunnel && hasCommercialContract && !hasPdeMembershipDestination) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.GERA_SALES_PAGE,
              "Destino PDE MUSA inválido",
              "O anúncio do funil PDE MUSA deve apontar para a entrada do Clube MUSA, onde a lead faz login, experimenta a parte gratuita e encontra o paywall interno.",
              "Atualize a URL do anúncio para um slot produtivo versionado aprovado, como https://v5.clubemusa.com.br.",
              List.of()));
    }

    if (!purchaseIntent && !pdeMembershipFunnel && !hasGeraLandingPipeline) {
      issues.add(
          new ExperimentReadinessIssueDto(
              ExperimentReadinessIssueType.GERA_LANDING,
              "GeraLanding incompleto",
              "O fluxo do GeraLanding ainda não concluiu todas as etapas obrigatórias.",
              "Abra a aba Gera landing, corrija etapas com falha e execute as pendentes até a finalização.",
              List.of()));
    }

    return new ExperimentReadinessSummaryDto(
        hasCreatives,
        creativeCount,
        hasLeadPortalFlow,
        leadPortalFlowCount,
        hasCompleteTargeting,
        hasGeraLandingPipeline,
        geraLandingCompletedStageCount,
        GERA_LANDING_REQUIRED_STAGES.size(),
        List.copyOf(missingTypes),
        List.copyOf(issues));
  }

  /** Verifica se o experimento já pode entrar na fila de campanha paga. */
  public boolean isReadyForCampaign(Experiment experiment) {
    return computeMissingConfiguration(experiment).isEmpty();
  }

  /** Lista as configurações faltantes que bloqueiam publicação de campanha. */
  public List<String> computeMissingConfiguration(Experiment experiment) {
    List<String> missing = new ArrayList<>();
    if (!hasApprovedCreative(experiment)) {
      missing.add("creativeApproval");
    }
    if (!hasApprovedLandingDestination(experiment)) {
      missing.add("landingDestination");
    }
    missing.addAll(campaignDestinationPolicy.missingConfiguration(experiment));
    if (isLowTicketProduct(experiment) && !hasFacebookPixel(experiment)) {
      missing.add("facebookPixel");
    }
    if (isPersonalizedSampleProductAi(experiment)
        && !hasProductAiPersonalizationFunnel(experiment)) {
      missing.add("productAiPersonalizedSampleFunnel");
    }
    if (hasRequiredVideoBlockingRelease(experiment)) {
      missing.add("experimentVideoAsset");
    }
    if (campaignDestinationPolicy.requiresSalesPageBeforePurchase(experiment)
        && !salesPageAbTestService.hasReadyActiveTest(experiment.getId())) {
      missing.add("salesPageAbTest");
    }
    if (!hasConfiguredTargeting(experiment)) {
      missing.add("approvedTargetingPackage");
    }
    return List.copyOf(missing);
  }

  /** Conta as etapas obrigatórias do GeraLanding cuja execução mais recente foi concluída. */
  private long countCompletedGeraLandingStages(Long experimentId) {
    return GERA_LANDING_REQUIRED_STAGES.stream()
        .filter(
            stageCode ->
                geraLandingStageExecutionRepository
                    .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                        experimentId, stageCode)
                    .map(GeraLandingStageExecution::getStatus)
                    .map(STATUS_COMPLETED::equalsIgnoreCase)
                    .orElse(false))
        .count();
  }

  /** Verifica se o experimento possui segmentação escolhida e salva pelo usuário. */
  private boolean hasConfiguredTargeting(Experiment experiment) {
    if (experiment == null) {
      return false;
    }
    return hasSelectedTargeting(experiment.getId());
  }

  /**
   * Verifica a aprovação real dos criativos pela fonte canônica: registros READY com imagem
   * publicável.
   */
  private boolean hasApprovedCreative(Experiment experiment) {
    if (experiment == null || experiment.getId() == null) {
      return false;
    }
    if (!creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
        experiment.getId(), CreativeStatus.READY)) {
      return false;
    }
    List<Creative> readyCreatives =
        creativeRepository.findByExperimentId(experiment.getId()).stream()
            .filter(creative -> creative.getStatus() == CreativeStatus.READY)
            .toList();
    if (readyCreatives.isEmpty()) {
      return true;
    }
    return readyCreatives.stream()
        .anyMatch(creative -> passesPublicationGate(experiment, creative));
  }

  /** Valida o gate mínimo de publicação Meta para mídia e copy do criativo. */
  private boolean passesPublicationGate(Experiment experiment, Creative creative) {
    return hasPublishableMedia(experiment, creative)
        && hasCommerciallyAlignedCopy(experiment, creative);
  }

  /** Verifica se a mídia do criativo pode ser publicada sem pular a aprovação técnica de vídeo. */
  private boolean hasPublishableMedia(Experiment experiment, Creative creative) {
    if (creative == null) {
      return false;
    }
    if (isVideoCreative(creative)) {
      return experimentVideoAssetService.hasReadyApprovedAudibleVideoForPublication(
          experiment.getId(), creative.getVideoId(), creative.getVideoUrl());
    }
    return StringUtils.hasText(creative.getImageUrl());
  }

  /**
   * Confirma que a copy possui mensagem, headline, CTA e conexão mínima com o contrato comercial.
   */
  private boolean hasCommerciallyAlignedCopy(Experiment experiment, Creative creative) {
    if (creative == null
        || !StringUtils.hasText(creative.getPrimaryText())
        || !StringUtils.hasText(creative.getHeadline())
        || !StringUtils.hasText(creative.getCta())) {
      return false;
    }
    List<String> anchors = commercialCopyAnchors(experiment);
    if (anchors.isEmpty()) {
      return true;
    }
    String copy =
        normalizeCommercialText(
            String.join(
                " ",
                creative.getPrimaryText(),
                creative.getHeadline(),
                creative.getDescription() == null ? "" : creative.getDescription(),
                creative.getCta()));
    return anchors.stream().anyMatch(anchor -> copy.contains(anchor));
  }

  /**
   * Extrai termos comerciais fortes de dor, promessa, recompensa e CTA para checagem
   * determinística.
   */
  private List<String> commercialCopyAnchors(Experiment experiment) {
    if (experiment == null) {
      return List.of();
    }
    return Stream.of(
            experiment.getSinglePain(),
            experiment.getFunnelPromise(),
            experiment.getFreeReward(),
            experiment.getPrimaryCta())
        .flatMap(value -> significantTerms(value).stream())
        .distinct()
        .toList();
  }

  /**
   * Extrai termos suficientemente específicos para evitar aprovação de copy genérica ou
   * desalinhada.
   */
  private List<String> significantTerms(String value) {
    String normalized = normalizeCommercialText(value);
    if (!StringUtils.hasText(normalized)) {
      return List.of();
    }
    return List.of(normalized.split(" ")).stream()
        .filter(term -> term.length() >= 5)
        .filter(term -> !isCommonCommercialTerm(term))
        .limit(8)
        .toList();
  }

  /** Remove termos genéricos que não provam aderência real da copy ao experimento. */
  private boolean isCommonCommercialTerm(String term) {
    return Set.of(
            "receber", "baixar", "acessar", "conheca", "gratis", "agora", "metodo", "produto",
            "digital", "clique")
        .contains(term);
  }

  /** Normaliza texto comercial para comparação simples e estável. */
  private String normalizeCommercialText(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", " ")
        .trim();
  }

  /** Identifica criativos de vídeo que exigem áudio perceptível e revisão humana aprovados. */
  private boolean isVideoCreative(Creative creative) {
    return creative != null
        && StringUtils.hasText(creative.getFormat())
        && "VIDEO".equalsIgnoreCase(creative.getFormat().trim());
  }

  /** Verifica se existe fluxo aprovado do portal do lead vinculado ao experimento. */
  private boolean hasReadyLeadPortalFlow(Experiment experiment) {
    return experiment != null
        && experiment.getLeadPortalFlow() != null
        && experiment.getLeadPortalFlow().isApproved();
  }

  /**
   * Verifica se o usuário selecionou qualquer público publicável para segmentação do experimento.
   */
  private boolean hasSelectedTargeting(Long experimentId) {
    if (experimentId == null) {
      return false;
    }
    return targetingSelectionRepository
        .findByExperimentIdWithTargetingElement(experimentId)
        .stream()
        .map(com.marketinghub.experiment.ExperimentTargetingSelection::getTargetingElement)
        .anyMatch(this::isPublishableTargetingElement);
  }

  /**
   * Confirma que o item de público escolhido está aprovado e possui identificador oficial da Meta.
   */
  private boolean isPublishableTargetingElement(TargetingElement element) {
    return element != null
        && element.getStatus() == TargetingElementStatus.APPROVED
        && element.getType() != null
        && PUBLISHABLE_TARGETING_TYPE_SET.contains(element.getType())
        && element.getMetaId() != null
        && !element.getMetaId().isBlank();
  }

  /** Verifica se o experimento possui URL de destino aprovada para campanha. */
  private boolean hasApprovedLandingDestination(Experiment experiment) {
    return experiment != null
        && experiment.getFollowUpActionUrl() != null
        && !experiment.getFollowUpActionUrl().isBlank();
  }

  /** Verifica se o nicho do experimento já possui pixel da Meta para otimização de compra. */
  private boolean hasFacebookPixel(Experiment experiment) {
    return experiment != null
        && experiment.getNiche() != null
        && experiment.getNiche().getFacebookPixelId() != null
        && !experiment.getNiche().getFacebookPixelId().isBlank();
  }

  /** Identifica experimento de venda direta low-ticket. */
  private boolean isLowTicketProduct(Experiment experiment) {
    return experiment != null
        && experiment.getExperimentType() == ExperimentType.LOW_TICKET_PRODUCT;
  }

  /** Identifica Produto IA que depende de dados do lead para gerar amostra personalizada. */
  private boolean isPersonalizedSampleProductAi(Experiment experiment) {
    return experiment != null
        && experiment.getProductAiSubtype() == ProductAiSubtype.AI_PERSONALIZED_SAMPLE;
  }

  /** Verifica se o funil de personalização está aprovado e possui os campos mínimos de coleta. */
  private boolean hasProductAiPersonalizationFunnel(Experiment experiment) {
    if (experiment == null
        || experiment.getLeadPortalFlow() == null
        || !experiment.getLeadPortalFlow().isApproved()) {
      return false;
    }
    Set<String> keys =
        experiment.getLeadPortalFlow().getQuestions().stream()
            .map(
                question ->
                    question.getDataKey() == null
                        ? ""
                        : question.getDataKey().toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toSet());
    return keys.containsAll(PRODUCT_AI_PERSONALIZED_SAMPLE_REQUIRED_KEYS);
  }

  /** Verifica se algum vídeo obrigatório do experimento ainda impede liberação comercial. */
  private boolean hasRequiredVideoBlockingRelease(Experiment experiment) {
    if (experiment == null || experiment.getId() == null) {
      return false;
    }
    Long experimentId = experiment.getId();
    if (experimentVideoAssetService.hasRequiredVideoBlockingRelease(experimentId)) {
      return true;
    }
    return hasVideoDependentSalesPage(experimentId)
        && !experimentVideoAssetService.hasReadyApprovedVideo(experimentId);
  }

  /** Verifica se o desenho comercial escolhido depende de página com vídeo humano. */
  private boolean hasVideoDependentSalesPage(Long experimentId) {
    return salesPageAbTestService.hasHumanVideoVariant(experimentId)
        || salesPageTypeSelectionRepository.existsByExperimentIdAndSalesPageTypeCodeAndActiveTrue(
            experimentId, "HUMAN_VIDEO_SALES_PAGE");
  }
}
