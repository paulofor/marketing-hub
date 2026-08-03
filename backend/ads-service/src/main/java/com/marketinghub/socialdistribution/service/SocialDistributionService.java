package com.marketinghub.socialdistribution.service;

import com.marketinghub.media.Asset;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.socialdistribution.SocialAccountRepository;
import com.marketinghub.repository.jpa.socialdistribution.SocialGrowthContentRepository;
import com.marketinghub.repository.jpa.socialdistribution.SocialGrowthPlanRepository;
import com.marketinghub.repository.jpa.socialdistribution.SocialPublicationMetricRepository;
import com.marketinghub.repository.jpa.socialdistribution.SocialVideoPublicationRepository;
import com.marketinghub.socialdistribution.*;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerenciar contas, fila e métricas de distribuição orgânica de vídeos. */
@Service
public class SocialDistributionService {
  private final SocialAccountRepository accountRepository;
  private final SocialVideoPublicationRepository publicationRepository;
  private final SocialPublicationMetricRepository metricRepository;
  private final SocialGrowthPlanRepository growthPlanRepository;
  private final SocialGrowthContentRepository growthContentRepository;
  private final ProductRepository productRepository;
  private final AssetRepository assetRepository;

  /** Inicializa o serviço com repositórios do módulo e vínculos de produto/asset. */
  public SocialDistributionService(
      SocialAccountRepository accountRepository,
      SocialVideoPublicationRepository publicationRepository,
      SocialPublicationMetricRepository metricRepository,
      SocialGrowthPlanRepository growthPlanRepository,
      SocialGrowthContentRepository growthContentRepository,
      ProductRepository productRepository,
      AssetRepository assetRepository) {
    this.accountRepository = accountRepository;
    this.publicationRepository = publicationRepository;
    this.metricRepository = metricRepository;
    this.growthPlanRepository = growthPlanRepository;
    this.growthContentRepository = growthContentRepository;
    this.productRepository = productRepository;
    this.assetRepository = assetRepository;
  }

  /** Lista contas sociais, filtrando por rede quando informado. */
  @Transactional(readOnly = true)
  public List<SocialAccountResponse> listAccounts(SocialPlatform platform) {
    List<SocialAccount> accounts =
        platform == null
            ? accountRepository.findAll()
            : accountRepository.findByPlatformOrderByCreatedAtDesc(platform);
    return accounts.stream().map(this::toAccountResponse).toList();
  }

  /** Cadastra uma conta social com pendências e escopos da rede. */
  @Transactional
  public SocialAccountResponse createAccount(SaveSocialAccountRequest request) {
    SocialPlatform platform = requirePlatform(request.platform());
    SocialAccount account = new SocialAccount();
    account.setPlatform(platform);
    account.setDisplayName(normalizeRequired(request.displayName(), "Informe o nome da conta."));
    account.setHandle(normalizeOptional(request.handle()));
    account.setExternalAccountId(normalizeOptional(request.externalAccountId()));
    account.setConnectionMode(
        Optional.ofNullable(request.connectionMode()).orElse(SocialConnectionMode.OAUTH));
    account.setStatus(
        Optional.ofNullable(request.status()).orElse(SocialAccountStatus.SETUP_REQUIRED));
    account.setRequiredScopes(requiredScopesFor(platform));
    account.setSetupNotes(defaultSetupNotes(platform, request.setupNotes()));
    if (account.getStatus() == SocialAccountStatus.CONNECTED) {
      account.setConnectedAt(Instant.now());
    }
    return toAccountResponse(accountRepository.save(account));
  }

  /** Lista publicações orgânicas, filtrando por produto e rede quando informado. */
  @Transactional(readOnly = true)
  public List<SocialVideoPublicationResponse> listPublications(
      Long productId, SocialPlatform platform) {
    List<SocialVideoPublication> publications;
    if (productId != null && platform != null) {
      publications =
          publicationRepository.findTop100ByProductIdAndPlatformOrderByCreatedAtDesc(
              productId, platform);
    } else if (productId != null) {
      publications = publicationRepository.findTop100ByProductIdOrderByCreatedAtDesc(productId);
    } else if (platform != null) {
      publications = publicationRepository.findTop100ByPlatformOrderByCreatedAtDesc(platform);
    } else {
      publications = publicationRepository.findTop100ByOrderByCreatedAtDesc();
    }
    return publications.stream().map(this::toPublicationResponse).toList();
  }

