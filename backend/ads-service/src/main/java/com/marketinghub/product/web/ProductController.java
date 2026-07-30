package com.marketinghub.product.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.pde.service.publishslotcontract.PublishPdeProductionSlotContractRequest;
import com.marketinghub.pde.service.versionvideos.PdeProductionSlotVideoPanelDto;
import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.product.dto.ProductScientificArticleDto;
import com.marketinghub.product.dto.ProductVideoProviderAvatarDto;
import com.marketinghub.product.dto.RegisterProductVideoProviderAvatarRequest;
import com.marketinghub.product.dto.SaveProductScientificArticleRequest;
import com.marketinghub.product.mapper.ProductMapper;
import com.marketinghub.product.service.ProductScientificArticleService;
import com.marketinghub.product.service.ProductService;
import com.marketinghub.product.service.adlibrary.ProductAdLibraryResponse;
import com.marketinghub.product.service.experimentcomparison.ProductExperimentComparisonResponse;
import com.marketinghub.product.service.financialsummary.ProductFinancialSummaryResponse;
import com.marketinghub.product.service.organicvideoplan.ProductOrganicVideoPlanResponse;
import com.marketinghub.product.service.updateVideoSeedImage.UpdateProductVideoSeedImageRequest;
import com.marketinghub.product.service.videoimage.GenerateProductVideoImagesRequest;
import com.marketinghub.product.service.videoimage.ProductVideoImageDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor endpoints REST do cadastro comercial de produtos. */
@RestController
@RequestMapping("/api/products")
public class ProductController {
  private final ProductService service;
  private final ProductScientificArticleService scientificArticleService;
  private final ProductMapper mapper;
  private final PdeProductionSlotService pdeProductionSlotService;
  private final ProductMarketingDefinitionHtmlRenderer htmlRenderer =
      new ProductMarketingDefinitionHtmlRenderer();

  /** Inicializa o controller com serviço de produto e mapper de resposta. */
  public ProductController(
      ProductService service,
      ProductScientificArticleService scientificArticleService,
      ProductMapper mapper,
      PdeProductionSlotService pdeProductionSlotService) {
    this.service = service;
    this.scientificArticleService = scientificArticleService;
    this.mapper = mapper;
    this.pdeProductionSlotService = pdeProductionSlotService;
  }

  /** Cadastra um novo produto comercial no Marketing Hub. */
  @PostMapping
  public ProductDto create(@RequestBody CreateProductRequest request) {
    return mapper.toDto(service.createProduct(request));
  }

  /** Retorna um produto comercial pelo identificador interno. */
  @GetMapping("/{id}")
  public ProductDto get(@PathVariable Long id) {
    return mapper.toDto(service.getProduct(id));
  }

  /** Atualiza os dados comerciais editáveis de um produto existente. */
  @PutMapping("/{id}")
  public ProductDto update(@PathVariable Long id, @RequestBody CreateProductRequest request) {
    return mapper.toDto(service.updateProduct(id, request));
  }

  /** Retorna custos, receitas e lucro do produto para análise financeira. */
  @GetMapping("/{id}/financial-summary")
  public ProductFinancialSummaryResponse getFinancialSummary(@PathVariable Long id) {
    return service.getFinancialSummary(id);
  }

  /** Retorna o painel comparativo automático dos experimentos vinculados ao produto. */
  @GetMapping("/{id}/experiment-comparison")
  public ProductExperimentComparisonResponse getExperimentComparison(@PathVariable Long id) {
    return service.getExperimentComparison(id);
  }

  /** Retorna a biblioteca de anúncios reutilizáveis gerados para experimentos do produto. */
  @GetMapping("/{id}/ads")
  public ProductAdLibraryResponse getAdLibrary(@PathVariable Long id) {
    return service.getAdLibrary(id);
  }

  /** Retorna anúncios do produto que estão em uso por um experimento específico. */
  @GetMapping("/experiments/{experimentId}/ads-in-use")
  public ProductAdLibraryResponse getExperimentAdsInUse(@PathVariable Long experimentId) {
    return service.getExperimentAdsInUse(experimentId);
  }

