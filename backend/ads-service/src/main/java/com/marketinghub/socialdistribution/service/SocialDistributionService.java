package com.marketinghub.socialdistribution.service;

import com.marketinghub.media.Asset;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.socialdistribution.SocialAccountRepository;
import com.marketinghub.repository.jpa.socialdistribution.SocialPublicationMetricRepository;
import com.marketinghub.repository.jpa.socialdistribution.SocialVideoPublicationRepository;
import com.marketinghub.socialdistribution.*;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
  private final ProductRepository productRepository;
  private final AssetRepository assetRepository;

  /** Inicializa o serviço com repositórios do módulo e vínculos de produto/asset. */
  public SocialDistributionService(
      SocialAccountRepository accountRepository,
      SocialVideoPublicationRepository publicationRepository,
      SocialPublicationMetricRepository metricRepository,
      ProductRepository productRepository,
      AssetRepository assetRepository) {
    this.accountRepository = accountRepository;
    this.publicationRepository = publicationRepository;
    this.metricRepository = metricRepository;
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
    Product product =
        productRepository
            .findById(requireId(request.productId(), "Informe o produto."))
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
    publication.setTitle(normalizeRequired(request.title(), "Informe o título do vídeo."));
    publication.setCaption(normalizeOptional(request.caption()));
    publication.setHashtags(normalizeOptional(request.hashtags()));
    publication.setVideoUrl(normalizeOptional(request.videoUrl()));
    publication.setScheduledAt(request.scheduledAt());
    publication.setPublishPayloadJson(buildPublishPayload(publication));
    return toPublicationResponse(publicationRepository.save(publication));
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
    metric.setLikes(nonNegative(request.likes()));
    metric.setComments(nonNegative(request.comments()));
    metric.setShares(nonNegative(request.shares()));
    metric.setClicks(nonNegative(request.clicks()));
    metric.setRawPayloadJson(normalizeOptional(request.rawPayloadJson()));
    metric.setCapturedAt(Optional.ofNullable(request.capturedAt()).orElse(Instant.now()));
    return toMetricResponse(metricRepository.save(metric));
  }

  /** Retorna a publicação pelo identificador interno. */
  private SocialVideoPublication getPublication(Long publicationId) {
    return publicationRepository
        .findById(requireId(publicationId, "Informe a publicação."))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada."));
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
        metric);
  }

  /** Converte métrica de publicação em resposta REST. */
  private SocialPublicationMetricResponse toMetricResponse(SocialPublicationMetric metric) {
    return new SocialPublicationMetricResponse(
        metric.getId(),
        metric.getViews(),
        metric.getLikes(),
        metric.getComments(),
        metric.getShares(),
        metric.getClicks(),
        metric.getRawPayloadJson(),
        metric.getCapturedAt());
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
}