  /** Cria uma publicação em rascunho para reaproveitar um vídeo em uma rede. */
  @Transactional
  public SocialVideoPublicationResponse createPublication(
      CreateSocialVideoPublicationRequest request) {
    SocialGrowthContent growthContent = resolveApprovedGrowthContent(request.growthContentId());
    Long requestedProductId =
        growthContent != null ? growthContent.getPlan().getProduct().getId() : request.productId();
    Product product =
        productRepository
            .findById(requireId(requestedProductId, "Informe o produto."))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
    Asset asset = resolveAsset(request.assetId());
    SocialPlatform platform = requirePlatform(request.platform());
    SocialVideoPublication publication = new SocialVideoPublication();
    publication.setProduct(product);
    publication.setAsset(asset);
    publication.setSocialAccount(resolveAccount(request.socialAccountId(), platform));
    publication.setPlatform(platform);
    publication.setVideoFormat(
        Optional.ofNullable(request.videoFormat()).orElse(defaultFormat(platform)));
    publication.setStatus(SocialVideoPublicationStatus.DRAFT);
    publication.setTitle(
        normalizeRequired(
            growthContent != null && !StringUtils.hasText(request.title())
                ? growthContent.getTopic()
                : request.title(),
            "Informe o título do vídeo."));
    publication.setCaption(
        normalizeOptional(
            growthContent != null && !StringUtils.hasText(request.caption())
                ? growthContent.getCta() + " " + growthContent.getTrackingUrl()
                : request.caption()));
    publication.setHashtags(normalizeOptional(request.hashtags()));
    publication.setVideoUrl(normalizeOptional(request.videoUrl()));
    publication.setScheduledAt(request.scheduledAt());
    publication.setPublishPayloadJson(buildPublishPayload(publication));
    SocialVideoPublication saved = publicationRepository.save(publication);
    if (growthContent != null) {
      growthContent.setPublication(saved);
      growthContentRepository.save(growthContent);
    }
    return toPublicationResponse(saved);
  }

  /** Coloca uma publicação na fila quando a conta tem condição operacional de publicar. */
  @Transactional
  public SocialVideoPublicationResponse queuePublication(Long publicationId) {
    SocialVideoPublication publication = getPublication(publicationId);
    String blockReason = publicationBlockReason(publication);
    if (StringUtils.hasText(blockReason)) {
      publication.setStatus(SocialVideoPublicationStatus.BLOCKED);
      publication.setFailureReason(blockReason);
      return toPublicationResponse(publicationRepository.save(publication));
    }
    publication.setStatus(SocialVideoPublicationStatus.QUEUED);
    publication.setFailureReason(null);
    publication.setQueuedAt(Instant.now());
    publication.setPublishPayloadJson(buildPublishPayload(publication));
    return toPublicationResponse(publicationRepository.save(publication));
  }

  /** Lista publicações em fila para consumo futuro pelo executor oficial. */
  @Transactional(readOnly = true)
  public List<SocialVideoPublicationResponse> listQueuedPublications() {
    return publicationRepository
        .findTop50ByStatusOrderByQueuedAtAsc(SocialVideoPublicationStatus.QUEUED)
        .stream()
        .map(this::toPublicationResponse)
        .toList();
  }

  /** Marca uma publicação como publicada após confirmação da plataforma ou operação humana. */
  @Transactional
  public SocialVideoPublicationResponse markPublished(
      Long publicationId, MarkSocialVideoPublishedRequest request) {
    SocialVideoPublication publication = getPublication(publicationId);
    publication.setStatus(SocialVideoPublicationStatus.PUBLISHED);
    publication.setPublishedUrl(
        normalizeRequired(request.publishedUrl(), "Informe a URL publicada."));
    publication.setExternalPostId(normalizeOptional(request.externalPostId()));
    publication.setPublishedAt(Optional.ofNullable(request.publishedAt()).orElse(Instant.now()));
    publication.setFailureReason(null);
    SocialVideoPublication saved = publicationRepository.save(publication);
    growthContentRepository
        .findByPublicationId(publicationId)
        .ifPresent(
            content -> {
              content.setStatus(SocialGrowthContentStatus.PUBLISHED);
              growthContentRepository.save(content);
            });
    return toPublicationResponse(saved);
  }

