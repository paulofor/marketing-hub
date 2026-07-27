package com.marketinghub.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.imagegenerator.dto.ImageGeneratorRequest;
import com.marketinghub.imagegenerator.dto.ImageGeneratorResponse;
import com.marketinghub.imagegenerator.service.ImageGeneratorService;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.product.Product;
import com.marketinghub.product.ProductVideoImage;
import com.marketinghub.product.ProductVideoProviderAvatar;
import com.marketinghub.product.ProductVideoSeedImageReviewStatus;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.dto.ProductVideoProviderAvatarDto;
import com.marketinghub.product.dto.RegisterProductVideoProviderAvatarRequest;
import com.marketinghub.product.service.financialsummary.ProductFinancialAmountResponse;
import com.marketinghub.product.service.financialsummary.ProductFinancialLineResponse;
import com.marketinghub.product.service.financialsummary.ProductFinancialSummaryResponse;
import com.marketinghub.product.service.experimentcomparison.ProductExperimentComparisonExperimentResponse;
import com.marketinghub.product.service.experimentcomparison.ProductExperimentComparisonFunnelStageResponse;
import com.marketinghub.product.service.experimentcomparison.ProductExperimentComparisonResponse;
import com.marketinghub.product.service.organicvideoplan.ProductOrganicVideoDecisionRuleResponse;
import com.marketinghub.product.service.organicvideoplan.ProductOrganicVideoPlanItemResponse;
import com.marketinghub.product.service.organicvideoplan.ProductOrganicVideoPlanResponse;
import com.marketinghub.product.service.updateVideoSeedImage.UpdateProductVideoSeedImageRequest;
import com.marketinghub.product.service.videoimage.GenerateProductVideoImagesRequest;
import com.marketinghub.product.service.videoimage.ProductVideoImageDto;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.product.ProductVideoImageRepository;
import com.marketinghub.repository.jpa.product.ProductVideoProviderAvatarRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import com.marketinghub.storage.StorageException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerenciar o cadastro comercial de produtos digitais. */
@Service
public class ProductService {
  private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
  private static final Pattern EXPERIMENT_ID_PATTERN =
      Pattern.compile("(?i)(?:experimento|experiment|exp)[^0-9]*(\\d+)|#(\\d+)");

  private final ProductRepository repository;
  private final InstagramAccountRepository accountRepository;
  private final MarketNicheRepository marketNicheRepository;
  private final AssetRepository assetRepository;
  private final ProductVideoImageRepository productVideoImageRepository;
  private final ProductVideoProviderAvatarRepository productVideoProviderAvatarRepository;
  private final ImageGeneratorService imageGeneratorService;
  private final AssetStorageService assetStorageService;
  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;
  private static final BigDecimal BRL_PER_USD = new BigDecimal("5.00");

