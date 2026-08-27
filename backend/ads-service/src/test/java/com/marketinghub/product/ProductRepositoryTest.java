package com.marketinghub.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: validar a persistência e a pesquisa das identidades de produto. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class ProductRepositoryTest {

  @Autowired ProductRepository repository;

  /** Deve persistir, pesquisar e substituir os apelidos internos de um produto. */
  @Test
  void testSaveProduct() {
    Product product =
        Product.builder()
            .niche("Health")
            .name("Vitalidade em 30 Dias")
            .internalName("Projeto Vitalidade")
            .aliases(Set.of("Energia 30", "Plano Vital"))
            .avatar("Women")
            .explicitPain("Lack of energy")
            .promise("More vitality in 30 days")
            .uniqueMechanism("Special diet")
            .scientificEvidencePack("Evidence Pack v1")
            .sevenDayJourney("Dia 1: diagnóstico")
            .supportMaterialPositioning("Apoio secundário")
            .primaryCta("Começar agora")
            .aiCost(java.math.BigDecimal.TEN)
            .build();
    repository.save(product);
    assertThat(repository.findById(product.getId())).isPresent();
    assertThat(repository.findById(product.getId()).orElseThrow().getScientificEvidencePack())
        .isEqualTo("Evidence Pack v1");
    assertThat(repository.findById(product.getId()).orElseThrow().getSevenDayJourney())
        .isEqualTo("Dia 1: diagnóstico");
    assertThat(repository.findById(product.getId()).orElseThrow().getSupportMaterialPositioning())
        .isEqualTo("Apoio secundário");
    assertThat(repository.findById(product.getId()).orElseThrow().getPrimaryCta())
        .isEqualTo("Começar agora");
    assertThat(repository.searchByIdentity("Energia 30")).containsExactly(product);
    assertThat(repository.searchByIdentity("Projeto Vitalidade")).containsExactly(product);
    assertThat(repository.countIdentityOnAnotherProduct(null, "Plano Vital")).isEqualTo(1L);
    assertThat(repository.countIdentityOnAnotherProduct(product.getId(), "Plano Vital")).isZero();

    product.setAliases(new LinkedHashSet<>(Set.of("Energia Renovada")));
    repository.saveAndFlush(product);

    Product updated = repository.findById(product.getId()).orElseThrow();
    assertThat(updated.getAliases()).containsExactly("Energia Renovada");
    assertThat(repository.searchByIdentity("Plano Vital")).isEmpty();
  }

  /** Deve atualizar somente o nome interno e preservar os demais campos do produto. */
  @Test
  void updateOnlyInternalName() {
    Product product =
        repository.saveAndFlush(
            Product.builder()
                .name("Nexo — Clareza, Sentido e Ação")
                .internalName("Nome anterior")
                .commercialNotes("Contrato preservado")
                .build());

    assertThat(repository.updateInternalName(product.getId(), "Polaris")).isEqualTo(1);

    Product updated = repository.findById(product.getId()).orElseThrow();
    assertThat(updated.getInternalName()).isEqualTo("Polaris");
    assertThat(updated.getName()).isEqualTo("Nexo — Clareza, Sentido e Ação");
    assertThat(updated.getCommercialNotes()).isEqualTo("Contrato preservado");
  }

  /** Deve retornar somente produtos em PLAY nas consultas operacionais. */
  @Test
  void findOnlyProductsInPlayState() {
    Product productInPlay =
        repository.saveAndFlush(
            Product.builder()
                .name("Produto em PLAY")
                .internalName("Produto operacional")
                .automaticExecutionEnabled(true)
                .build());
    Product productStopped =
        repository.saveAndFlush(
            Product.builder()
                .name("Produto em STOP")
                .internalName("Produto interrompido")
                .automaticExecutionEnabled(false)
                .build());

    assertThat(repository.findAllInPlayState())
        .extracting(Product::getId)
        .contains(productInPlay.getId())
        .doesNotContain(productStopped.getId());
    assertThat(repository.searchByIdentityInPlayState("Produto"))
        .extracting(Product::getId)
        .containsExactly(productInPlay.getId());
  }
}