  /** Marca uma publicação como em processamento pelo executor externo. */
  @Transactional
  public SocialVideoPublicationResponse markPublishing(Long publicationId) {
    SocialVideoPublication publication = getPublication(publicationId);
    publication.setStatus(SocialVideoPublicationStatus.PUBLISHING);
    publication.setFailureReason(null);
    return toPublicationResponse(publicationRepository.save(publication));
  }

  /** Marca uma publicação como falha preservando causa acionável para o operador. */
  @Transactional
  public SocialVideoPublicationResponse markFailed(
      Long publicationId, MarkSocialVideoFailedRequest request) {
    SocialVideoPublication publication = getPublication(publicationId);
    publication.setStatus(SocialVideoPublicationStatus.FAILED);
    publication.setFailureReason(
        normalizeFailureReason(request.errorCategory(), request.errorMessage()));
    return toPublicationResponse(publicationRepository.save(publication));
  }

  /** Registra a leitura de métricas posterior de uma publicação. */
  @Transactional
  public SocialPublicationMetricResponse recordMetric(
      Long publicationId, RecordSocialPublicationMetricRequest request) {
    SocialVideoPublication publication = getPublication(publicationId);
    SocialPublicationMetric metric = new SocialPublicationMetric();
    metric.setPublication(publication);
    metric.setViews(nonNegative(request.views()));
    metric.setEngagedViews(nonNegative(request.engagedViews()));
    metric.setAverageViewDurationSeconds(nonNegative(request.averageViewDurationSeconds()));
    metric.setRecurringViewers(nonNegative(request.recurringViewers()));
    metric.setSubscribersGained(nonNegative(request.subscribersGained()));
    metric.setLikes(nonNegative(request.likes()));
    metric.setComments(nonNegative(request.comments()));
    metric.setShares(nonNegative(request.shares()));
    metric.setClicks(nonNegative(request.clicks()));
    metric.setLandingSessions(nonNegative(request.landingSessions()));
    metric.setLeads(nonNegative(request.leads()));
    metric.setCheckoutsStarted(nonNegative(request.checkoutsStarted()));
    metric.setSalesApproved(nonNegative(request.salesApproved()));
    metric.setRevenue(nonNegative(request.revenue()));
    metric.setRawPayloadJson(normalizeOptional(request.rawPayloadJson()));
    metric.setCapturedAt(Optional.ofNullable(request.capturedAt()).orElse(Instant.now()));
    return toMetricResponse(metricRepository.save(metric));
  }

  /** Lista planos orgânicos com calendário e leitura comercial calculada no backend. */
  @Transactional(readOnly = true)
  public List<SocialGrowthPlanResponse> listGrowthPlans(Long productId) {
    List<SocialGrowthPlan> plans =
        productId == null
            ? growthPlanRepository.findTop50ByOrderByCreatedAtDesc()
            : growthPlanRepository.findTop50ByProductIdOrderByCreatedAtDesc(productId);
    return plans.stream().map(this::toGrowthPlanResponse).toList();
  }

  /** Cria um plano em rascunho sem autorizar qualquer publicação externa. */
  @Transactional
  public SocialGrowthPlanResponse createGrowthPlan(CreateSocialGrowthPlanRequest request) {
    Product product =
        productRepository
            .findById(requireId(request.productId(), "Informe o produto."))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
    if (request.startsOn() != null
        && request.endsOn() != null
        && request.endsOn().isBefore(request.startsOn())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A data final deve ser posterior à data inicial.");
    }
    SocialGrowthPlan plan = new SocialGrowthPlan();
    plan.setProduct(product);
    plan.setName(normalizeRequired(request.name(), "Informe o nome do plano."));
    plan.setAudience(normalizeRequired(request.audience(), "Informe o público do plano."));
    plan.setCommercialHypothesis(
        normalizeRequired(request.commercialHypothesis(), "Informe a hipótese comercial."));
    plan.setCommercialObjective(
        normalizeRequired(request.commercialObjective(), "Informe o objetivo comercial."));
    plan.setPrimaryCta(normalizeRequired(request.primaryCta(), "Informe o CTA principal."));
    plan.setDestinationUrl(
        normalizeRequired(request.destinationUrl(), "Informe a URL de destino."));
    plan.setUtmCampaign(normalizeTrackingValue(request.utmCampaign(), "Informe a campanha UTM."));
    plan.setStatus(SocialGrowthPlanStatus.DRAFT);
    plan.setStartsOn(request.startsOn());
    plan.setEndsOn(request.endsOn());
    return toGrowthPlanResponse(growthPlanRepository.save(plan));
  }

