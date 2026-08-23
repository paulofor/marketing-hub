package com.marketinghub.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.producttype.ProductTypeStatus;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.product.ProductVideoImageRepository;
import com.marketinghub.repository.jpa.product.ProductVideoProviderAvatarRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import com.marketinghub.storage.AssetStorageService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar as regras de cadastro comercial de produtos. */
class ProductServiceTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Cria o serviço com catálogo de tipos controlado pelo cenário. */
  private ProductService newService(
      ProductRepository productRepository,
      ProductTypeDefinitionRepository productTypeDefinitionRepository) {
    return newService(
        productRepository, productTypeDefinitionRepository, mock(MarketNicheRepository.class));
  }

  /** Cria o serviço com catálogos de tipo e nicho controlados pelo cenário. */
  private ProductService newService(
      ProductRepository productRepository,
      ProductTypeDefinitionRepository productTypeDefinitionRepository,
      MarketNicheRepository marketNicheRepository) {
    return new ProductService(
        productRepository,
        mock(InstagramAccountRepository.class),
        marketNicheRepository,
        mock(AssetRepository.class),
        mock(ProductVideoImageRepository.class),
        mock(ProductVideoProviderAvatarRepository.class),
        productTypeDefinitionRepository,
        mock(ImageGeneratorService.class),
        mock(AssetStorageService.class),
        objectMapper,
        mock(JdbcTemplate.class));
  }

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

  /** Deve separar o filtro dinâmico do ORDER BY ao consultar a biblioteca de anúncios. */
  @Test
  void getAdLibraryBuildsValidSqlAfterDynamicScope() {
    ProductRepository productRepository = mock(ProductRepository.class);
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    MarketNiche niche = MarketNiche.builder().id(21L).build();
    Product product =
        Product.builder()
            .id(7L)
            .name("Agenda Cheia")
            .slug("agenda-cheia")
            .marketNiche(niche)
            .build();
    when(productRepository.findById(7L)).thenReturn(Optional.of(product));
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              String sql = invocation.getArgument(0);
              assertThat(sql).contains("e.niche_id = ?\nORDER BY");
              assertThat(sql)
                  .contains("c.source_creative_id AS source_creative_id")
                  .contains("c.version_number AS version_number")
                  .contains("END AS final_candidate");
              assertThat(sql).doesNotContain("?ORDER BY");
              return java.util.List.of();
            });
    ProductService service =
        new ProductService(
            productRepository,
            mock(InstagramAccountRepository.class),
            mock(MarketNicheRepository.class),
            mock(AssetRepository.class),
            mock(ProductVideoImageRepository.class),
            mock(ProductVideoProviderAvatarRepository.class),
            mock(ProductTypeDefinitionRepository.class),
            mock(ImageGeneratorService.class),
            mock(AssetStorageService.class),
            objectMapper,
            jdbcTemplate);

    var response = service.getAdLibrary(7L);

    assertThat(response.ads()).isEmpty();
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
        mock(ProductTypeDefinitionRepository.class),
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
    request.setInternalName("MUSA desejo v7");
    request.setAliases(
        List.of(
            " MUSA v7 ",
            "músa v7",
            "Vídeos orientados ao desejo",
            "Método MUSA - Presença Elegante em 7 Dias"));
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
    request.setProductFormat("GUIDED_PROGRAM");
    request.setDeliveryMode("HYBRID");
    request.setRevenueModel("ONE_TIME_PURCHASE");
    request.setValueUnit("7 dias concluídos");
    request.setValueEvidenceMetric("SATISFACTION");
    request.setValidationDefinitionVersion("v1");
    request.setValidationDefinitionJson(
        """
        {"problem":{},"promise":{},"mechanism":{},"format":{},"delivery":{},
         "economics":{},"successEvidence":{},"decisionRules":{}}
        """);
    request.setDesireAssociationMapVersion("v1");
    request.setDesireAssociationMapJson(
        """
        {"painState":"improviso","desiredState":"orgulho","territories":[
          {"code":"PROFESSIONAL_PRIDE","name":"Orgulho profissional","idea":"Perfil à altura do talento",
           "symbols":["perfil organizado"],"truthBoundary":"Não garantir agenda lotada"}],
         "causalChain":["ativos","presença"],"evidence":{"currentLevel":"HYPOTHESIS"},
         "prohibitedAssociations":["renda garantida"],"measurementPlan":{"funnel":["clique","venda"]}}
        """);

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(marketNicheRepository.findById(10L)).thenReturn(Optional.of(niche));
    when(productRepository.save(product)).thenReturn(product);

    Product updated = service.updateProduct(1L, request);

    assertThat(updated.getName()).isEqualTo(request.getName());
    assertThat(updated.getInternalName()).isEqualTo("MUSA desejo v7");
    assertThat(updated.getAliases()).containsExactly("MUSA v7", "Vídeos orientados ao desejo");
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
    assertThat(updated.getProductFormat()).isEqualTo("GUIDED_PROGRAM");
    assertThat(updated.getDeliveryMode()).isEqualTo("HYBRID");
    assertThat(updated.getRevenueModel()).isEqualTo("ONE_TIME_PURCHASE");
    assertThat(updated.getValueUnit()).isEqualTo("7 dias concluídos");
    assertThat(updated.getValueEvidenceMetric()).isEqualTo("SATISFACTION");
    assertThat(updated.getValidationDefinitionVersion()).isEqualTo("v1");
    assertThat(updated.getValidationDefinitionJson()).contains("decisionRules");
    assertThat(updated.getDesireAssociationMapVersion()).isEqualTo("v1");
    assertThat(updated.getDesireAssociationMapJson()).contains("PROFESSIONAL_PRIDE");
    assertThat(updated.getMarketNiche()).isSameAs(niche);
  }

  /** Deve vincular o produto à identidade estável do tipo selecionado na tela. */
  @Test
  void updateProductAssignsCatalogType() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeDefinitionRepository typeRepository = mock(ProductTypeDefinitionRepository.class);
    MarketNicheRepository nicheRepository = mock(MarketNicheRepository.class);
    ProductService service = newService(productRepository, typeRepository, nicheRepository);
    Product product = Product.builder().id(9L).name("Kit WhatsApp Pronto").build();
    ProductTypeDefinition type =
        ProductTypeDefinition.builder()
            .id(5L)
            .code("PDE")
            .name("PDE - Produto Digital Experiencial")
            .aliases(Set.of("Produto Digital Experiencial"))
            .status(ProductTypeStatus.ACTIVE)
            .build();
    CreateProductRequest request = new CreateProductRequest();
    request.setName(product.getName());
    request.setProductTypeId(5L);
    request.setMarketNicheId(10L);
    MarketNiche niche = new MarketNiche();
    when(productRepository.findById(9L)).thenReturn(Optional.of(product));
    when(typeRepository.findById(5L)).thenReturn(Optional.of(type));
    when(nicheRepository.findById(10L)).thenReturn(Optional.of(niche));
    when(productRepository.save(product)).thenReturn(product);

    Product updated = service.updateProduct(9L, request);

    assertThat(updated.getProductTypeDefinition()).isSameAs(type);
    assertThat(updated.getProductType()).isEqualTo("PDE - Produto Digital Experiencial");
  }

  /** Deve aceitar um apelido cadastrado enviado por uma integração legada. */
  @Test
  void updateProductResolvesLegacyTypeAlias() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeDefinitionRepository typeRepository = mock(ProductTypeDefinitionRepository.class);
    MarketNicheRepository nicheRepository = mock(MarketNicheRepository.class);
    ProductService service = newService(productRepository, typeRepository, nicheRepository);
    Product product = Product.builder().id(8L).name("Especialista no WhatsApp").build();
    ProductTypeDefinition type =
        ProductTypeDefinition.builder()
            .id(6L)
            .code("AI_SANDBOX_CONVERSATIONAL_PRODUCT")
            .name("Produto IA de atendimento personalizado por sandbox")
            .aliases(Set.of("PDE - Consultor Especialista por WhatsApp", "Consultor por WhatsApp"))
            .status(ProductTypeStatus.ACTIVE)
            .build();
    CreateProductRequest request = new CreateProductRequest();
    request.setName(product.getName());
    request.setProductType("consultor por whatsapp");
    request.setMarketNicheId(10L);
    MarketNiche niche = new MarketNiche();
    when(productRepository.findById(8L)).thenReturn(Optional.of(product));
    when(typeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(type));
    when(nicheRepository.findById(10L)).thenReturn(Optional.of(niche));
    when(productRepository.save(product)).thenReturn(product);

    assertThat(service.updateProduct(8L, request).getProductTypeDefinition()).isSameAs(type);
  }

  /** Deve impedir novo vínculo com um tipo já aposentado. */
  @Test
  void updateProductRejectsRetiredType() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeDefinitionRepository typeRepository = mock(ProductTypeDefinitionRepository.class);
    ProductService service = newService(productRepository, typeRepository);
    Product product = Product.builder().id(7L).name("Agenda Cheia").build();
    ProductTypeDefinition retired =
        ProductTypeDefinition.builder()
            .id(7L)
            .code("OLD")
            .name("Tipo antigo")
            .aliases(Set.of())
            .status(ProductTypeStatus.RETIRED)
            .build();
    CreateProductRequest request = new CreateProductRequest();
    request.setName(product.getName());
    request.setProductTypeId(7L);
    when(productRepository.findById(7L)).thenReturn(Optional.of(product));
    when(typeRepository.findById(7L)).thenReturn(Optional.of(retired));

    assertThatThrownBy(() -> service.updateProduct(7L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Somente tipos em uso");
  }

  /** Deve impedir que um produto novo nasça sem classificação comercial auditável. */
  @Test
  void createProductRejectsMissingCatalogType() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeDefinitionRepository typeRepository = mock(ProductTypeDefinitionRepository.class);
    MarketNicheRepository nicheRepository = mock(MarketNicheRepository.class);
    ProductService service = newService(productRepository, typeRepository, nicheRepository);
    CreateProductRequest request = new CreateProductRequest();
    request.setName("Ideia ainda aberta");
    request.setMarketNicheId(10L);
    when(nicheRepository.findById(10L)).thenReturn(Optional.of(new MarketNiche()));

    assertThatThrownBy(() -> service.createProduct(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Selecione um tipo em uso");
  }

  /** Deve preservar o nome interno quando um cliente antigo não envia o novo campo. */
  @Test
  void updateProductPreservesInternalIdentityForLegacyClient() {
    ProductRepository productRepository = mock(ProductRepository.class);
    MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
    ProductService service =
        newService(
            productRepository,
            mock(InstagramAccountRepository.class),
            marketNicheRepository,
            mock(AssetRepository.class));
    Product product =
        Product.builder()
            .id(1L)
            .name("Nome comercial antigo")
            .internalName("Projeto original")
            .aliases(Set.of("Apelido histórico"))
            .build();
    CreateProductRequest request = new CreateProductRequest();
    request.setName("Novo nome comercial");
    request.setMarketNicheId(10L);
    MarketNiche niche = new MarketNiche();

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(marketNicheRepository.findById(10L)).thenReturn(Optional.of(niche));
    when(productRepository.save(product)).thenReturn(product);

    Product updated = service.updateProduct(1L, request);

    assertThat(updated.getInternalName()).isEqualTo("Projeto original");
    assertThat(updated.getAliases()).containsExactly("Apelido histórico");
  }

  /** Deve bloquear apelido que já identifica outro produto do catálogo. */
  @Test
  void updateProductRejectsAmbiguousAlias() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductService service =
        newService(
            productRepository,
            mock(InstagramAccountRepository.class),
            mock(MarketNicheRepository.class),
            mock(AssetRepository.class));
    Product product = Product.builder().id(9L).name("Kit WhatsApp Pronto").build();
    CreateProductRequest request = new CreateProductRequest();
    request.setName("Kit WhatsApp Pronto");
    request.setInternalName("Implantação WhatsApp 48h");
    request.setAliases(List.of("MUSA v7"));

    when(productRepository.findById(9L)).thenReturn(Optional.of(product));
    when(productRepository.countIdentityOnAnotherProduct(9L, "MUSA v7")).thenReturn(1L);

    assertThatThrownBy(() -> service.updateProduct(9L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("já identifica outro produto");
  }

  /** Deve bloquear slug que já funciona como nome ou apelido de outro produto. */
  @Test
  void updateProductRejectsSlugThatIdentifiesAnotherProduct() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductService service =
        newService(
            productRepository,
            mock(InstagramAccountRepository.class),
            mock(MarketNicheRepository.class),
            mock(AssetRepository.class));
    Product product = Product.builder().id(9L).name("Oferta WhatsApp").build();
    CreateProductRequest request = new CreateProductRequest();
    request.setSlug("musa-v7");
    request.setName("Oferta WhatsApp");
    request.setInternalName("Implantação WhatsApp 48h");
    request.setAliases(List.of());

    when(productRepository.findById(9L)).thenReturn(Optional.of(product));
    when(productRepository.countIdentityOnAnotherProduct(9L, "musa-v7")).thenReturn(1L);

    assertThatThrownBy(() -> service.updateProduct(9L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("já identifica outro produto");
  }

  /** Deve pesquisar a mesma entidade por um apelido usado por pessoas ou agentes. */
  @Test
  void listProductsSearchesByInternalIdentity() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductService service =
        newService(
            productRepository,
            mock(InstagramAccountRepository.class),
            mock(MarketNicheRepository.class),
            mock(AssetRepository.class));
    Product musa = Product.builder().id(4L).name("Método MUSA").build();
    when(productRepository.searchByIdentity("MUSA v7")).thenReturn(List.of(musa));

    assertThat(service.listProducts(" MUSA v7 ")).containsExactly(musa);
  }

  /** Rejeita mapa sem limite de verdade para impedir associação comercial enganosa. */
  @Test
  void updateProductRejectsDesireMapWithoutTruthBoundary() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductService service =
        newService(
            productRepository,
            mock(InstagramAccountRepository.class),
            mock(MarketNicheRepository.class),
            mock(AssetRepository.class));
    Product product = Product.builder().id(1L).build();
    CreateProductRequest request = new CreateProductRequest();
    request.setDesireAssociationMapJson(
        """
        {"painState":"dor","desiredState":"prazer","territories":[
          {"code":"PRIDE","name":"Orgulho","idea":"Ser valorizada","symbols":[]}],
         "causalChain":[],"evidence":{},"prohibitedAssociations":[],"measurementPlan":{}}
        """);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    assertThatThrownBy(() -> service.updateProduct(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("truthBoundary");
  }

  /** Rejeita contrato incompleto para impedir comparações comerciais sem base comum. */
  @Test
  void updateProductRejectsIncompleteValidationDefinition() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductService service =
        newService(
            productRepository,
            mock(InstagramAccountRepository.class),
            mock(MarketNicheRepository.class),
            mock(AssetRepository.class));
    Product product = Product.builder().id(1L).build();
    CreateProductRequest request = new CreateProductRequest();
    request.setValidationDefinitionJson("{\"problem\":{}}");
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    assertThatThrownBy(() -> service.updateProduct(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("promise");
  }

  /** Rejeita texto inválido para preservar a auditabilidade da definição versionada. */
  @Test
  void updateProductRejectsInvalidValidationDefinitionJson() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductService service =
        newService(
            productRepository,
            mock(InstagramAccountRepository.class),
            mock(MarketNicheRepository.class),
            mock(AssetRepository.class));
    Product product = Product.builder().id(1L).build();
    CreateProductRequest request = new CreateProductRequest();
    request.setValidationDefinitionJson("não é JSON");
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    assertThatThrownBy(() -> service.updateProduct(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("JSON válido");
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
            .internalName("MUSA v7 — vídeos orientados ao desejo")
            .aliases(Set.of("Projeto desejo", "Versão sete"))
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
    assertThat(markdown).doesNotContain("MUSA v7 — vídeos orientados ao desejo");
    assertThat(markdown).doesNotContain("Projeto desejo");
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
