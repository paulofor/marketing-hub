package com.marketinghub.product;

import com.marketinghub.repository.jpa.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class ProductRepositoryTest {

    @Autowired
    ProductRepository repository;

    @Test
    void testSaveProduct() {
        Product product = Product.builder()
                .niche("Health")
                .avatar("Women")
                .explicitPain("Lack of energy")
                .promise("More vitality in 30 days")
                .uniqueMechanism("Special diet")
                .scientificEvidencePack("Evidence Pack v1")
                .aiCost(java.math.BigDecimal.TEN)
                .build();
        repository.save(product);
        assertThat(repository.findById(product.getId())).isPresent();
        assertThat(repository.findById(product.getId()).orElseThrow().getScientificEvidencePack())
                .isEqualTo("Evidence Pack v1");
    }
}