  /** Adiciona uma pauta em rascunho e gera sua URL rastreável no backend. */
  @Transactional
  public SocialGrowthContentResponse createGrowthContent(
      Long planId, CreateSocialGrowthContentRequest request) {
    SocialGrowthPlan plan = getGrowthPlan(planId);
    SocialGrowthContent content = new SocialGrowthContent();
    content.setPlan(plan);
    content.setContentType(
        Optional.ofNullable(request.contentType()).orElse(SocialGrowthContentType.SHORT));
    content.setPillar(normalizeRequired(request.pillar(), "Informe o pilar editorial."));
    content.setTopic(normalizeRequired(request.topic(), "Informe a pauta."));
    content.setFunnelStage(normalizeRequired(request.funnelStage(), "Informe a etapa do funil."));
    content.setCta(
        StringUtils.hasText(request.cta()) ? request.cta().trim() : plan.getPrimaryCta());
    content.setTrackingCode(
        normalizeTrackingValue(plan.getUtmCampaign() + "-" + UUID.randomUUID(), ""));
    content.setTrackingUrl(buildTrackingUrl(plan, content));
    content.setStatus(SocialGrowthContentStatus.DRAFT);
    content.setPlannedAt(request.plannedAt());
    return toGrowthContentResponse(growthContentRepository.save(content));
  }

  /** Registra aprovação humana da pauta sem colocá-la automaticamente na fila. */
  @Transactional
  public SocialGrowthContentResponse approveGrowthContent(Long planId, Long contentId) {
    SocialGrowthContent content = getGrowthContent(planId, contentId);
    content.setStatus(SocialGrowthContentStatus.APPROVED);
    return toGrowthContentResponse(growthContentRepository.save(content));
  }

  /** Retorna a publicação pelo identificador interno. */
  private SocialVideoPublication getPublication(Long publicationId) {
    return publicationRepository
        .findById(requireId(publicationId, "Informe a publicação."))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada."));
  }

