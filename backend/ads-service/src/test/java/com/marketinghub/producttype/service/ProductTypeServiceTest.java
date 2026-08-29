package com.marketinghub.producttype.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.producttype.ProductTypeStatus;
import com.marketinghub.producttype.service.catalog.ProductTypeBlueprintData;
import com.marketinghub.producttype.service.catalog.SaveProductTypeRequest;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar as regras do catálogo extensível de tipos de produto. */
class ProductTypeServiceTest {
  /** Deve criar proposta com código derivado e apelidos sem redundância. */
  @Test
  void createProposedTypeWithAliases() {
    ProductTypeDefinitionRepository repository = mock(ProductTypeDefinitionRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeService service = new ProductTypeService(repository, productRepository);
    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of());
    when(repository.save(any(ProductTypeDefinition.class)))
        .thenAnswer(
            invocation -> {
              ProductTypeDefinition saved = invocation.getArgument(0);
              saved.setId(12L);
              return saved;
            });

    var response =
        service.create(
            new SaveProductTypeRequest(
                null,
                "Experiência guiada por desafios",
                "Granada",
                "Valor percebido pela prática.",
                List.of(
                    "Desafio guiado",
                    "desafio guiado",
                    "Experiência guiada por desafios",
                    "Granada"),
                null));

    assertThat(response.code()).isEqualTo("EXPERIENCIA_GUIADA_POR_DESAFIOS");
    assertThat(response.internalName()).isEqualTo("Granada");
    assertThat(response.status()).isEqualTo(ProductTypeStatus.PROPOSED);
    assertThat(response.aliases()).containsExactly("Desafio guiado");
  }