  /** Retorna o playbook de vídeos orgânicos recomendado para o produto. */
  @GetMapping("/{id}/organic-video-plan")
  public ProductOrganicVideoPlanResponse getOrganicVideoPlan(@PathVariable Long id) {
    return service.getOrganicVideoPlan(id);
  }

  /** Lista artigos científicos usados para sustentar o mecanismo do produto. */
  @GetMapping("/{id}/scientific-articles")
  public List<ProductScientificArticleDto> listScientificArticles(@PathVariable Long id) {
    return scientificArticleService.listArticles(id);
  }

  /** Cadastra um artigo científico na base de mecanismo do produto. */
  @PostMapping("/{id}/scientific-articles")
  public ProductScientificArticleDto createScientificArticle(
      @PathVariable Long id, @Valid @RequestBody SaveProductScientificArticleRequest request) {
    return scientificArticleService.createArticle(id, request);
  }

  /** Atualiza um artigo científico cadastrado no produto. */
  @PutMapping("/{id}/scientific-articles/{articleId}")
  public ProductScientificArticleDto updateScientificArticle(
      @PathVariable Long id,
      @PathVariable Long articleId,
      @Valid @RequestBody SaveProductScientificArticleRequest request) {
    return scientificArticleService.updateArticle(id, articleId, request);
  }

  /** Remove um artigo científico da base de mecanismo do produto. */
  @DeleteMapping("/{id}/scientific-articles/{articleId}")
  public ResponseEntity<Void> deleteScientificArticle(
      @PathVariable Long id, @PathVariable Long articleId) {
    scientificArticleService.deleteArticle(id, articleId);
    return ResponseEntity.noContent().build();
  }

  /** Insere a jornada persuasiva interativa padrão no contrato PDE do produto. */
  @PostMapping("/{id}/pde-persuasive-journey/default")
  public ProductDto applyDefaultPdePersuasiveJourney(@PathVariable Long id) {
    return mapper.toDto(service.applyDefaultPdePersuasiveJourney(id));
  }

  /** Aprova, reprova ou deixa pendente a imagem semente usada na produção de vídeos do produto. */
  @PatchMapping("/{id}/video-seed-image")
  public ProductDto updateVideoSeedImage(
      @PathVariable Long id, @RequestBody UpdateProductVideoSeedImageRequest request) {
    return mapper.toDto(service.updateVideoSeedImage(id, request));
  }

  /** Lista imagens geradas exclusivamente para vídeos do produto. */
  @GetMapping("/{id}/video-images")
  public List<ProductVideoImageDto> listVideoImages(@PathVariable Long id) {
    return service.listVideoImages(id);
  }

  /** Lista personagens/avatars de vídeo do produto cadastrados por provider. */
  @GetMapping("/{id}/video-provider-avatars")
  public List<ProductVideoProviderAvatarDto> listVideoProviderAvatars(@PathVariable Long id) {
    return service.listVideoProviderAvatars(id);
  }

  /** Registra ou atualiza um personagem/avatar de vídeo retornado pelo provider. */
  @PostMapping("/{id}/video-provider-avatars")
  public ProductVideoProviderAvatarDto registerVideoProviderAvatar(
      @PathVariable Long id, @RequestBody RegisterProductVideoProviderAvatarRequest request) {
    return service.registerVideoProviderAvatar(id, request);
  }

  /** Gera novas imagens por prompt e vincula à galeria de vídeos do produto. */
  @PostMapping("/{id}/video-images/generations")
  public List<ProductVideoImageDto> generateVideoImages(
      @PathVariable Long id, @Valid @RequestBody GenerateProductVideoImagesRequest request) {
    return service.generateVideoImages(id, request);
  }

  /** Lista as versões produtivas PDE gerenciadas pelo cadastro do produto. */
  @GetMapping("/{id}/pde-production-slots")
  public List<PostDeployPdeProductionSlotDto> listPdeProductionSlots(@PathVariable Long id) {
    Product product = service.getProduct(id);
    return pdeProductionSlotService.listProductionSlotsForProduct(product.getSlug());
  }