  /** Resolve uma pauta opcional e exige aprovação humana antes de criar a publicação. */
  private SocialGrowthContent resolveApprovedGrowthContent(Long contentId) {
    if (contentId == null) {
      return null;
    }
    SocialGrowthContent content =
        growthContentRepository
            .findById(contentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada."));
    if (content.getStatus() != SocialGrowthContentStatus.APPROVED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A pauta precisa de aprovação humana antes da publicação.");
    }
    if (content.getPublication() != null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A pauta já possui uma publicação vinculada.");
    }
    return content;
  }

  /** Retorna um plano orgânico pelo identificador. */
  private SocialGrowthPlan getGrowthPlan(Long planId) {
    return growthPlanRepository
        .findById(requireId(planId, "Informe o plano."))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado."));
  }

  /** Retorna uma pauta e valida que ela pertence ao plano informado. */
  private SocialGrowthContent getGrowthContent(Long planId, Long contentId) {
    SocialGrowthContent content =
        growthContentRepository
            .findById(requireId(contentId, "Informe a pauta."))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada."));
    if (!content.getPlan().getId().equals(requireId(planId, "Informe o plano."))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A pauta não pertence ao plano informado.");
    }
    return content;
  }

  /** Resolve asset opcional de vídeo para publicação. */
  private Asset resolveAsset(Long assetId) {
    if (assetId == null) {
      return null;
    }
    return assetRepository
        .findById(assetId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset não encontrado."));
  }

  /** Resolve conta social opcional e valida a rede quando informada. */
  private SocialAccount resolveAccount(Long accountId, SocialPlatform platform) {
    if (accountId == null) {
      return null;
    }
    SocialAccount account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conta social não encontrada."));
    if (account.getPlatform() != platform) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A conta social não pertence à rede selecionada.");
    }
    return account;
  }

  /** Retorna a causa de bloqueio antes de enfileirar a publicação. */
  private String publicationBlockReason(SocialVideoPublication publication) {
    if (!StringUtils.hasText(publication.getVideoUrl()) && publication.getAsset() == null) {
      return "Informe um asset de vídeo ou URL pública do vídeo antes de publicar.";
    }
    SocialAccount account = publication.getSocialAccount();
    if (account == null) {
      return "Conecte uma conta social de destino antes de publicar.";
    }
    if (account.getStatus() != SocialAccountStatus.CONNECTED) {
      return "A conta social ainda não está conectada via OAuth.";
    }
    if (publication.getPlatform() == SocialPlatform.TIKTOK
        && publication.getVideoFormat() == SocialVideoFormat.TIKTOK_DRAFT) {
      return "TikTok está configurado como rascunho até aprovação do Direct Post.";
    }
    return "";
  }

  /** Constrói payload técnico auditável para o futuro worker de publicação. */
  private String buildPublishPayload(SocialVideoPublication publication) {
    String accountId =
        publication.getSocialAccount() != null
            ? String.valueOf(publication.getSocialAccount().getId())
            : "";
    String videoSource =
        StringUtils.hasText(publication.getVideoUrl())
            ? publication.getVideoUrl()
            : publication.getAsset() != null ? publication.getAsset().getUrl() : "";
    return """
        {"platform":"%s","format":"%s","accountId":"%s","title":"%s","videoUrl":"%s"}
        """
        .formatted(
            publication.getPlatform(),
            publication.getVideoFormat(),
            accountId,
            escapeJson(publication.getTitle()),
            escapeJson(videoSource))
        .trim();
  }

  /** Escapa valores simples para o payload JSON auditável. */
  private String escapeJson(String value) {
    return Optional.ofNullable(value).orElse("").replace("\\", "\\\\").replace("\"", "\\\"");
  }

  /** Retorna o formato padrão de cada rede. */
  private SocialVideoFormat defaultFormat(SocialPlatform platform) {
    return switch (platform) {
      case YOUTUBE -> SocialVideoFormat.YOUTUBE_SHORT;
      case INSTAGRAM -> SocialVideoFormat.INSTAGRAM_REELS;
      case TIKTOK -> SocialVideoFormat.TIKTOK_DRAFT;
    };
  }

  /** Retorna escopos oficiais necessários para a primeira integração. */
  private String requiredScopesFor(SocialPlatform platform) {
    return switch (platform) {
      case YOUTUBE -> "https://www.googleapis.com/auth/youtube.upload";
      case INSTAGRAM -> "instagram_basic,instagram_content_publish,pages_read_engagement";
      case TIKTOK -> "video.upload,video.publish";
    };
  }

  /** Retorna observação operacional padrão de cada rede. */
  private String defaultSetupNotes(SocialPlatform platform, String customNotes) {
    if (StringUtils.hasText(customNotes)) {
      return customNotes.trim();
    }
    return switch (platform) {
      case YOUTUBE ->
          "Criar credencial OAuth no Google Cloud, habilitar YouTube Data API v3 e conectar o canal.";
      case INSTAGRAM ->
          "Usar conta profissional Business/Creator conectada à Meta e publicar Reels via Content Publishing API.";
      case TIKTOK ->
          "Começar com upload/rascunho; Direct Post público depende de aprovação/auditoria do TikTok.";
    };
  }

  /** Converte conta social em resposta REST. */
  private SocialAccountResponse toAccountResponse(SocialAccount account) {
    return new SocialAccountResponse(
        account.getId(),
        account.getPlatform(),
        account.getDisplayName(),
        account.getHandle(),
        account.getExternalAccountId(),
        account.getConnectionMode(),
        account.getStatus(),
        account.getRequiredScopes(),
        account.getSetupNotes(),
        account.getConnectedAt());
  }

  /** Converte publicação em resposta REST com a última métrica. */
  private SocialVideoPublicationResponse toPublicationResponse(SocialVideoPublication publication) {
    SocialPublicationMetricResponse metric =
        metricRepository
            .findFirstByPublicationIdOrderByCapturedAtDesc(publication.getId())
            .map(this::toMetricResponse)
            .orElse(null);
    Product product = publication.getProduct();
    SocialAccount account = publication.getSocialAccount();
    return new SocialVideoPublicationResponse(
        publication.getId(),
        product.getId(),
        product.getName(),
        product.getSlug(),
        publication.getAsset() != null ? publication.getAsset().getId() : null,
        growthContentRepository.findByPublicationId(publication.getId()).stream()
            .map(SocialGrowthContent::getId)
            .findFirst()
            .orElse(null),
        account != null ? account.getId() : null,
        account != null ? account.getDisplayName() : null,
        publication.getPlatform(),
        publication.getVideoFormat(),
        publication.getStatus(),
        publication.getTitle(),
        publication.getCaption(),
        publication.getHashtags(),
        publication.getVideoUrl(),
        publication.getPublishedUrl(),
        publication.getExternalPostId(),
        publication.getFailureReason(),
        publication.getPublishPayloadJson(),
        publication.getScheduledAt(),
        publication.getQueuedAt(),
        publication.getPublishedAt(),
        metric,
        account != null ? account.getExternalAccountId() : null);
  }

  /** Converte métrica de publicação em resposta REST. */
  private SocialPublicationMetricResponse toMetricResponse(SocialPublicationMetric metric) {
    return new SocialPublicationMetricResponse(
        metric.getId(),
        metric.getViews(),
        metric.getEngagedViews(),
        metric.getAverageViewDurationSeconds(),
        metric.getRecurringViewers(),
        metric.getSubscribersGained(),
        metric.getLikes(),
        metric.getComments(),
        metric.getShares(),
        metric.getClicks(),
        metric.getLandingSessions(),
        metric.getLeads(),
        metric.getCheckoutsStarted(),
        metric.getSalesApproved(),
        metric.getRevenue(),
        metric.getRawPayloadJson(),
        metric.getCapturedAt());
  }

  /** Converte o plano persistido em relatório comercial completo. */
  private SocialGrowthPlanResponse toGrowthPlanResponse(SocialGrowthPlan plan) {
    List<SocialGrowthContent> contents =
        growthContentRepository.findByPlanIdOrderByPlannedAtAscCreatedAtAsc(plan.getId());
    return new SocialGrowthPlanResponse(
        plan.getId(),
        plan.getProduct().getId(),
        plan.getProduct().getName(),
        plan.getName(),
        plan.getAudience(),
        plan.getCommercialHypothesis(),
        plan.getCommercialObjective(),
        plan.getPrimaryCta(),
        plan.getDestinationUrl(),
        plan.getUtmCampaign(),
        plan.getStatus(),
        plan.getStartsOn(),
        plan.getEndsOn(),
        contents.stream().map(this::toGrowthContentResponse).toList(),
        buildPlanPerformance(contents));
  }

  /** Converte uma pauta em contrato da tela. */
  private SocialGrowthContentResponse toGrowthContentResponse(SocialGrowthContent content) {
    return new SocialGrowthContentResponse(
        content.getId(),
        content.getContentType(),
        content.getPillar(),
        content.getTopic(),
        content.getFunnelStage(),
        content.getCta(),
        content.getTrackingCode(),
        content.getTrackingUrl(),
        content.getStatus(),
        content.getPlannedAt(),
        content.getPublication() != null ? content.getPublication().getId() : null);
  }

  /** Consolida os snapshots mais recentes e recomenda o próximo movimento do plano. */
  private SocialGrowthPlanPerformanceResponse buildPlanPerformance(
      List<SocialGrowthContent> contents) {
    List<SocialPublicationMetric> metrics = new ArrayList<>();
    for (SocialGrowthContent content : contents) {
      if (content.getPublication() != null) {
        metricRepository
            .findFirstByPublicationIdOrderByCapturedAtDesc(content.getPublication().getId())
            .ifPresent(metrics::add);
      }
    }
    long views = sum(metrics, SocialPublicationMetric::getViews);
    long engagedViews = sum(metrics, SocialPublicationMetric::getEngagedViews);
    long recurringViewers = sum(metrics, SocialPublicationMetric::getRecurringViewers);
    long landingSessions = sum(metrics, SocialPublicationMetric::getLandingSessions);
    long leads = sum(metrics, SocialPublicationMetric::getLeads);
    long checkouts = sum(metrics, SocialPublicationMetric::getCheckoutsStarted);
    long sales = sum(metrics, SocialPublicationMetric::getSalesApproved);
    BigDecimal revenue =
        metrics.stream()
            .map(SocialPublicationMetric::getRevenue)
            .filter(value -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    String decision;
    String reason;
    if (contents.isEmpty()) {
      decision = "PLANEJAR";
      reason = "Adicione pautas com pilar, etapa do funil e CTA antes de avaliar o canal.";
    } else if (metrics.isEmpty()) {
      decision = "COLETAR_AMOSTRA";
      reason = "Ainda não existem métricas atribuídas às publicações deste plano.";
    } else if (sales > 0 || leads > 0) {
      decision = "CONTINUAR";
      reason =
          "O conteúdo já gerou lead ou venda atribuída; preserve o formato e amplie a amostra.";
    } else if (views > 0 && landingSessions == 0) {
      decision = "AJUSTAR_CTA";
      reason =
          "Há audiência, mas nenhuma sessão rastreada na landing; revise CTA e passagem do conteúdo.";
    } else {
      decision = "COLETAR_AMOSTRA";
      reason = "O funil ainda não possui sinal comercial suficiente para encerrar o formato.";
    }
    return new SocialGrowthPlanPerformanceResponse(
        views,
        engagedViews,
        recurringViewers,
        landingSessions,
        leads,
        checkouts,
        sales,
        revenue,
        decision,
        reason);
  }

  /** Soma uma métrica opcional sem transformar ausência em valor negativo. */
  private long sum(
      List<SocialPublicationMetric> metrics,
      java.util.function.Function<SocialPublicationMetric, Long> extractor) {
    return metrics.stream()
        .map(extractor)
        .filter(value -> value != null)
        .mapToLong(Long::longValue)
        .sum();
  }

  /** Gera a URL canônica de atribuição sem depender de inferência do frontend. */
  private String buildTrackingUrl(SocialGrowthPlan plan, SocialGrowthContent content) {
    String separator = plan.getDestinationUrl().contains("?") ? "&" : "?";
    return plan.getDestinationUrl()
        + separator
        + "utm_source=youtube&utm_medium=organic&utm_campaign="
        + encode(plan.getUtmCampaign())
        + "&utm_content="
        + encode(content.getTrackingCode());
  }

  /** Codifica valores usados na URL de atribuição. */
  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /** Valida identificador obrigatório. */
  private Long requireId(Long id, String message) {
    if (id == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return id;
  }

  /** Valida rede obrigatória. */
  private SocialPlatform requirePlatform(SocialPlatform platform) {
    if (platform == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a rede social.");
    }
    return platform;
  }

  /** Normaliza texto obrigatório. */
  private String normalizeRequired(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return value.trim();
  }

  /** Normaliza texto opcional. */
  private String normalizeOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Garante que métricas negativas não entrem na leitura comercial. */
  private Long nonNegative(Long value) {
    if (value == null) {
      return null;
    }
    return Math.max(0L, value);
  }

  /** Garante que valores decimais negativos não entrem na leitura comercial. */
  private BigDecimal nonNegative(BigDecimal value) {
    if (value == null) {
      return null;
    }
    return value.max(BigDecimal.ZERO);
  }

  /** Normaliza identificadores de rastreamento para uso seguro em UTM. */
  private String normalizeTrackingValue(String value, String message) {
    String normalized = normalizeRequired(value, message).toLowerCase();
    return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
  }

  /** Normaliza a falha operacional recebida do worker para exibição na tela. */
  private String normalizeFailureReason(String errorCategory, String errorMessage) {
    String category = StringUtils.hasText(errorCategory) ? errorCategory.trim() : "WORKER_ERROR";
    String message =
        StringUtils.hasText(errorMessage)
            ? errorMessage.trim()
            : "Falha não detalhada pelo executor de mídia social.";
    return category + ": " + message;
  }
}
