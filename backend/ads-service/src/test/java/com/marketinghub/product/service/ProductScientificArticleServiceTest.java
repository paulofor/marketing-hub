package com.marketinghub.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.product.Product;
import com.marketinghub.product.ProductScientificArticle;
import com.marketinghub.product.dto.SaveProductScientificArticleRequest;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.product.ProductScientificArticleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: validar o cadastro de artigos científicos associados a produtos. */
class ProductScientificArticleServiceTest {

  /** Deve persistir hash SHA-256 do link para evitar índice único longo no MySQL 5.7. */
  @Test
  void createArticleStoresSha256LinkHash() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ProductScientificArticleRepository articleRepository =
        mock(ProductScientificArticleRepository.class);
    ProductScientificArticleService service =
        new ProductScientificArticleService(productRepository, articleRepository);
    Product product = Product.builder().id(1L).build();
    SaveProductScientificArticleRequest request =
        new SaveProductScientificArticleRequest(
            " https://doi.org/10.1016/j.jesp.2012.02.008 ",
            "Enclothed cognition",
            "Cognição vestida",
            "Resumo operacional.",
            "Aplicação no mecanismo.");

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(articleRepository.save(org.mockito.ArgumentMatchers.any(ProductScientificArticle.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.createArticle(1L, request);

    ArgumentCaptor<ProductScientificArticle> captor =
        ArgumentCaptor.forClass(ProductScientificArticle.class);
    verify(articleRepository).save(captor.capture());
    ProductScientificArticle saved = captor.getValue();
    assertThat(saved.getLink()).isEqualTo("https://doi.org/10.1016/j.jesp.2012.02.008");
    assertThat(saved.getLinkHash())
        .isEqualTo("7178068633012b272692a6c56651e72abc8b82d64cd4a09dfa766e3c590791b5");
  }
}