  /** Inicializa o serviço com os repositórios necessários para cadastro de produtos. */
  public ProductService(
      ProductRepository repository,
      InstagramAccountRepository accountRepository,
      MarketNicheRepository marketNicheRepository,
      AssetRepository assetRepository,
      ProductVideoImageRepository productVideoImageRepository,
      ProductVideoProviderAvatarRepository productVideoProviderAvatarRepository,
      ImageGeneratorService imageGeneratorService,
      AssetStorageService assetStorageService,
      ObjectMapper objectMapper,
      JdbcTemplate jdbcTemplate) {
    this.repository = repository;
    this.accountRepository = accountRepository;
    this.marketNicheRepository = marketNicheRepository;
    this.assetRepository = assetRepository;
    this.productVideoImageRepository = productVideoImageRepository;
    this.productVideoProviderAvatarRepository = productVideoProviderAvatarRepository;
    this.imageGeneratorService = imageGeneratorService;
    this.assetStorageService = assetStorageService;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Lista personagens de vídeo cadastrados por provider para um produto. */
  @Transactional(readOnly = true)
  public List<ProductVideoProviderAvatarDto> listVideoProviderAvatars(Long productId) {
    return productVideoProviderAvatarRepository
        .findByProductIdOrderByCreatedAtDesc(productId)
        .stream()
        .map(this::toProductVideoProviderAvatarDto)
        .toList();
  }

  /** Registra ou atualiza o avatar/personagem retornado por um provider de vídeo. */
  @Transactional
  public ProductVideoProviderAvatarDto registerVideoProviderAvatar(
      Long productId, RegisterProductVideoProviderAvatarRequest request) {
    Product product = getProduct(productId);
    Long sourceAssetId = requireSourceAssetId(request.sourceAssetId());
    Asset sourceAsset =
        assetRepository
            .findById(sourceAssetId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Asset fonte não encontrado."));
    if (sourceAsset.getType() != AssetType.IMAGE || sourceAsset.getStatus() != AssetStatus.READY) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Avatar de vídeo exige asset IMAGE em status READY.");
    }
    String provider = normalizeRequired(request.provider(), "Informe o provider do avatar.");
    String characterName =
        normalizeRequired(request.characterName(), "Informe o nome da personagem.");
    String sourceImageUrl =
        normalizeRequired(
            Optional.ofNullable(request.sourceImageUrl()).orElse(sourceAsset.getUrl()),
            "Informe a URL pública da imagem fonte.");
    ProductVideoProviderAvatar avatar =
        productVideoProviderAvatarRepository
            .findFirstByProductIdAndProviderIgnoreCaseAndSourceAssetId(
                productId, provider, sourceAssetId)
            .orElseGet(ProductVideoProviderAvatar::new);
    avatar.setProduct(product);
    avatar.setSourceAsset(sourceAsset);
    avatar.setProvider(provider);
    avatar.setCharacterName(characterName);
    avatar.setProviderAvatarId(normalizeOptional(request.providerAvatarId()));
    avatar.setProviderAvatarGroupId(normalizeOptional(request.providerAvatarGroupId()));
    avatar.setProviderStatus(
        Optional.ofNullable(normalizeOptional(request.providerStatus())).orElse("REFERENCE_ONLY"));
    avatar.setSourceImageUrl(sourceImageUrl);
    avatar.setSupportsReusableAvatar(Boolean.TRUE.equals(request.supportsReusableAvatar()));
    avatar.setNotes(normalizeOptional(request.notes()));
    return toProductVideoProviderAvatarDto(productVideoProviderAvatarRepository.save(avatar));
  }

  /** Cria e persiste um produto comercial com seus atributos de venda e entrega. */
  @Transactional
  public Product createProduct(CreateProductRequest request) {
    Product product = Product.builder().build();
    applyRequest(product, request);
    return repository.save(product);
  }

  /** Atualiza um produto comercial existente com os dados informados pela tela. */
  @Transactional
  public Product updateProduct(Long id, CreateProductRequest request) {
    Product product = getProduct(id);
    applyRequest(product, request);
    return repository.save(product);
  }

  /** Insere a jornada persuasiva interativa padrão no contrato PDE do produto. */
  @Transactional
  public Product applyDefaultPdePersuasiveJourney(Long id) {
    Product product = getProduct(id);
    ObjectNode contract = readPdeExperienceContract(product);
    if (!contract.hasNonNull("slug") && product.getSlug() != null && !product.getSlug().isBlank()) {
      contract.put("slug", product.getSlug().trim());
    }
    contract.set("persuasiveJourney", buildDefaultPdePersuasiveJourney(product));
    try {
      product.setPdeExperienceJson(
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contract));
    } catch (JsonProcessingException ex) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Não foi possível atualizar a jornada persuasiva PDE",
          ex);
    }
    return repository.save(product);
  }

  /** Atualiza a imagem semente oficial usada para gerar avatares e vídeos do produto. */
  @Transactional
  public Product updateVideoSeedImage(Long id, UpdateProductVideoSeedImageRequest request) {
    Product product = getProduct(id);
    if (request.assetId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe o asset da imagem semente.");
    }
    Asset asset =
        assetRepository
            .findById(request.assetId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset não encontrado."));
    if (asset.getType() != AssetType.IMAGE) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A imagem semente deve usar um asset do tipo IMAGE.");
    }
    if (asset.getStatus() != AssetStatus.READY) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A imagem semente precisa estar pronta antes da revisão.");
    }
    ProductVideoSeedImageReviewStatus reviewStatus =
        request.reviewStatus() != null
            ? request.reviewStatus()
            : ProductVideoSeedImageReviewStatus.PENDING;
    product.setVideoSeedImageAsset(asset);
    product.setVideoSeedCharacterName(normalizeOptional(request.characterName()));
    product.setVideoSeedReviewStatus(reviewStatus);
    product.setVideoSeedReviewNotes(normalizeOptional(request.reviewNotes()));
    product.setVideoSeedReviewedBy(normalizeOptional(request.reviewedBy()));
    product.setVideoSeedReviewedAt(Instant.now());
    markGalleryImageReviewed(product.getId(), asset.getId(), reviewStatus, request.reviewNotes());
    return repository.save(product);
  }

  /** Lista a galeria de imagens geradas exclusivamente para vídeos do produto. */
  @Transactional(readOnly = true)
  public List<ProductVideoImageDto> listVideoImages(Long productId) {
    return productVideoImageRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
        .map(this::toProductVideoImageDto)
        .toList();
  }

  /** Gera imagens por prompt, salva como assets e vincula à galeria de vídeos do produto. */
  @Transactional
  public List<ProductVideoImageDto> generateVideoImages(
      Long productId, GenerateProductVideoImagesRequest request) {
    Product product = getProduct(productId);
    String prompt = normalizeRequiredPrompt(request.prompt());
    ImageGeneratorResponse response =
        imageGeneratorService.generate(new ImageGeneratorRequest(prompt));
    if (response.images() == null || response.images().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Nenhuma imagem foi gerada para o produto.");
    }
    return response.images().stream()
        .map(image -> saveGeneratedVideoImage(product, prompt, image))
        .map(this::toProductVideoImageDto)
        .toList();
  }

  /** Consolida custos, receitas e lucro do produto para a tela financeira. */
  @Transactional(readOnly = true)
  public ProductFinancialSummaryResponse getFinancialSummary(Long productId) {
    Product product = getProduct(productId);
    Instant now = Instant.now();
    Instant monthStart =
        LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant yearStart =
        LocalDate.now(ZoneOffset.UTC).withDayOfYear(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    Long marketNicheId = product.getMarketNiche() != null ? product.getMarketNiche().getId() : null;

    ProductFinancialLineResponse videoProduction =
        costLine(
            "VIDEO_PRODUCTION",
            "Produção de vídeo",
            videoCostUsd(marketNicheId, monthStart, now),
            videoCostUsd(marketNicheId, yearStart, now),
            "Soma de cost + audio_cost dos vídeos de experimentos do mesmo nicho do produto.");
    ProductFinancialLineResponse media =
        brlCostLine(
            "MEDIA",
            "Mídia paga",
            mediaCostBrl(marketNicheId, monthStart, now),
            mediaCostBrl(marketNicheId, yearStart, now),
            "Soma de spend em experiment_campaign_metric para experimentos do mesmo nicho do produto.");
    ProductFinancialLineResponse pdeProduction =
        brlCostLine(
            "PDE_PRODUCTION",
            "Produção do próprio PDE",
            productLevelPdeCost(product),
            productLevelPdeCost(product),
            "Valor ai_cost registrado no cadastro do produto.");
    ProductFinancialLineResponse otherCosts =
        brlCostLine(
            "OTHER",
            "Outros custos",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "Nenhum custo adicional categorizado foi localizado no contrato atual.");

    ProductFinancialLineResponse revenue =
        brlRevenueLine(
            revenueBrl(marketNicheId, monthStart, now), revenueBrl(marketNicheId, yearStart, now));
    ProductFinancialAmountResponse monthlyProfit =
        subtract(
            revenue.monthly(),
            sumAmounts(
                videoProduction.monthly(),
                media.monthly(),
                pdeProduction.monthly(),
                otherCosts.monthly()));
    ProductFinancialAmountResponse annualProfit =
        subtract(
            revenue.annual(),
            sumAmounts(
                videoProduction.annual(),
                media.annual(),
                pdeProduction.annual(),
                otherCosts.annual()));
    ProductFinancialLineResponse profit =
        new ProductFinancialLineResponse(
            "PROFIT",
            "Lucro",
            monthlyProfit,
            annualProfit,
            "Receitas aprovadas menos custos categorizados.");

    return new ProductFinancialSummaryResponse(
        product.getId(),
        product.getName(),
        product.getSlug(),
        BRL_PER_USD,
        monthStart,
        yearStart,
        List.of(videoProduction, media, pdeProduction, otherCosts),
        revenue,
        profit);
  }

  /** Consolida experimentos do mesmo produto para comparação comercial automática. */
  @Transactional(readOnly = true)
  public ProductExperimentComparisonResponse getExperimentComparison(Long productId) {
    Product product = getProduct(productId);
    List<Long> explicitExperimentIds = extractExperimentIds(product.getAssociatedExperiments());
    List<Object> params = new ArrayList<>();
    StringBuilder where = new StringBuilder(" WHERE 1 = 0");
    if (product.getMarketNiche() != null && product.getMarketNiche().getId() != null) {
      where.append(" OR e.niche_id = ?");
      params.add(product.getMarketNiche().getId());
    }
    if (!explicitExperimentIds.isEmpty()) {
      where.append(" OR e.id IN (");
      where.append("?,".repeat(explicitExperimentIds.size()));
      where.setLength(where.length() - 1);
      where.append(")");
      params.addAll(explicitExperimentIds);
    }
    if (params.isEmpty()) {
      return new ProductExperimentComparisonResponse(
          product.getId(),
          product.getName(),
          product.getSlug(),
          product.getCommercialStatus(),
          "Vincule o produto a um nicho ou informe experimentos associados para comparar histórico.",
          List.of());
    }

    String sql =
        """
        SELECT e.id AS experiment_id,
               e.name AS experiment_name,
               e.status AS experiment_status,
               e.campaign_objective AS campaign_objective,
               e.experiment_type AS experiment_type,
               e.start_date AS start_date,
               e.end_date AS end_date,
               e.daily_budget AS daily_budget,
               e.unit_price_brl AS unit_price_brl,
               e.updated_at AS updated_at,
               e.hypothesis AS hypothesis,
               e.funnel_promise AS funnel_promise,
               e.learned_lessons AS learned_lessons,
               COALESCE(metric.impressions, 0) AS impressions,
               COALESCE(metric.reach, 0) AS reach,
               COALESCE(metric.clicks, 0) AS clicks,
               COALESCE(metric.leads, 0) AS leads,
               COALESCE(metric.spend, 0) AS spend,
               COALESCE(metric.cpc, 0) AS cpc,
               COALESCE(metric.cpl, 0) AS cpl,
               (
                   SELECT fac.status
                   FROM facebook_ads_campaign fac
                   WHERE fac.experiment_id = e.id
                   ORDER BY fac.updated_at DESC, fac.created_at DESC
                   LIMIT 1
               ) AS campaign_status,
               (
                   SELECT COUNT(*)
                   FROM creative_variant cv
                   WHERE cv.experiment_id = e.id
               ) AS total_creatives,
               CASE WHEN e.creative_approved = 1 THEN (
                   SELECT COUNT(*)
                   FROM creative_variant cv
                   WHERE cv.experiment_id = e.id
               ) ELSE 0 END AS approved_creatives
        FROM experiment e
        LEFT JOIN experiment_campaign_metric metric ON metric.experiment_id = e.id
        """
            + where
            + " ORDER BY e.updated_at DESC, e.id DESC";
    List<ProductExperimentComparisonExperimentResponse> experiments =
        jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
              Long experimentId = rs.getLong("experiment_id");
              List<ProductExperimentComparisonFunnelStageResponse> funnelStages =
                  listFunnelStages(experimentId);
              long impressions = rs.getLong("impressions");
              long clicks = rs.getLong("clicks");
              long leads = rs.getLong("leads");
              BigDecimal spend = rs.getBigDecimal("spend");
              return new ProductExperimentComparisonExperimentResponse(
                  experimentId,
                  rs.getString("experiment_name"),
                  rs.getString("experiment_status"),
                  rs.getString("campaign_status"),
                  rs.getString("campaign_objective"),
                  rs.getString("experiment_type"),
                  rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null,
                  rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null,
                  rs.getBigDecimal("daily_budget"),
                  rs.getBigDecimal("unit_price_brl"),
                  impressions,
                  rs.getLong("reach"),
                  clicks,
                  leads,
                  spend,
                  rs.getBigDecimal("cpc"),
                  rs.getBigDecimal("cpl"),
                  rs.getLong("approved_creatives"),
                  rs.getLong("total_creatives"),
                  funnelStages,
                  rs.getString("hypothesis"),
                  rs.getString("funnel_promise"),
                  rs.getString("learned_lessons"),
                  recommendExperimentAction(impressions, clicks, leads, spend, funnelStages),
                  rs.getTimestamp("updated_at") != null
                      ? rs.getTimestamp("updated_at").toInstant()
                      : null);
            },
            params.toArray());
    return new ProductExperimentComparisonResponse(
        product.getId(),
        product.getName(),
        product.getSlug(),
        product.getCommercialStatus(),
        recommendProductAction(experiments),
        experiments);
  }

  /** Extrai identificadores numéricos de experimentos informados no cadastro comercial. */
  private List<Long> extractExperimentIds(String associatedExperiments) {
    if (!StringUtils.hasText(associatedExperiments)) {
      return List.of();
    }
    Matcher matcher = EXPERIMENT_ID_PATTERN.matcher(associatedExperiments);
    List<Long> ids = new ArrayList<>();
    while (matcher.find()) {
      String rawId = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
      if (rawId != null) {
        ids.add(Long.parseLong(rawId));
      }
    }
    return ids.stream().distinct().toList();
  }

  /** Lista as etapas de funil com eventos registrados para um experimento. */
  private List<ProductExperimentComparisonFunnelStageResponse> listFunnelStages(Long experimentId) {
    return jdbcTemplate.query(
        """
        SELECT stage, COUNT(*) AS total
        FROM experiment_funnel_event
        WHERE experiment_id = ?
        GROUP BY stage
        ORDER BY MIN(id)
        """,
        (rs, rowNum) -> {
          String stageCode = rs.getString("stage");
          return new ProductExperimentComparisonFunnelStageResponse(
              stageCode, labelFunnelStage(stageCode), rs.getLong("total"));
        },
        experimentId);
  }

  /** Traduz códigos canônicos de funil para nomes comerciais legíveis. */
  private String labelFunnelStage(String stageCode) {
    if (stageCode == null) {
      return "Etapa não informada";
    }
    return switch (stageCode) {
      case "VISUALIZACAO_ANUNCIO" -> "Visualização do anúncio";
      case "ACESSO_FORM_LEAD" -> "Acesso ao formulário de lead";
      case "VISUALIZACAO_FORM" -> "Visualização do formulário";
      case "ENVIO_FORM" -> "Envio do formulário";
      case "ABERTURA_EMAIL_AMOSTRA" -> "Abertura do e-mail de amostra";
      case "ACESSO_CHECKOUT" -> "Acesso ao checkout";
      case "COMPRA" -> "Compra";
      case "ABERTURA_EMAIL_COMPRA" -> "Abertura do e-mail de compra";
      case "DOWNLOAD_MATERIAL_PAGO" -> "Download do material pago";
      default -> stageCode;
    };
  }

  /** Recomenda a próxima ação de marketing a partir dos sinais comerciais do experimento. */
  private String recommendExperimentAction(
      long impressions,
      long clicks,
      long leads,
      BigDecimal spend,
      List<ProductExperimentComparisonFunnelStageResponse> funnelStages) {
    long checkoutAccesses = funnelTotal(funnelStages, "ACESSO_CHECKOUT");
    long purchases = funnelTotal(funnelStages, "COMPRA");
    if (purchases > 0) {
      return "Escalar com cautela e criar variação para aumentar volume mantendo a promessa.";
    }
    if (checkoutAccesses > 0) {
      return "Aprofundar oferta e checkout: existe intenção, mas ainda falta compra registrada.";
    }
    if (clicks > 0 && leads == 0 && funnelStages.isEmpty()) {
      return "Corrigir ativação pós-clique: o anúncio gera interesse, mas o funil não registra entrada.";
    }
    if (impressions >= 100 && clicks == 0) {
      return "Revisar criativo e ângulo: houve entrega, mas sem clique suficiente.";
    }
    if (spend != null && spend.compareTo(new BigDecimal("20.00")) >= 0 && leads == 0) {
      return "Pausar ou corrigir antes de gastar mais: investimento inicial sem avanço no funil.";
    }
    return "Aguardar mais dados antes de decidir; ainda não há sinal comercial suficiente.";
  }

  /** Soma eventos de uma etapa do funil no resumo comparativo. */
  private long funnelTotal(
      List<ProductExperimentComparisonFunnelStageResponse> funnelStages, String stageCode) {
    return funnelStages.stream()
        .filter(stage -> stage.stageCode().equals(stageCode))
        .mapToLong(ProductExperimentComparisonFunnelStageResponse::total)
        .sum();
  }

  /** Resume a ação recomendada para o conjunto de experimentos do produto. */
  private String recommendProductAction(
      List<ProductExperimentComparisonExperimentResponse> experiments) {
    if (experiments.isEmpty()) {
      return "Sem histórico comparável; criar ou vincular experimentos antes de decidir escala.";
    }
    boolean hasPurchase =
        experiments.stream()
            .flatMap(experiment -> experiment.funnelStages().stream())
            .anyMatch(stage -> "COMPRA".equals(stage.stageCode()) && stage.total() > 0);
    if (hasPurchase) {
      return "Priorizar o experimento com compra registrada e criar variações de escala sem trocar a promessa central.";
    }
    boolean hasClicksWithoutFunnel =
        experiments.stream()
            .anyMatch(
                experiment ->
                    experiment.clicks() != null
                        && experiment.clicks() > 0
                        && experiment.funnelStages().isEmpty());
    if (hasClicksWithoutFunnel) {
      return "Priorizar correção da ativação/funil antes de comparar novos criativos ou públicos.";
    }
    return "Comparar os criativos e manter rodando apenas testes com entrega suficiente para aprendizado.";
  }

  /** Monta o plano recomendado de vídeos orgânicos para conduzir desconhecidos ao desejo. */
  @Transactional(readOnly = true)
  public ProductOrganicVideoPlanResponse getOrganicVideoPlan(Long productId) {
    Product product = getProduct(productId);
    return new ProductOrganicVideoPlanResponse(
        product.getId(),
        product.getName(),
        product.getSlug(),
        "9 vídeos em 7 dias",
        "Validar atenção, identificação e intenção antes de aumentar CTA ou levar o criativo para anúncio.",
        "7 dias, com 1 publicação diária e 2 dias com reforço de segundo vídeo.",
        "TikTok e Instagram Reels como leitura principal; YouTube Shorts como reaproveitamento.",
        "6 vídeos de entretenimento/dor cotidiana criam relevância, 2 educativos constroem autoridade e 1 direto testa conversão para diagnóstico.",
        buildOrganicVideoPlanItems(product),
        buildOrganicVideoDecisionRules(),
        List.of(
            "Começar por situação reconhecível antes de falar do produto.",
            "Usar o diagnóstico como próximo passo leve, não como venda agressiva.",
            "Comparar retenção, comentários e cliques por categoria antes de mudar a oferta.",
            "Só transformar vídeo direto em anúncio quando ele converter mesmo com alcance menor."));
  }

  /** Monta uma linha de custo informada originalmente em dólares. */
  private ProductFinancialLineResponse costLine(
      String type, String label, BigDecimal monthlyUsd, BigDecimal annualUsd, String source) {
    return new ProductFinancialLineResponse(
        type, label, amountFromUsd(monthlyUsd), amountFromUsd(annualUsd), source);
  }

  /** Monta uma linha de custo informada originalmente em reais. */
  private ProductFinancialLineResponse brlCostLine(
      String type, String label, BigDecimal monthlyBrl, BigDecimal annualBrl, String source) {
    return new ProductFinancialLineResponse(
        type, label, amountFromBrl(monthlyBrl), amountFromBrl(annualBrl), source);
  }

  /** Monta a linha de receita do produto a partir das vendas aprovadas. */
  private ProductFinancialLineResponse brlRevenueLine(BigDecimal monthlyBrl, BigDecimal annualBrl) {
    return new ProductFinancialLineResponse(
        "SALES",
        "Receitas de vendas",
        amountFromBrl(monthlyBrl),
        amountFromBrl(annualBrl),
        "Soma de lead_portal_purchase aprovado para experimentos do mesmo nicho do produto.");
  }

  /** Converte um valor em dólares para o par BRL/USD usado pela tela. */
  private ProductFinancialAmountResponse amountFromUsd(BigDecimal usd) {
    BigDecimal normalizedUsd = normalizeMoney(usd);
    return new ProductFinancialAmountResponse(
        normalizeMoney(normalizedUsd.multiply(BRL_PER_USD)), normalizedUsd);
  }

  /** Converte um valor em reais para o par BRL/USD usado pela tela. */
  private ProductFinancialAmountResponse amountFromBrl(BigDecimal brl) {
    BigDecimal normalizedBrl = normalizeMoney(brl);
    return new ProductFinancialAmountResponse(
        normalizedBrl, normalizeMoney(normalizedBrl.divide(BRL_PER_USD, 2, RoundingMode.HALF_UP)));
  }

  /** Soma valores financeiros já normalizados em reais e dólares. */
  private ProductFinancialAmountResponse sumAmounts(ProductFinancialAmountResponse... amounts) {
    BigDecimal brl = BigDecimal.ZERO;
    BigDecimal usd = BigDecimal.ZERO;
    for (ProductFinancialAmountResponse amount : amounts) {
      brl = brl.add(Optional.ofNullable(amount.brl()).orElse(BigDecimal.ZERO));
      usd = usd.add(Optional.ofNullable(amount.usd()).orElse(BigDecimal.ZERO));
    }
    return new ProductFinancialAmountResponse(normalizeMoney(brl), normalizeMoney(usd));
  }

  /** Subtrai custos da receita para obter lucro. */
  private ProductFinancialAmountResponse subtract(
      ProductFinancialAmountResponse revenue, ProductFinancialAmountResponse costs) {
    return new ProductFinancialAmountResponse(
        normalizeMoney(revenue.brl().subtract(costs.brl())),
        normalizeMoney(revenue.usd().subtract(costs.usd())));
  }

  /** Normaliza valores monetários para duas casas decimais. */
  private BigDecimal normalizeMoney(BigDecimal amount) {
    return Optional.ofNullable(amount).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
  }

  /** Soma custos de vídeo em dólares por período para o nicho do produto. */
  private BigDecimal videoCostUsd(Long marketNicheId, Instant start, Instant end) {
    if (marketNicheId == null) {
      return BigDecimal.ZERO;
    }
    return queryBigDecimal(
        """
        SELECT COALESCE(SUM(COALESCE(v.cost, 0) + COALESCE(v.audio_cost, 0)), 0)
        FROM experiment_video_asset v
        JOIN experiment e ON e.id = v.experiment_id
        WHERE e.niche_id = ?
          AND v.created_at >= ?
          AND v.created_at < ?
        """,
        marketNicheId,
        Timestamp.from(start),
        Timestamp.from(end));
  }

  /** Soma custos de mídia em reais por período para o nicho do produto. */
  private BigDecimal mediaCostBrl(Long marketNicheId, Instant start, Instant end) {
    if (marketNicheId == null) {
      return BigDecimal.ZERO;
    }
    return queryBigDecimal(
        """
        SELECT COALESCE(SUM(COALESCE(m.spend, 0)), 0)
        FROM experiment_campaign_metric m
        JOIN experiment e ON e.id = m.experiment_id
        WHERE e.niche_id = ?
          AND (m.date_stop IS NULL OR m.date_stop >= ?)
          AND (m.date_start IS NULL OR m.date_start <= ?)
        """,
        marketNicheId,
        Date.valueOf(LocalDate.ofInstant(start, ZoneOffset.UTC)),
        Date.valueOf(LocalDate.ofInstant(end, ZoneOffset.UTC)));
  }

  /** Retorna o custo de produção PDE registrado diretamente no produto. */
  private BigDecimal productLevelPdeCost(Product product) {
    return Optional.ofNullable(product.getAiCost()).orElse(BigDecimal.ZERO);
  }

  /** Soma receitas aprovadas em reais por período para o nicho do produto. */
  private BigDecimal revenueBrl(Long marketNicheId, Instant start, Instant end) {
    if (marketNicheId == null) {
      return BigDecimal.ZERO;
    }
    return queryBigDecimal(
        """
        SELECT COALESCE(SUM(purchase.amount), 0)
        FROM (
            SELECT DISTINCT p.id, p.amount
            FROM lead_portal_purchase p
            JOIN flow_submissions s ON s.id = p.submission_id
            JOIN lead_portal_flow f ON f.slug = s.flow_slug
            JOIN experiment e ON (e.lead_portal_flow_id = f.id OR f.experiment_id = e.id)
            WHERE e.niche_id = ?
              AND (p.payment_approved_at IS NOT NULL OR p.mp_status = 'approved')
              AND COALESCE(p.payment_approved_at, p.updated_at) >= ?
              AND COALESCE(p.payment_approved_at, p.updated_at) < ?
        ) purchase
        """,
        marketNicheId,
        Timestamp.from(start),
        Timestamp.from(end));
  }

  /** Executa consulta agregada e garante zero quando o banco não retorna valor. */
  private BigDecimal queryBigDecimal(String sql, Object... args) {
    BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
    return Optional.ofNullable(value).orElse(BigDecimal.ZERO);
  }

  /** Define os nove roteiros operacionais do playbook orgânico do produto. */
  private List<ProductOrganicVideoPlanItemResponse> buildOrganicVideoPlanItems(Product product) {
    String cta = StringUtils.hasText(product.getPrimaryCta())
        ? product.getPrimaryCta().trim()
        : "Faça o diagnóstico e veja seu primeiro ajuste.";
    return List.of(
        videoPlanItem(
            1,
            1,
            "ENTRETENIMENTO_DOR",
            "Desconhecido -> relevante",
            "Isso acontece comigo.",
            "TikTok + Reels",
            "POV: você já trocou de roupa 4 vezes e nenhuma parece você.",
            "Cena rápida de troca de looks no espelho, com expressão de frustração cotidiana.",
            "O problema não é falta de roupa; pode ser falta de intenção visual.",
            "Quer descobrir o que está faltando no seu visual?",
            "Retenção nos 3 primeiros segundos e comentários de identificação.",
            "Cortes rápidos; legenda grande; sem citar o produto nos primeiros segundos."),
        videoPlanItem(
            2,
            2,
            "ENTRETENIMENTO_DOR",
            "Desconhecido -> relevante",
            "Eu vivo esse desconforto.",
            "TikTok + Reels",
            "Quando o look está certo, mas ainda parece sem graça.",
            "Antes/depois simples com o mesmo look e mudança de acabamento.",
            "Um detalhe final pode mudar a percepção de presença sem exigir compra nova.",
            "Salve para testar no próximo look.",
            "Salvamentos e retenção média.",
            "Mostrar transformação visual clara; evitar tom professoral."),
        videoPlanItem(
            3,
            3,
            "EDUCATIVO",
            "Relevante -> compreensível",
            "Agora entendo o que pode estar errado.",
            "Reels + Shorts",
            "Ruído visual: o motivo de algumas combinações parecerem improvisadas.",
            "Apontar três elementos competindo no look e remover um deles.",
            "Ruído visual tira clareza da sua presença; reduzir excesso aumenta elegância percebida.",
            "Teste removendo um item antes de sair.",
            "Salvamentos e compartilhamentos.",
            "Usar exemplo visual concreto; uma regra por vídeo."),
        videoPlanItem(
            4,
            4,
            "ENTRETENIMENTO_DOR",
            "Relevante -> curiosidade segura",
            "Talvez exista um jeito simples de resolver.",
            "TikTok + Reels",
            "O acessório errado que faz o look parecer improvisado.",
            "Cena de escolha entre dois acessórios com reação imediata no espelho.",
            "Acessório não é enfeite; é sinal visual. O sinal errado bagunça a mensagem.",
            "Comente qual dos dois parece mais elegante.",
            "Comentários e taxa de conclusão.",
            "Estimular escolha A/B nos comentários."),
        videoPlanItem(
            5,
            5,
            "ENTRETENIMENTO_DOR",
            "Curiosidade segura -> mecanismo plausível",
            "Não preciso virar outra pessoa.",
            "TikTok + Reels",
            "Você não precisa comprar roupa nova. Talvez precise tirar ruído.",
            "Usar o mesmo look em duas versões: com excesso e com intenção.",
            "O mecanismo é reduzir ruído e escolher uma peça-sinal.",
            "Faça o teste da peça-sinal hoje.",
            "Retenção completa e salvamentos.",
            "Falar de mecanismo sem jargão; visual antes da explicação."),
        videoPlanItem(
            5,
            6,
            "EDUCATIVO",
            "Mecanismo plausível -> microexperiência",
            "Consigo testar isso em mim.",
            "Reels + Shorts",
            "O teste do espelho: seu look tem uma peça-sinal ou só peças competindo?",
            "Checklist visual em três tomadas: cor, acabamento, peça-sinal.",
            "Uma peça-sinal orienta a leitura do visual e reduz esforço na escolha.",
            cta,
            "Cliques no diagnóstico e salvamentos.",
            "CTA leve; mostrar o teste antes de mencionar diagnóstico."),
        videoPlanItem(
            6,
            7,
            "ENTRETENIMENTO_DOR",
            "Microexperiência -> valioso para mim",
            "Isso melhora uma situação real minha.",
            "TikTok + Reels",
            "Quando você se arruma para parecer confiante, mas sente que faltou presença.",
            "Situação de saída para trabalho, encontro ou evento simples.",
            "Presença visual nasce de sinais consistentes, não de exagero.",
            "Quer seu primeiro ajuste de presença?",
            "Comentários qualificados e cliques.",
            "Abrir com emoção cotidiana; fechar com pergunta curta."),
        videoPlanItem(
            6,
            8,
            "ENTRETENIMENTO_DOR",
            "Valioso para mim -> desejável",
            "Quero repetir esse ganho.",
            "TikTok + Reels",
            "O look que funciona porque comunica uma intenção.",
            "Montar look em três passos com legenda de intenção em cada peça.",
            "Quando cada elemento tem função, o visual parece mais elegante e menos acidental.",
            "Envie para alguém que sempre diz que nada combina.",
            "Compartilhamentos e retenção.",
            "Evitar venda direta; buscar prova social via compartilhamento."),
        videoPlanItem(
            7,
            9,
            "DIRETO_DIAGNOSTICO",
            "Desejável -> comprável",
            "Quero saber meu próximo passo.",
            "Reels + retargeting",
            "Se você sente que falta presença no visual, comece pelo diagnóstico MUSA.",
            "Tela ou simulação do diagnóstico com promessa de plano de 7 dias.",
            "O diagnóstico transforma sensação vaga em um primeiro plano prático.",
            cta,
            "Cliques, início de diagnóstico e checkout quando houver.",
            "Usar como candidato a retargeting se converter mesmo com alcance menor."));
  }

  /** Cria um item de plano de vídeo orgânico com notas de produção. */
  private ProductOrganicVideoPlanItemResponse videoPlanItem(
      int day,
      int sequence,
      String category,
      String funnelStage,
      String mentalShift,
      String platformPriority,
      String hook,
      String scene,
      String message,
      String callToAction,
      String primaryMetric,
      String productionNote) {
    return new ProductOrganicVideoPlanItemResponse(
        day,
        sequence,
        category,
        funnelStage,
        mentalShift,
        platformPriority,
        hook,
        scene,
        message,
        callToAction,
        primaryMetric,
        List.of(productionNote));
  }

  /** Define as regras de leitura para decidir o próximo movimento comercial. */
  private List<ProductOrganicVideoDecisionRuleResponse> buildOrganicVideoDecisionRules() {
    return List.of(
        new ProductOrganicVideoDecisionRuleResponse(
            "Dor cotidiana",
            "Vídeos de dor geram retenção e comentários acima dos demais.",
            "Aumentar CTA para diagnóstico nos próximos roteiros.",
            "A audiência reconheceu o problema; vale conduzir para microexperiência."),
        new ProductOrganicVideoDecisionRuleResponse(
            "Educativo",
            "Só os vídeos educativos performam melhor.",
            "Ajustar a linha editorial para mais autoridade e demonstração.",
            "O público precisa entender mecanismo antes de aceitar promessa."),
        new ProductOrganicVideoDecisionRuleResponse(
            "Direto para diagnóstico",
            "Vídeo direto converte mesmo com menor alcance.",
            "Usar como retargeting e anúncio de intenção.",
            "Baixo alcance com clique qualificado indica criativo mais próximo de compra."));
  }

  /** Aplica os campos editáveis do cadastro comercial ao produto informado. */
  private void applyRequest(Product product, CreateProductRequest request) {
    product.setSlug(request.getSlug());
    product.setName(request.getName());
    product.setPublicUrl(request.getPublicUrl());
    product.setLogoUrl(request.getLogoUrl());
    product.setColorPalette(request.getColorPalette());
    product.setTargetAudience(request.getTargetAudience());
    product.setLanguageStyle(request.getLanguageStyle());
    product.setCodeModules(request.getCodeModules());
    product.setProductType(request.getProductType());
    product.setCommercialStatus(request.getCommercialStatus());
    product.setCurrentPriceBrl(request.getCurrentPriceBrl());
    product.setPrimaryHypothesisId(request.getPrimaryHypothesisId());
    product.setPrimaryHypothesis(request.getPrimaryHypothesis());
    product.setAssociatedExperiments(request.getAssociatedExperiments());
    product.setCommercialNotes(request.getCommercialNotes());
    product.setSevenDayJourney(request.getSevenDayJourney());
    product.setSupportMaterialPositioning(request.getSupportMaterialPositioning());
    product.setPrimaryCta(request.getPrimaryCta());
    product.setNiche(request.getNiche());
    product.setAvatar(request.getAvatar());
    product.setInstagramAccount(resolveAccount(request.getInstagramAccountId()));
    product.setMarketNiche(resolveNiche(request.getMarketNicheId()));
    product.setExplicitPain(request.getExplicitPain());
    product.setPromise(request.getPromise());
    product.setUniqueMechanism(request.getUniqueMechanism());
    product.setTripwire(request.getTripwire());
    product.setRiskReversal(request.getRiskReversal());
    product.setSocialProof(request.getSocialProof());
    product.setScientificEvidencePack(request.getScientificEvidencePack());
    product.setPdeExperienceJson(validatePdeExperienceJson(request.getPdeExperienceJson()));
    product.setCheckoutMonetization(request.getCheckoutMonetization());
    product.setFunnel(request.getFunnel());
    product.setCreativeVolume(request.getCreativeVolume());
    product.setStorytelling(request.getStorytelling());
    product.setAiCost(request.getAiCost());
  }

  /** Normaliza campos opcionais removendo espaços inúteis antes de persistir. */
  private String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  /** Normaliza e valida o prompt comercial usado para gerar imagens de vídeo. */
  private String normalizeRequiredPrompt(String prompt) {
    if (!StringUtils.hasText(prompt)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o prompt da imagem.");
    }
    return prompt.trim();
  }

  /** Atualiza o status da imagem na galeria para permitir múltiplas aprovadas por produto. */
  private void markGalleryImageReviewed(
      Long productId,
      Long assetId,
      ProductVideoSeedImageReviewStatus reviewStatus,
      String reviewNotes) {
    productVideoImageRepository
        .findFirstByProductIdAndAssetId(productId, assetId)
        .ifPresent(
            image -> {
              image.setReviewStatus(reviewStatus);
              image.setReviewNotes(normalizeOptional(reviewNotes));
              productVideoImageRepository.save(image);
            });
  }

  /** Converte o cadastro de avatar por provider em DTO de resposta. */
  private ProductVideoProviderAvatarDto toProductVideoProviderAvatarDto(
      ProductVideoProviderAvatar avatar) {
    return new ProductVideoProviderAvatarDto(
        avatar.getId(),
        avatar.getProduct().getId(),
        avatar.getSourceAsset().getId(),
        avatar.getProvider(),
        avatar.getCharacterName(),
        avatar.getProviderAvatarId(),
        avatar.getProviderAvatarGroupId(),
        avatar.getProviderStatus(),
        avatar.getSourceImageUrl(),
        avatar.isSupportsReusableAvatar(),
        avatar.getNotes(),
        avatar.getCreatedAt(),
        avatar.getUpdatedAt());
  }

  /** Valida a presença do asset fonte antes de registrar avatar por provider. */
  private Long requireSourceAssetId(Long sourceAssetId) {
    if (sourceAssetId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o asset fonte do avatar.");
    }
    return sourceAssetId;
  }

  /** Normaliza e exige um texto obrigatório para cadastro de avatar por provider. */
  private String normalizeRequired(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return value.trim();
  }

  /** Persiste a imagem gerada no storage, cria o asset e vincula ao produto. */
  private ProductVideoImage saveGeneratedVideoImage(
      Product product, String prompt, ImageGeneratorResponse.ImageGeneratorResult image) {
    String format = StringUtils.hasText(image.outputFormat()) ? image.outputFormat() : "png";
    byte[] bytes = decodeImage(image.imageBase64());
    AssetStorageService.StoredObject storedObject =
        storeGeneratedVideoImage(product, image, bytes, format);
    Asset asset =
        assetRepository.save(
            Asset.builder()
                .type(AssetType.IMAGE)
                .provider(MediaProvider.OPENAI)
                .status(AssetStatus.READY)
                .url(storedObject.publicUrl())
                .externalId(storedObject.storedFileName())
                .model(image.model())
                .prompt(prompt)
                .payload(buildProductVideoImageAssetPayload(product, image, storedObject))
                .build());
    ProductVideoImage productVideoImage =
        ProductVideoImage.builder()
            .product(product)
            .asset(asset)
            .purpose("PRODUCT_SALES_VIDEO")
            .prompt(prompt)
            .reviewStatus(ProductVideoSeedImageReviewStatus.PENDING)
            .build();
    return productVideoImageRepository.save(productVideoImage);
  }

  /** Decodifica o base64 retornado pela IA para persistência em arquivo. */
  private byte[] decodeImage(String imageBase64) {
    if (!StringUtils.hasText(imageBase64)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Imagem gerada sem conteúdo retornado pela IA.");
    }
    return Base64.getDecoder().decode(imageBase64);
  }

  /** Envia a imagem gerada para o storage compartilhado do Marketing Hub. */
  private AssetStorageService.StoredObject storeGeneratedVideoImage(
      Product product,
      ImageGeneratorResponse.ImageGeneratorResult image,
      byte[] bytes,
      String format) {
    String productKey =
        StringUtils.hasText(product.getSlug()) ? product.getSlug() : "product-" + product.getId();
    String filename = image.jobId() + "." + format;
    try {
      return assetStorageService.storeBytes(
          bytes,
          filename,
          "image/" + format,
          new AssetUploadContext(AssetUploadCategory.PRODUCT_VIDEO_IMAGE, null, null, productKey));
    } catch (IOException | StorageException ex) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível salvar a imagem gerada.", ex);
    }
  }

  /** Monta o payload auditável do asset de imagem gerada para vídeo de produto. */
  private String buildProductVideoImageAssetPayload(
      Product product,
      ImageGeneratorResponse.ImageGeneratorResult image,
      AssetStorageService.StoredObject storedObject) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("category", AssetUploadCategory.PRODUCT_VIDEO_IMAGE.name());
    payload.put("product_id", product.getId());
    payload.put("product_slug", product.getSlug());
    payload.put("generation_job_id", image.jobId());
    payload.put("service_tier", image.serviceTier());
    payload.put("output_format", image.outputFormat());
    payload.put("stored_file_name", storedObject.storedFileName());
    payload.put("public_url", storedObject.publicUrl());
    payload.put("storage_medium", storedObject.storedInBucket() ? "CLOUDFLARE_R2" : "LOCAL_FS");
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (IOException ex) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível serializar o asset de imagem.", ex);
    }
  }

  /** Converte o vínculo de imagem de vídeo para DTO de galeria. */
  private ProductVideoImageDto toProductVideoImageDto(ProductVideoImage image) {
    Asset asset = image.getAsset();
    Product product = image.getProduct();
    return new ProductVideoImageDto(
        image.getId(),
        product != null ? product.getId() : null,
        asset != null ? asset.getId() : null,
        asset != null ? asset.getType() : null,
        asset != null ? asset.getProvider() : null,
        asset != null ? asset.getStatus() : null,
        asset != null ? asset.getUrl() : null,
        asset != null ? asset.getModel() : null,
        image.getPurpose(),
        image.getPrompt(),
        image.getReviewStatus(),
        image.getReviewNotes(),
        image.getCreatedAt());
  }

  /** Resolve a conta do Instagram quando ela for informada no cadastro. */
  private InstagramAccount resolveAccount(Long id) {
    if (id == null) {
      return null;
    }
    return accountRepository.findById(id).orElseThrow();
  }

  /** Resolve o nicho de mercado obrigatório para produtos cadastrados pela tela atual. */
  private MarketNiche resolveNiche(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("marketNicheId is required");
    }
    return marketNicheRepository.findById(id).orElseThrow();
  }

  /** Busca um produto pelo identificador interno. */
  public Product getProduct(Long id) {
    return repository.findById(id).orElseThrow();
  }

  /** Lista todos os produtos cadastrados para uso operacional no Marketing Hub. */
  public Iterable<Product> listProducts() {
    return repository.findAll();
  }

  /** Monta a definição pública de mercado do produto em Markdown. */
  @Transactional(readOnly = true)
  public String buildPublicMarketingDefinitionMarkdown(String productCode) {
    Product product =
        findProductByCode(productCode)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));

    StringBuilder markdown = new StringBuilder();
    appendTitle(markdown, product);
    appendSection(
        markdown,
        "1. Identidade do produto",
        line("Nome comercial", product.getName()),
        line("Código do produto", product.getSlug()),
        line("Tipo de produto", product.getProductType()),
        line("Status comercial", product.getCommercialStatus()),
        line("Preço atual", formatPrice(product.getCurrentPriceBrl())),
        line("URL pública", product.getPublicUrl()),
        optionalLine("Logo", product.getLogoUrl()));
    appendSection(
        markdown,
        "2. Mercado e nicho",
        line("Nicho", resolveNiche(product)),
        line("Público alvo", product.getTargetAudience()),
        line("Avatar", product.getAvatar()));
    appendSection(markdown, "3. Hipótese comercial", paragraph(product.getPrimaryHypothesis()));
    appendSection(
        markdown,
        "4. Dor, resultado e mecanismo",
        line("Dor principal", product.getExplicitPain()),
        line("Resultado prometido", product.getPromise()),
        line("Mecanismo único", product.getUniqueMechanism()));
    appendSection(
        markdown,
        "5. Estilo de comunicação",
        line("Linguagem", product.getLanguageStyle()),
        line("Storytelling", product.getStorytelling()),
        line("Paleta visual completa", product.getColorPalette()));
    appendSection(
        markdown,
        "6. Oferta e monetização",
        line("Oferta", product.getTripwire()),
        line("Reversão de risco", product.getRiskReversal()),
        line("Prova", product.getSocialProof()),
        optionalLine("Base científica operacional", product.getScientificEvidencePack()),
        optionalLine("Material de apoio", product.getSupportMaterialPositioning()),
        optionalLine("CTA principal recomendado", product.getPrimaryCta()),
        line("Checkout e monetização", product.getCheckoutMonetization()));
    appendSection(markdown, "7. Jornada de 7 dias", paragraph(product.getSevenDayJourney()));
    appendSection(markdown, "8. Funil de aquisição e venda", paragraph(product.getFunnel()));
    appendSection(
        markdown,
        "9. Criativos e escala",
        line("Volume criativo esperado", product.getCreativeVolume()),
        line("Experimentos associados", product.getAssociatedExperiments()));
    appendSection(
        markdown,
        "10. Aprendizados e próximos ajustes de marketing",
        paragraph(product.getCommercialNotes()));
    return markdown.toString();
  }

  /** Retorna o contrato JSON da experiência PDE publicada pelo Marketing Hub. */
  @Transactional(readOnly = true)
  public String getPublicPdeExperienceJson(String productCode) {
    Product product =
        findProductByCode(productCode)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    if (product.getPdeExperienceJson() == null || product.getPdeExperienceJson().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Experiência PDE não publicada para o produto");
    }
    return product.getPdeExperienceJson().trim();
  }

  /** Lê a jornada persuasiva interativa publicada no contrato PDE do produto. */
  @Transactional(readOnly = true)
  public JsonNode getPublicPdePersuasiveJourney(String productCode) {
    Product product =
        findProductByCode(productCode)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    ObjectNode contract = readPdeExperienceContract(product);
    JsonNode journey = contract.get("persuasiveJourney");
    if (journey == null || journey.isNull() || journey.isMissingNode()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Jornada persuasiva PDE não cadastrada");
    }
    return journey;
  }

  /** Valida que o contrato PDE informado é JSON antes de persistir no cadastro comercial. */
  private String validatePdeExperienceJson(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return rawJson;
    }
    try {
      objectMapper.readTree(rawJson);
      return rawJson.trim();
    } catch (JsonProcessingException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Contrato JSON da experiência PDE inválido", ex);
    }
  }

  /** Converte o contrato PDE salvo em objeto JSON editável. */
  private ObjectNode readPdeExperienceContract(Product product) {
    String rawJson = product.getPdeExperienceJson();
    if (rawJson == null || rawJson.isBlank()) {
      return objectMapper.createObjectNode();
    }
    try {
      JsonNode parsed = objectMapper.readTree(rawJson);
      if (!parsed.isObject()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Contrato JSON da experiência PDE deve ser um objeto");
      }
      return (ObjectNode) parsed;
    } catch (JsonProcessingException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Contrato JSON da experiência PDE inválido", ex);
    }
  }

  /** Monta o funil experiencial por estágios para medir a jornada comercial do PDE. */
  private ObjectNode buildDefaultPdePersuasiveJourney(Product product) {
    ObjectNode journey = objectMapper.createObjectNode();
    journey.put("version", "commercial-stages-v1");
    journey.put("framework", "Funil experiencial PDE");
    journey.put("psychologicalModel", "AIDA como apoio, não como eixo principal de leitura");
    journey.put("name", "Jornada Persuasiva Interativa do PDE");
    journey.put(
        "objective",
        "Medir em qual estágio comercial a pessoa ganha ou perde confiança, desejo e disposição de"
            + " pagar.");
    journey.put("productSlug", valueOrFallback(product.getSlug()));
    journey.put("commercialPromise", valueOrFallback(product.getPromise()));
    ArrayNode steps = journey.putArray("steps");
    addPersuasiveJourneyStep(
        steps,
        1,
        "promise_contact",
        "Contato com a promessa",
        "Atenção",
        "Anúncio e primeira dobra apresentam dor, promessa e motivo para clicar/entrar.",
        "A pessoa deixa de ignorar o anúncio e aceita conhecer a promessa do produto.",
        new String[] {"login_hero"},
        new String[] {"PAGE_VIEW", "PAGE_VISIBLE_TIME"},
        "impressões, CTR, CPC, page_view e tempo visível na primeira dobra",
        "Se quebra aqui, revisar promessa, criativo, público, carregamento e clareza do primeiro"
            + " CTA.");
    addPersuasiveJourneyStep(
        steps,
        2,
        "diagnostic_value",
        "Envolvimento diagnóstico",
        "Interesse + Desejo",
        "Questionário e plano de 7 dias transformam curiosidade em valor percebido personalizado.",
        "A pessoa troca passividade por microcompromisso e recebe um plano aplicável à própria"
            + " rotina.",
        new String[] {"interactive_diagnostic", "free_diagnostic_preview"},
        new String[] {"PRESENCE_MAP_CHOICE_SELECTED", "DIAGNOSTIC_CHOICE_SELECTED", "SECTION_VIEW"},
        "início/conclusão do questionário, visualização do plano e tempo no diagnóstico",
        "Se quebra aqui, reduzir fricção das perguntas e tornar a recompensa do plano mais"
            + " concreta.");
    addPersuasiveJourneyStep(
        steps,
        3,
        "continuity_commitment",
        "Compromisso de continuidade",
        "Desejo + Ação",
        "Login, cadastro, salvar plano ou iniciar missão transformam valor percebido em intenção"
            + " real.",
        "A pessoa aceita deixar um sinal de identidade para continuar a jornada.",
        new String[] {"login_panel", "guided_journey"},
        new String[] {"LOGIN_STARTED", "LOGIN_COMPLETED", "FIRST_USE", "MISSION_OPEN"},
        "login iniciado/concluído, plano salvo, primeira missão aberta e primeiro uso",
        "Se quebra aqui, simplificar cadastro, reforçar continuidade do plano e explicar por que"
            + " salvar a jornada.");
    addPersuasiveJourneyStep(
        steps,
        4,
        "commercial_conversion",
        "Conversão comercial",
        "Ação",
        "Paywall, checkout e compra convertem intenção em receita.",
        "A pessoa entende que a parte paga libera a continuidade de maior valor.",
        new String[] {"subscription_paywall"},
        new String[] {
          "PAYWALL_VIEWED", "SUBSCRIPTION_CLICKED", "CHECKOUT_STARTED", "SUBSCRIPTION_APPROVED"
        },
        "paywall visto, clique de assinatura, checkout iniciado e compra aprovada",
        "Se quebra aqui, revisar preço, oferta, garantia, prova, checkout e transição entre plano"
            + " gratuito e acesso pago.");
    addPersuasiveJourneyStep(
        steps,
        5,
        "post_purchase_validation",
        "Validação pós-compra",
        "Retenção",
        "Acesso liberado, uso inicial e missões concluídas confirmam que a promessa vendida está"
            + " sendo aplicada.",
        "A pessoa percebe progresso prático e reduz risco de arrependimento ou abandono.",
        new String[] {"member_journey", "materials_library"},
        new String[] {"ACCESS_RELEASED", "FIRST_USE", "MISSION_COMPLETED", "MATERIAL_OPEN"},
        "acesso liberado, primeiro uso, missão concluída e abertura de materiais",
        "Se quebra aqui, melhorar onboarding, missão do Dia 1, clareza dos materiais e"
            + " acompanhamento inicial.");
    return journey;
  }

  /** Adiciona uma etapa comercial rastreável à jornada persuasiva padrão. */
  private void addPersuasiveJourneyStep(
      ArrayNode steps,
      int stageNumber,
      String stage,
      String stageName,
      String psychologicalRole,
      String commercialFunction,
      String userShift,
      String[] trackedSectionIds,
      String[] eventNames,
      String primaryMetric,
      String optimizationRule) {
    ObjectNode step = steps.addObject();
    step.put("stageNumber", stageNumber);
    step.put("stage", stage);
    step.put("stageName", stageName);
    step.put("psychologicalRole", psychologicalRole);
    step.put("commercialFunction", commercialFunction);
    step.put("userShift", userShift);
    ArrayNode sections = step.putArray("trackedSectionIds");
    for (String trackedSectionId : trackedSectionIds) {
      sections.add(trackedSectionId);
    }
    if (trackedSectionIds.length > 0) {
      step.put("trackedSectionId", trackedSectionIds[0]);
    }
    ArrayNode events = step.putArray("eventNames");
    for (String eventName : eventNames) {
      events.add(eventName);
    }
    step.put("primaryMetric", primaryMetric);
    step.put("optimizationRule", optimizationRule);
  }

  /** Busca o produto por slug público ou por identificador interno numérico. */
  private Optional<Product> findProductByCode(String productCode) {
    if (productCode == null || productCode.isBlank()) {
      return Optional.empty();
    }
    String normalizedCode = productCode.trim();
    Optional<Product> bySlug = repository.findBySlug(normalizedCode);
    if (bySlug.isPresent()) {
      return bySlug;
    }
    if (!normalizedCode.matches("\\d+")) {
      return Optional.empty();
    }
    return repository.findById(Long.valueOf(normalizedCode));
  }

  /** Adiciona o título principal do documento. */
  private void appendTitle(StringBuilder markdown, Product product) {
    markdown
        .append("# Definição de Produto para Mercado — ")
        .append(valueOrFallback(product.getName()))
        .append("\n\n");
    markdown.append(
        "> Documento público de posicionamento comercial do produto. Não inclui detalhes técnicos"
            + " de implementação.\n\n");
  }

  /** Adiciona uma seção com linhas ou parágrafos já formatados. */
  private void appendSection(StringBuilder markdown, String title, String... entries) {
    markdown.append("## ").append(title).append("\n\n");
    for (String entry : entries) {
      if (entry == null || entry.isBlank()) {
        continue;
      }
      markdown.append(entry);
      if (!entry.endsWith("\n")) {
        markdown.append("\n");
      }
    }
    markdown.append("\n");
  }

  /** Formata uma linha de definição de negócio. */
  private String line(String label, String value) {
    return "- **" + label + ":** " + valueOrFallback(value) + "\n";
  }

  /** Formata uma linha somente quando o campo comercial foi cadastrado. */
  private String optionalLine(String label, String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return "- **" + label + ":** " + value.trim() + "\n";
  }

  /** Formata um parágrafo livre preservando um fallback quando não houver dado cadastrado. */
  private String paragraph(String value) {
    return valueOrFallback(value) + "\n";
  }

  /**
   * Resolve o nicho priorizando o relacionamento canônico e usando o campo legado como fallback.
   */
  private String resolveNiche(Product product) {
    if (product.getMarketNiche() != null && product.getMarketNiche().getName() != null) {
      return product.getMarketNiche().getName();
    }
    return product.getNiche();
  }

  /** Formata o preço comercial em reais quando ele estiver cadastrado. */
  private String formatPrice(BigDecimal price) {
    if (price == null) {
      return null;
    }
    return NumberFormat.getCurrencyInstance(BRAZIL).format(price);
  }

  /** Retorna um texto padrão para campos comerciais ainda não definidos. */
  private String valueOrFallback(String value) {
    if (value == null || value.isBlank()) {
      return "Não definido";
    }
    return value.trim();
  }
}
