package com.marketinghub.product;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = AdsServiceApplication.class)
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
                .aiCost(java.math.BigDecimal.TEN)
                .build();
        repository.save(product);
        assertThat(repository.findById(product.getId())).isPresent();
    }
}