  /** Lista os vídeos HLS resolvidos pelo backend para cada versão produtiva PDE do produto. */
  @GetMapping("/{id}/pde-videos")
  public List<PdeProductionSlotVideoPanelDto> listPdeVersionVideos(@PathVariable Long id) {
    Product product = service.getProduct(id);
    return pdeProductionSlotService.listProductionSlotVideosForProduct(product.getSlug());
  }

  /** Cria ou atualiza uma versão produtiva PDE a partir do cadastro do produto. */
  @PostMapping("/{id}/pde-production-slots")
  public PostDeployPdeProductionSlotDto savePdeProductionSlot(
      @PathVariable Long id, @RequestBody PostDeployPdeProductionSlotRequestDto request) {
    Product product = service.getProduct(id);
    return pdeProductionSlotService.saveProductionSlot(
        product.getSlug(), request.sourceExperimentId(), request);
  }

  /** Valida se uma versão produtiva PDE realmente entrega a URL e o contrato público. */
  @PostMapping("/{id}/pde-production-slots/{slotCode}/validate")
  public PostDeployPdeProductionSlotDto validatePdeProductionSlot(
      @PathVariable Long id, @PathVariable String slotCode) {
    Product product = service.getProduct(id);
    return pdeProductionSlotService.validateProductionSlot(product.getSlug(), slotCode);
  }

  /** Publica o contrato comercial editável de uma versão produtiva PDE. */
  @PostMapping("/{id}/pde-production-slots/{slotCode}/publish")
  public PostDeployPdeProductionSlotDto publishPdeProductionSlotContract(
      @PathVariable Long id,
      @PathVariable String slotCode,
      @RequestBody PublishPdeProductionSlotContractRequest request) {
    Product product = service.getProduct(id);
    return pdeProductionSlotService.publishProductionSlotContract(
        product.getSlug(), slotCode, request);
  }

  /** Lista os produtos comerciais cadastrados no Marketing Hub. */
  @GetMapping
  public List<ProductDto> list() {
    return StreamSupport.stream(service.listProducts().spliterator(), false)
        .map(mapper::toDto)
        .toList();
  }

  /** Retorna a definição pública de mercado do produto em Markdown. */
  @GetMapping(
      value = "/public/{productCode}/marketing-definition.md",
      produces = "text/markdown;charset=UTF-8")
  public ResponseEntity<String> getPublicMarketingDefinitionMarkdown(
      @PathVariable String productCode) {
    String filename = "produto-" + productCode + "-definicao-mercado.md";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
        .body(service.buildPublicMarketingDefinitionMarkdown(productCode));
  }

  /** Retorna a definição pública de mercado do produto como página HTML formatada. */
  @GetMapping(
      value = "/public/{productCode}/marketing-definition",
      produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
  public ResponseEntity<String> getPublicMarketingDefinitionHtml(@PathVariable String productCode) {
    String markdown = service.buildPublicMarketingDefinitionMarkdown(productCode);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
        .body(htmlRenderer.render(markdown));
  }

  /** Retorna o contrato JSON público da experiência PDE publicada pelo Marketing Hub. */
  @GetMapping(
      value = "/public/{productCode}/pde-experience",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getPublicPdeExperience(
      @PathVariable String productCode,
      @RequestParam(required = false) String slotCode,
      @RequestParam(required = false) String experienceVersion) {
    String body =
        pdeProductionSlotService
            .findPublishedExperienceJson(productCode, slotCode, experienceVersion)
            .orElseGet(() -> service.getPublicPdeExperienceJson(productCode));
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }

  /** Retorna a jornada persuasiva interativa cadastrada no contrato PDE do produto. */
  @GetMapping(
      value = "/public/{productCode}/pde-persuasive-journey",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode getPublicPdePersuasiveJourney(@PathVariable String productCode) {
    return service.getPublicPdePersuasiveJourney(productCode);
  }
}