  /** Deve impedir que um apelido torne duas classificações ambíguas. */
  @Test
  void rejectAliasAlreadyUsedByAnotherType() {
    ProductTypeDefinitionRepository repository = mock(ProductTypeDefinitionRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeService service = new ProductTypeService(repository, productRepository);
    ProductTypeDefinition existing =
        ProductTypeDefinition.builder()
            .id(1L)
            .code("PDE")
            .name("Produto Digital Experiencial")
            .internalName("Opala")
            .aliases(Set.of("Experiência guiada"))
            .status(ProductTypeStatus.ACTIVE)
            .build();
    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(existing));

    assertThatThrownBy(
            () ->
                service.create(
                    new SaveProductTypeRequest(
                        "GUIDED_PRODUCT",
                        "Produto guiado",
                        "Granada",
                        null,
                        List.of("experiencia guiada"),
                        ProductTypeStatus.PROPOSED)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("já identifica outro tipo");
  }

  /** Deve impedir que o codinome mineral identifique dois tipos diferentes. */
  @Test
  void rejectInternalNameAlreadyUsedByAnotherType() {
    ProductTypeDefinitionRepository repository = mock(ProductTypeDefinitionRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeService service = new ProductTypeService(repository, productRepository);
    ProductTypeDefinition existing =
        ProductTypeDefinition.builder()
            .id(1L)
            .code("PDE")
            .name("Produto Digital Experiencial")
            .internalName("Opala")
            .aliases(Set.of())
            .status(ProductTypeStatus.ACTIVE)
            .build();
    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(existing));

    assertThatThrownBy(
            () ->
                service.create(
                    new SaveProductTypeRequest(
                        "GUIDED_PRODUCT",
                        "Produto guiado",
                        "ópala",
                        null,
                        List.of(),
                        ProductTypeStatus.PROPOSED)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("já identifica outro tipo");
  }

  /** Deve sincronizar o nome legado dos produtos quando o nome canônico evolui. */
  @Test
  void updateNameSynchronizesLinkedProducts() {
    ProductTypeDefinitionRepository repository = mock(ProductTypeDefinitionRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeService service = new ProductTypeService(repository, productRepository);
    ProductTypeDefinition type =
        ProductTypeDefinition.builder()
            .id(3L)
            .code("AI_PRODUCT")
            .name("Produto IA")
            .internalName("Safira")
            .aliases(Set.of())
            .status(ProductTypeStatus.ACTIVE)
            .build();
    Product product = Product.builder().id(9L).productType("Produto IA").build();
    when(repository.findById(3L)).thenReturn(java.util.Optional.of(type));
    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(type));
    when(repository.save(type)).thenReturn(type);
    when(productRepository.countByProductTypeDefinition_Id(3L)).thenReturn(1L);
    when(productRepository.findAllByProductTypeDefinition_Id(3L)).thenReturn(List.of(product));

    service.update(
        3L,
        new SaveProductTypeRequest(
            "AI_PRODUCT",
            "Produto personalizado por IA",
            "Safira",
            "Transforma entrada em saída útil.",
            List.of("Produto IA"),
            ProductTypeStatus.ACTIVE));

    assertThat(product.getProductType()).isEqualTo("Produto personalizado por IA");
    verify(productRepository).saveAll(List.of(product));
  }

  /** Deve preservar o código estável quando o tipo já classifica algum produto. */
  @Test
  void rejectCodeChangeWhenTypeIsInUse() {
    ProductTypeDefinitionRepository repository = mock(ProductTypeDefinitionRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeService service = new ProductTypeService(repository, productRepository);
    ProductTypeDefinition type =
        ProductTypeDefinition.builder()
            .id(3L)
            .code("PDE")
            .name("Produto Digital Experiencial")
            .internalName("Opala")
            .aliases(Set.of())
            .status(ProductTypeStatus.ACTIVE)
            .build();
    when(repository.findById(3L)).thenReturn(java.util.Optional.of(type));
    when(productRepository.countByProductTypeDefinition_Id(3L)).thenReturn(2L);

    assertThatThrownBy(
            () ->
                service.update(
                    3L,
                    new SaveProductTypeRequest(
                        "NOVO_CODIGO",
                        type.getName(),
                        "Opala",
                        null,
                        List.of(),
                        ProductTypeStatus.ACTIVE)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("código do tipo não pode mudar");
  }

  /** Deve exigir uma fronteira de uso antes de disponibilizar o tipo para produtos. */
  @Test
  void rejectActiveTypeWithoutDescription() {
    ProductTypeDefinitionRepository repository = mock(ProductTypeDefinitionRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeService service = new ProductTypeService(repository, productRepository);
    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                service.create(
                    new SaveProductTypeRequest(
                        "GUIDED",
                        "Experiência guiada",
                        "Granada",
                        null,
                        List.of(),
                        ProductTypeStatus.ACTIVE)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Explique quando usar");
  }

  /** Deve pesquisar apelidos e esconder propostas da seleção operacional padrão. */
  @Test
  void listActiveTypesByAlias() {
    ProductTypeDefinitionRepository repository = mock(ProductTypeDefinitionRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeService service = new ProductTypeService(repository, productRepository);
    ProductTypeDefinition active =
        ProductTypeDefinition.builder()
            .id(1L)
            .code("PDE")
            .name("Produto Digital Experiencial")
            .internalName("Opala")
            .aliases(Set.of("Experiência guiada"))
            .status(ProductTypeStatus.ACTIVE)
            .build();
    ProductTypeDefinition proposed =
        ProductTypeDefinition.builder()
            .id(2L)
            .code("IMMERSIVE")
            .name("Produto imersivo")
            .internalName("Ametista")
            .aliases(Set.of("Experiência experimental"))
            .status(ProductTypeStatus.PROPOSED)
            .build();
    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(active, proposed));

    assertThat(service.list("experiencia guiada", false)).extracting("id").containsExactly(1L);
    assertThat(service.list("opala", false)).extracting("id").containsExactly(1L);
    assertThat(service.list(null, false)).extracting("id").containsExactly(1L);
  }

  /** Deve ativar Turmalina somente com a base PWA completa e normalizada. */
  @Test
  void createActivePwaConsultantWithCompleteBlueprint() {
    ProductTypeDefinitionRepository repository = mock(ProductTypeDefinitionRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeService service = new ProductTypeService(repository, productRepository);
    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of());
    when(repository.save(any(ProductTypeDefinition.class)))
        .thenAnswer(
            invocation -> {
              ProductTypeDefinition saved = invocation.getArgument(0);
              saved.setId(7L);
              return saved;
            });

    var response =
        service.create(
            new SaveProductTypeRequest(
                "AI_PWA_CONSULTANT_PRODUCT",
                "Consultor PWA com IA",
                "Turmalina",
                "Consultoria visual mobile-first.",
                List.of("Consultor web instalável"),
                ProductTypeStatus.ACTIVE,
                completePwaBlueprint(" pwa ", true)));

    assertThat(response.constructionReady()).isTrue();
    assertThat(response.missingBlueprintFields()).isEmpty();
    assertThat(response.blueprint().primaryChannel()).isEqualTo("PWA");
    assertThat(response.blueprint().frontendSdkModule())
        .isEqualTo("pde-platform/frontend/src/consultant-sdk/v1");
  }

  /** Deve bloquear uma PWA ativa sem SDK React porque a experiência própria ficaria indefinida. */
  @Test
  void rejectActivePwaConsultantWithoutReactSdk() {
    ProductTypeDefinitionRepository repository = mock(ProductTypeDefinitionRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeService service = new ProductTypeService(repository, productRepository);
    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                service.create(
                    new SaveProductTypeRequest(
                        "AI_PWA_CONSULTANT_PRODUCT",
                        "Consultor PWA com IA",
                        "Turmalina",
                        "Consultoria visual mobile-first.",
                        List.of(),
                        ProductTypeStatus.ACTIVE,
                        completePwaBlueprint("PWA", false))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("SDK React");
  }

  /** Deve impedir que um formulário vazio apague a base de um tipo que permanece ativo. */
  @Test
  void rejectClearingBlueprintFromActiveType() {
    ProductTypeDefinitionRepository repository = mock(ProductTypeDefinitionRepository.class);
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductTypeService service = new ProductTypeService(repository, productRepository);
    ProductTypeDefinition type =
        ProductTypeDefinition.builder()
            .id(7L)
            .code("AI_PWA_CONSULTANT_PRODUCT")
            .name("Consultor PWA com IA")
            .internalName("Turmalina")
            .description("Consultoria visual mobile-first.")
            .aliases(Set.of())
            .status(ProductTypeStatus.ACTIVE)
            .blueprintVersion("consultant-pwa-v1")
            .primaryChannel("PWA")
            .build();
    when(repository.findById(7L)).thenReturn(java.util.Optional.of(type));
    when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(type));

    assertThatThrownBy(
            () ->
                service.update(
                    7L,
                    new SaveProductTypeRequest(
                        type.getCode(),
                        type.getName(),
                        type.getInternalName(),
                        type.getDescription(),
                        List.of(),
                        ProductTypeStatus.ACTIVE,
                        new ProductTypeBlueprintData(
                            null, null, null, null, null, null, null, null, null, null, null, null,
                            null))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Complete a base de construção");
  }

  /** Monta todos os contratos obrigatórios de uma base PWA para os cenários do serviço. */
  private ProductTypeBlueprintData completePwaBlueprint(
      String primaryChannel, boolean includeReactSdk) {
    return new ProductTypeBlueprintData(
        "consultant-pwa-v1",
        primaryChannel,
        "Receber orientação no celular.",
        "Transformar contexto em recomendação.",
        "Entrar; conversar; refinar; avaliar.",
        "Cliente, contexto, consentimento e foto opcional.",
        "Orientação, motivo e próximo passo.",
        "Memória segregada por cliente.",
        "Backend PDE, worker Java e App Server.",
        "Bloquear mistura de clientes e mídia sem consentimento.",
        "Orientação entregue, utilidade, retorno, venda e margem.",
        "pde-platform/pde-harness-sdk",
        includeReactSdk ? "pde-platform/frontend/src/consultant-sdk/v1" : null);
  }
}
