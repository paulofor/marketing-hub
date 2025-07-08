package com.marketinghub.worker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = SuccessProductWorkerApplication.class)
class SuccessProductRepositoryTest {

    @Autowired
    SuccessProductRepository repository;

    @Test
    void testSaveSuccessProduct() {
        SuccessProduct product = SuccessProduct.builder()
                .description("Great product")
                .build();
        repository.save(product);
        SuccessProduct saved = repository.findById(product.getId()).orElseThrow();
        assertThat(saved.isNovo()).isTrue();
    }
}
