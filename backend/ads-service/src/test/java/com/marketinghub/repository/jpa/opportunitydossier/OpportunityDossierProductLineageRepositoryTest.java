package com.marketinghub.repository.jpa.opportunitydossier;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.product.Product;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: comprovar a projeção JPA da linhagem entre produto, dossiê e ciclo factual. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class OpportunityDossierProductLineageRepositoryTest {
  @Autowired private TestEntityManager entityManager;
  @Autowired private OpportunityDossierRepository repository;

  /** Recupera o ciclo original diretamente pelo produto materializado. */
  @Test
  void findsProductDiscoveryCycleIdByCreatedProductId() {
    Product mira = Product.builder().name("Mira").internalName("Mira").build();
    entityManager.persist(mira);
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setTheme("Beleza e bem-estar");
    cycle.setCountry("BR");
    cycle.setLanguage("pt-BR");
    entityManager.persist(cycle);
    OpportunityDossier dossier =
        OpportunityDossier.builder()
            .title("Rotina pessoal de skincare")
            .ownerAgentKey("market-radar")
            .targetAudience("Mulheres de 35 a 60 anos")
            .mainPain("Incerteza sobre a própria rotina")
            .referenceProduct("Experiência digital personalizada")
            .aiAdvantage("Personalização da rotina com IA")
            .productDiscoveryCycle(cycle)
            .createdProduct(mira)
            .build();
    entityManager.persist(dossier);
    entityManager.flush();
    entityManager.clear();

    assertThat(repository.findProductDiscoveryCycleIdByCreatedProductId(mira.getId()))
        .contains(cycle.getId());
  }
}
