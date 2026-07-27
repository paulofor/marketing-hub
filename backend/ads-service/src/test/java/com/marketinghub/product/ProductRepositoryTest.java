package com.marketinghub.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.repository.jpa.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class ProductRepositoryTest {

  @Autowired ProductRepository repository;

  @Test
  void testSaveProduct() {
    Product product =
        Product.builder()
            .niche("Health")
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
  }
}
