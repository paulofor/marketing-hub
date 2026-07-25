package com.marketinghub.product.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.dto.ProductVideoProviderAvatarDto;
import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.product.dto.RegisterProductVideoProviderAvatarRequest;
import com.marketinghub.product.mapper.ProductMapper;
import com.marketinghub.product.service.ProductService;
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
  private final ProductMapper mapper;
  private final PdeProductionSlotService pdeProductionSlotService;
  private final ProductMarketingDefinitionHtmlRenderer htmlRenderer =
      new ProductMarketingDefinitionHtmlRenderer();

  /** Inicializa o controller com serviço de produto e mapper de resposta. */
  public ProductController(
      ProductService service,
      ProductMapper mapper,
      PdeProductionSlotService pdeProductionSlotService) {
    this.service = service;
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

  /** Cria ou atualiza uma versão produtiva PDE a partir do cadastro do produto. */
  @PostMapping("/{id}/pde-production-slots")
  public PostDeployPdeProductionSlotDto savePdeProductionSlot(
      @PathVariable Long id, @RequestBody PostDeployPdeProductionSlotRequestDto request) {
    Product product = service.getProduct(id);
    return pdeProductionSlotService.saveProductionSlot(
        product.getSlug(), request.sourceExperimentId(), request);
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
  public ResponseEntity<String> getPublicPdeExperience(@PathVariable String productCode) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(service.getPublicPdeExperienceJson(productCode));
  }

  /** Retorna a jornada persuasiva interativa cadastrada no contrato PDE do produto. */
  @GetMapping(
      value = "/public/{productCode}/pde-persuasive-journey",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode getPublicPdePersuasiveJourney(@PathVariable String productCode) {
    return service.getPublicPdePersuasiveJourney(productCode);
  }
}
