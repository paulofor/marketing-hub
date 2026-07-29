package com.marketinghub.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.imagegenerator.service.ImageGeneratorService;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.product.Product;
import com.marketinghub.product.ProductVideoImage;
import com.marketinghub.product.ProductVideoProviderAvatar;
import com.marketinghub.product.ProductVideoSeedImageReviewStatus;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.dto.RegisterProductVideoProviderAvatarRequest;
import com.marketinghub.product.service.updateVideoSeedImage.UpdateProductVideoSeedImageRequest;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.product.ProductVideoImageRepository;
import com.marketinghub.repository.jpa.product.ProductVideoProviderAvatarRepository;
import com.marketinghub.storage.AssetStorageService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar as regras de cadastro comercial de produtos. */
class ProductServiceTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Cria o serviço com dependências auxiliares mockadas para testes focados em produto. */
  private ProductService newService(
      ProductRepository productRepository,
      InstagramAccountRepository accountRepository,
      MarketNicheRepository marketNicheRepository,
      AssetRepository assetRepository) {
    return newService(
        productRepository,
        accountRepository,
        marketNicheRepository,
        assetRepository,
        mock(ProductVideoImageRepository.class),
        mock(ProductVideoProviderAvatarRepository.class));
  }

  /** Cria o serviço com repositório de galeria informado para validar revisão de imagem. */
  private ProductService newService(
      ProductRepository productRepository,
      InstagramAccountRepository accountRepository,
      MarketNicheRepository marketNicheRepository,
      AssetRepository assetRepository,
      ProductVideoImageRepository productVideoImageRepository) {
    return newService(
        productRepository,
        accountRepository,
        marketNicheRepository,
        assetRepository,
        productVideoImageRepository,
        mock(ProductVideoProviderAvatarRepository.class));
  }

  /** Cria o serviço com repositórios de vídeo informados para validar avatar por provider. */
  private ProductService newService(
      ProductRepository productRepository,
      InstagramAccountRepository accountRepository,
      MarketNicheRepository marketNicheRepository,
      AssetRepository assetRepository,
      ProductVideoImageRepository productVideoImageRepository,
      ProductVideoProviderAvatarRepository productVideoProviderAvatarRepository) {
    return new ProductService(
        productRepository,
        accountRepository,
        marketNicheRepository,
        assetRepository,
        productVideoImageRepository,
        productVideoProviderAvatarRepository,
        mock(ImageGeneratorService.class),
        mock(AssetStorageService.class),
        objectMapper,
        mock(JdbcTemplate.class));
  }

  /** Deve persistir alterações comerciais em um produto já existente. */
  @Test
  void updateProduct() {
    ProductRepository productRepository = mock(ProductRepository.class);
    InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    ProductService service =
        newService(
            productRepository,
            accountRepository,
            marketNicheRepository,
            mock(AssetRepository.class));
    Product product = Product.builder().id(1L).name("Nome antigo").build();
    MarketNiche niche = new MarketNiche();
    CreateProductRequest request = new CreateProductRequest();
    request.setName("Método MUSA - Presença Elegante em 7 Dias");
    request.setSlug("metodo-musa-7-dias");
    request.setLogoUrl("https://clubemusa.com.br/assets/logo-musa.svg");
    request.setMarketNicheId(10L);
    request.setCurrentPriceBrl(new BigDecimal("67.00"));
    request.setTargetAudience("Mulheres urbanas");
    request.setScientificEvidencePack("Evidence Pack MUSA v1");
    request.setPdeExperienceJson("{\"slug\":\"metodo-musa-7-dias\",\"missions\":[]}");
    request.setSevenDayJourney("Dia 1: diagnóstico; Dia 2: limpeza visual.");
    request.setSupportMaterialPositioning("Material de apoio como reforço secundário.");
    request.setPrimaryCta("Ver meu plano MUSA de 7 dias");

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(marketNicheRepository.findById(10L)).thenReturn(Optional.of(niche));
    when(productRepository.save(product)).thenReturn(product);

    Product updated = service.updateProduct(1L, request);

    assertThat(updated.getName()).isEqualTo(request.getName());
    assertThat(updated.getSlug()).isEqualTo(request.getSlug());
    assertThat(updated.getLogoUrl()).isEqualTo("https://clubemusa.com.br/assets/logo-musa.svg");
    assertThat(updated.getCurrentPriceBrl()).isEqualByComparingTo("67.00");
    assertThat(updated.getTargetAudience()).isEqualTo(request.getTargetAudience());
    assertThat(updated.getScientificEvidencePack()).isEqualTo("Evidence Pack MUSA v1");
    assertThat(updated.getPdeExperienceJson()).contains("\"metodo-musa-7-dias\"");
    assertThat(updated.getSevenDayJourney())
        .isEqualTo("Dia 1: diagnóstico; Dia 2: limpeza visual.");
    assertThat(updated.getSupportMaterialPositioning())
        .isEqualTo("Material de apoio como reforço secundário.");
    assertThat(updated.getPrimaryCta()).isEqualTo("Ver meu plano MUSA de 7 dias");
    assertThat(updated.getMarketNiche()).isSameAs(niche);
  }

  /** Deve incluir VPS ativa como custo fixo mensal no financeiro do produto PDE. */
  @Test
  void getFinancialSummaryIncludesPdeInfrastructureCost() {
    ProductRepository productRepository = mock(ProductRepository.class);
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    ProductService service =
        new ProductService(
            productRepository,
            mock(InstagramAccountRepository.class),
            mock(MarketNicheRepository.class),
            mock(AssetRepository.class),
            mock(ProductVideoImageRepository.class),
            mock(ProductVideoProviderAvatarRepository.class),
            mock(ImageGeneratorService.class),
            mock(AssetStorageService.class),
            objectMapper,
            jdbcTemplate);
    Product product =
        Product.builder().id(1L).name("Método MUSA").slug("metodo-musa-7-dias").build();

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(jdbcTemplate.queryForObject(
            org.mockito.ArgumentMatchers.contains("FROM pde_vps_server"),
            org.mockito.ArgumentMatchers.eq(BigDecimal.class),
            org.mockito.ArgumentMatchers.eq("metodo-musa-7-dias")))
        .thenReturn(new BigDecimal("49.90"));

    var response = service.getFinancialSummary(1L);

    assertThat(response.costs())
        .anySatisfy(
            cost -> {
              assertThat(cost.type()).isEqualTo("PDE_INFRASTRUCTURE");
              assertThat(cost.label()).isEqualTo("Infraestrutura VPS PDE");
              assertThat(cost.monthly().brl()).isEqualByComparingTo("49.90");
              assertThat(cost.annual().brl()).isEqualByComparingTo("598.80");
            });
  }

  /** Deve aprovar a imagem semente de vídeo do produto com nome de personagem. */
  @Test
  void updateVideoSeedImageApprovesReadyImageAsset() {
    ProductRepository productRepository = mock(ProductRepository.class);
    InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    AssetRepository assetRepository = mock(AssetRepository.class);
    ProductVideoImageRepository productVideoImageRepository =
        mock(ProductVideoImageRepository.class);
    ProductService service =
        newService(
            productRepository,
            accountRepository,
            marketNicheRepository,
            assetRepository,
            productVideoImageRepository);
    Product product = Product.builder().id(1L).name("Método MUSA").build();
    Asset asset =
        Asset.builder()
            .id(99L)
            .type(AssetType.IMAGE)
            .status(AssetStatus.READY)
            .url("/uploads/musa-seed.png")
            .build();
    ProductVideoImage galleryImage =
        ProductVideoImage.builder()
            .id(7L)
            .product(product)
            .asset(asset)
            .reviewStatus(ProductVideoSeedImageReviewStatus.PENDING)
            .build();

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(assetRepository.findById(99L)).thenReturn(Optional.of(asset));
    when(productVideoImageRepository.findFirstByProductIdAndAssetId(1L, 99L))
        .thenReturn(Optional.of(galleryImage));
    when(productRepository.save(product)).thenReturn(product);

    Product updated =
        service.updateVideoSeedImage(
            1L,
            new UpdateProductVideoSeedImageRequest(
                99L,
                "Sofia MUSA",
                ProductVideoSeedImageReviewStatus.APPROVED,
                "Aprovada como imagem-mestre.",
                "marketing@hub.local"));

    assertThat(updated.getVideoSeedImageAsset()).isSameAs(asset);
    assertThat(updated.getVideoSeedCharacterName()).isEqualTo("Sofia MUSA");
    assertThat(updated.getVideoSeedReviewStatus())
        .isEqualTo(ProductVideoSeedImageReviewStatus.APPROVED);
    assertThat(updated.getVideoSeedReviewNotes()).isEqualTo("Aprovada como imagem-mestre.");
    assertThat(updated.getVideoSeedReviewedBy()).isEqualTo("marketing@hub.local");
    assertThat(updated.getVideoSeedReviewedAt()).isNotNull();
    assertThat(galleryImage.getReviewStatus())
        .isEqualTo(ProductVideoSeedImageReviewStatus.APPROVED);
    assertThat(galleryImage.getReviewNotes()).isEqualTo("Aprovada como imagem-mestre.");
    verify(productVideoImageRepository).save(galleryImage);
  }

  /** Deve bloquear asset que não é imagem para impedir semente inválida de vídeo. */
  @Test
  void updateVideoSeedImageRejectsNonImageAsset() {
    ProductRepository productRepository = mock(ProductRepository.class);
    InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    AssetRepository assetRepository = mock(AssetRepository.class);
    ProductService service =
        newService(productRepository, accountRepository, marketNicheRepository, assetRepository);
    Product product = Product.builder().id(1L).build();
    Asset asset = Asset.builder().id(99L).type(AssetType.VIDEO).status(AssetStatus.READY).build();

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(assetRepository.findById(99L)).thenReturn(Optional.of(asset));

    assertThatThrownBy(
            () ->
                service.updateVideoSeedImage(
                    1L,
                    new UpdateProductVideoSeedImageRequest(
                        99L, "Sofia MUSA", ProductVideoSeedImageReviewStatus.APPROVED, null, null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("asset do tipo IMAGE");
  }

  /** Deve registrar avatar HeyGen reutilizável a partir da imagem aprovada do produto. */
  @Test
  void registerVideoProviderAvatarStoresReusableProviderIds() {
    ProductRepository productRepository = mock(ProductRepository.class);
    AssetRepository assetRepository = mock(AssetRepository.class);
    ProductVideoProviderAvatarRepository avatarRepository =
        mock(ProductVideoProviderAvatarRepository.class);
    ProductService service =
        newService(
            productRepository,
            mock(InstagramAccountRepository.class),
            mock(MarketNicheRepository.class),
            assetRepository,
            mock(ProductVideoImageRepository.class),
            avatarRepository);
    Product product = Product.builder().id(4L).name("Método MUSA").build();
    Asset asset =
        Asset.builder()
            .id(1927L)
            .type(AssetType.IMAGE)
            .status(AssetStatus.READY)
            .url("https://cdn.example/musa.png")
            .build();

    when(productRepository.findById(4L)).thenReturn(Optional.of(product));
    when(assetRepository.findById(1927L)).thenReturn(Optional.of(asset));
    when(avatarRepository.findFirstByProductIdAndProviderIgnoreCaseAndSourceAssetId(
            4L, "HEYGEN", 1927L))
        .thenReturn(Optional.empty());
    when(avatarRepository.save(org.mockito.ArgumentMatchers.any(ProductVideoProviderAvatar.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        service.registerVideoProviderAvatar(
            4L,
            new RegisterProductVideoProviderAvatarRequest(
                1927L,
                "HEYGEN",
                "Sofia MUSA",
                "281a1e5b526841b0865ea466dfb33ab9",
                "3952e73a14d94871b8130274e27287ee",
                "processing",
                null,
                true,
                "Avatar criado por API HeyGen."));

    assertThat(response.productId()).isEqualTo(4L);
    assertThat(response.sourceAssetId()).isEqualTo(1927L);
    assertThat(response.provider()).isEqualTo("HEYGEN");
    assertThat(response.providerAvatarId()).isEqualTo("281a1e5b526841b0865ea466dfb33ab9");
    assertThat(response.providerAvatarGroupId()).isEqualTo("3952e73a14d94871b8130274e27287ee");
    assertThat(response.sourceImageUrl()).isEqualTo("https://cdn.example/musa.png");
    assertThat(response.supportsReusableAvatar()).isTrue();
  }

  /** Deve montar uma definição pública em Markdown com foco comercial e sem detalhes técnicos. */
  @Test
  void buildPublicMarketingDefinitionMarkdown() {
    ProductRepository productRepository = mock(ProductRepository.class);
    InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    ProductService service =
        newService(
            productRepository,
            accountRepository,
            marketNicheRepository,
            mock(AssetRepository.class));
    MarketNiche niche = MarketNiche.builder().name("Elegância feminina prática").build();
    Product product =
        Product.builder()
            .id(1L)
            .slug("metodo-musa-7-dias")
            .name("Método MUSA - Presença Elegante em 7 Dias")
            .logoUrl("https://clubemusa.com.br/assets/logo-musa.svg")
            .productType("PDE")
            .commercialStatus("validação comercial")
            .currentPriceBrl(new BigDecimal("67.00"))
            .marketNiche(niche)
            .targetAudience(
                "Mulheres que querem parecer elegantes sem trocar o guarda-roupa inteiro")
            .primaryHypothesis(
                "Mulheres desejam presença elegante com baixo esforço e baixo gasto.")
            .explicitPain("Sente que a aparência não comunica o valor pessoal.")
            .promise("Parecer mais elegante em 7 dias.")
            .uniqueMechanism(
                "Curadoria guiada de presença visual com base em Adam e Galinsky (2012).")
            .languageStyle("Sofisticada, prática e acolhedora.")
            .colorPalette(
                "1. Vinho MUSA #7A2444; 2. Dourado #D6A75C; 3. Creme #FFF8F3; 4. Grafite #2F2A2C;"
                    + " 5. Blush #F3C9C1; 6. Oliva #6F7A52; 7. Champanhe #F7E4C6.")
            .tripwire(
                "Experiência guiada de 7 dias com diagnóstico, missões, checklists e templates.")
            .sevenDayJourney(
                "- **Dia 1 — Diagnóstico de presença:** identificar ruído visual.\n"
                    + "- **Dia 2 — Limpeza de ruído visual:** remover excessos sem comprar nada"
                    + " novo.")
            .supportMaterialPositioning(
                "Material de apoio aparece como reforço secundário da jornada.")
            .primaryCta("Ver meu plano MUSA de 7 dias")
            .socialProof("Prova científica, prova visual e experimento 66.")
            .scientificEvidencePack(
                "Evidence Pack MUSA v1: uso de IA associado aos artigos científicos citados,"
                    + " princípios permitidos, linguagem permitida, afirmações proibidas e"
                    + " referências científicas.")
            .funnel("Anúncio → login → experiência gratuita → paywall → compra.")
            .codeModules("pde-platform, backend")
            .aiCost(new BigDecimal("1.20"))
            .build();

    when(productRepository.findBySlug("metodo-musa-7-dias")).thenReturn(Optional.of(product));

    String markdown = service.buildPublicMarketingDefinitionMarkdown("metodo-musa-7-dias");

    assertThat(markdown)
        .contains(
            "# Definição de Produto para Mercado — Método MUSA - Presença Elegante em 7 Dias");
    assertThat(markdown).contains("## 2. Mercado e nicho");
    assertThat(markdown).contains("Elegância feminina prática");
    assertThat(markdown).contains("## 4. Dor, resultado e mecanismo");
    assertThat(markdown).contains("Resultado prometido");
    assertThat(markdown).contains("Logo");
    assertThat(markdown).contains("https://clubemusa.com.br/assets/logo-musa.svg");
    assertThat(markdown).contains("Parecer mais elegante em 7 dias.");
    assertThat(markdown).contains("Adam e Galinsky (2012)");
    assertThat(markdown).contains("Paleta visual completa");
    assertThat(markdown).contains("7. Champanhe #F7E4C6");
    assertThat(markdown).contains("Experiência guiada de 7 dias");
    assertThat(markdown).contains("Material de apoio aparece como reforço secundário da jornada.");
    assertThat(markdown).contains("CTA principal recomendado");
    assertThat(markdown).contains("Ver meu plano MUSA de 7 dias");
    assertThat(markdown).contains("## 7. Jornada de 7 dias");
    assertThat(markdown).contains("Dia 1 — Diagnóstico de presença");
    assertThat(markdown).contains("Dia 2 — Limpeza de ruído visual");
    assertThat(markdown).contains("## 8. Funil de aquisição e venda");
    assertThat(markdown).contains("Prova científica");
    assertThat(markdown).contains("Base científica operacional");
    assertThat(markdown).contains("uso de IA associado aos artigos científicos citados");
    assertThat(markdown).contains("afirmações proibidas");
    assertThat(markdown).doesNotContain("pde-platform");
    assertThat(markdown).doesNotContain("1.20");
  }

  /** Deve aceitar o identificador interno como fallback quando o código for numérico. */
  @Test
  void buildPublicMarketingDefinitionMarkdownByNumericCode() {
    ProductRepository productRepository = mock(ProductRepository.class);
    InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    ProductService service =
        newService(
            productRepository,
            accountRepository,
            marketNicheRepository,
            mock(AssetRepository.class));
    Product product = Product.builder().id(7L).name("Produto 7").build();

    when(productRepository.findBySlug("7")).thenReturn(Optional.empty());
    when(productRepository.findById(7L)).thenReturn(Optional.of(product));

    String markdown = service.buildPublicMarketingDefinitionMarkdown("7");

    assertThat(markdown).contains("Produto 7");
  }

  /** Deve retornar erro controlado quando o produto não existir. */
  @Test
  void buildPublicMarketingDefinitionMarkdownNotFound() {
    ProductRepository productRepository = mock(ProductRepository.class);
    InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    ProductService service =
        newService(
            productRepository,
            accountRepository,
            marketNicheRepository,
            mock(AssetRepository.class));

    when(productRepository.findBySlug("inexistente")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buildPublicMarketingDefinitionMarkdown("inexistente"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Produto não encontrado");
  }

  /** Deve expor o contrato JSON de experiência PDE salvo no cadastro do produto. */
  @Test
  void getPublicPdeExperienceJson() {
    ProductRepository productRepository = mock(ProductRepository.class);
    InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    ProductService service =
        newService(
            productRepository,
            accountRepository,
            marketNicheRepository,
            mock(AssetRepository.class));
    Product product =
        Product.builder()
            .slug("metodo-musa-7-dias")
            .pdeExperienceJson("{\"slug\":\"metodo-musa-7-dias\"}")
            .build();

    when(productRepository.findBySlug("metodo-musa-7-dias")).thenReturn(Optional.of(product));

    String json = service.getPublicPdeExperienceJson("metodo-musa-7-dias");

    assertThat(json).isEqualTo("{\"slug\":\"metodo-musa-7-dias\"}");
  }

  /**
   * Deve inserir a jornada persuasiva por estágios comerciais no contrato PDE preservando dados
   * existentes.
   */
  @Test
  void applyDefaultPdePersuasiveJourney() {
    ProductRepository productRepository = mock(ProductRepository.class);
    InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    ProductService service =
        newService(
            productRepository,
            accountRepository,
            marketNicheRepository,
            mock(AssetRepository.class));
    Product product =
        Product.builder()
            .id(1L)
            .slug("metodo-musa-7-dias")
            .promise("Presença elegante em 7 dias")
            .pdeExperienceJson(
                "{\"slug\":\"metodo-musa-7-dias\",\"experienceVersion\":\"musa-pde-entry-v4-video-hero\"}")
            .build();

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.save(product)).thenReturn(product);

    Product updated = service.applyDefaultPdePersuasiveJourney(1L);

    assertThat(updated.getPdeExperienceJson())
        .contains("\"experienceVersion\" : \"musa-pde-entry-v4-video-hero\"");
    assertThat(updated.getPdeExperienceJson()).contains("\"persuasiveJourney\"");
    assertThat(updated.getPdeExperienceJson())
        .contains("\"framework\" : \"Funil experiencial PDE\"");
    assertThat(updated.getPdeExperienceJson())
        .contains("\"stageName\" : \"Envolvimento diagnóstico\"");
    assertThat(updated.getPdeExperienceJson()).contains("\"stageName\" : \"Validação pós-compra\"");
    assertThat(updated.getPdeExperienceJson())
        .contains(
            "\"trackedSectionIds\" : [ \"interactive_diagnostic\", \"free_diagnostic_preview\" ]");
    assertThat(updated.getPdeExperienceJson())
        .contains("\"trackedSectionId\" : \"subscription_paywall\"");
  }

  /** Deve ler a jornada persuasiva publicada no contrato PDE do produto. */
  @Test
  void getPublicPdePersuasiveJourney() {
    ProductRepository productRepository = mock(ProductRepository.class);
    InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    ProductService service =
        newService(
            productRepository,
            accountRepository,
            marketNicheRepository,
            mock(AssetRepository.class));
    Product product =
        Product.builder()
            .slug("metodo-musa-7-dias")
            .pdeExperienceJson(
                "{\"persuasiveJourney\":{\"framework\":\"Funil experiencial PDE\",\"steps\":[]}}")
            .build();

    when(productRepository.findBySlug("metodo-musa-7-dias")).thenReturn(Optional.of(product));

    var journey = service.getPublicPdePersuasiveJourney("metodo-musa-7-dias");

    assertThat(journey.get("framework").asText()).isEqualTo("Funil experiencial PDE");
    assertThat(journey.get("steps").isArray()).isTrue();
  }

  /** Deve rejeitar contrato PDE que não seja JSON válido antes de salvar o produto. */
  @Test
  void updateProductRejectsInvalidPdeExperienceJson() {
    ProductRepository productRepository = mock(ProductRepository.class);
    InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    ProductService service =
        newService(
            productRepository,
            accountRepository,
            marketNicheRepository,
            mock(AssetRepository.class));
    Product product = Product.builder().id(1L).build();
    MarketNiche niche = new MarketNiche();
    CreateProductRequest request = new CreateProductRequest();
    request.setMarketNicheId(10L);
    request.setPdeExperienceJson("{json inválido");

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(marketNicheRepository.findById(10L)).thenReturn(Optional.of(niche));

    assertThatThrownBy(() -> service.updateProduct(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Contrato JSON da experiência PDE inválido");
  }
}
