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
import com.marketinghub.product.ProductVideoSeedImageReviewStatus;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.service.updateVideoSeedImage.UpdateProductVideoSeedImageRequest;
import com.marketinghub.product.service.videoimage.GenerateProductVideoImagesRequest;
import com.marketinghub.product.service.videoimage.ProductVideoImageDto;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.product.ProductVideoImageRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import com.marketinghub.storage.StorageException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerenciar o cadastro comercial de produtos digitais. */
@Service
public class ProductService {
  private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

  private final ProductRepository repository;
  private final InstagramAccountRepository accountRepository;
  private final MarketNicheRepository marketNicheRepository;
  private final AssetRepository assetRepository;
  private final ProductVideoImageRepository productVideoImageRepository;
  private final ImageGeneratorService imageGeneratorService;
  private final AssetStorageService assetStorageService;
  private final ObjectMapper objectMapper;

  /** Inicializa o serviço com os repositórios necessários para cadastro de produtos. */
  public ProductService(
      ProductRepository repository,
      InstagramAccountRepository accountRepository,
      MarketNicheRepository marketNicheRepository,
      AssetRepository assetRepository,
      ProductVideoImageRepository productVideoImageRepository,
      ImageGeneratorService imageGeneratorService,
      AssetStorageService assetStorageService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.accountRepository = accountRepository;
    this.marketNicheRepository = marketNicheRepository;
    this.assetRepository = assetRepository;
    this.productVideoImageRepository = productVideoImageRepository;
    this.imageGeneratorService = imageGeneratorService;
    this.assetStorageService = assetStorageService;
    this.objectMapper = objectMapper;
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
