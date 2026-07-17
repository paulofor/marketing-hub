package com.marketinghub.product.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Responsabilidade: validar as regras de cadastro comercial de produtos. */
class ProductServiceTest {

    /** Deve persistir alterações comerciais em um produto já existente. */
    @Test
    void updateProduct() {
        ProductRepository productRepository = mock(ProductRepository.class);
        InstagramAccountRepository accountRepository = mock(InstagramAccountRepository.class);
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        ProductService service = new ProductService(productRepository, accountRepository, marketNicheRepository);
        Product product = Product.builder().id(1L).name("Nome antigo").build();
        MarketNiche niche = new MarketNiche();
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Método MUSA - Presença Elegante em 7 Dias");
        request.setSlug("metodo-musa-7-dias");
        request.setMarketNicheId(10L);
        request.setCurrentPriceBrl(new BigDecimal("47.00"));
        request.setTargetAudience("Mulheres urbanas");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(marketNicheRepository.findById(10L)).thenReturn(Optional.of(niche));
        when(productRepository.save(product)).thenReturn(product);

        Product updated = service.updateProduct(1L, request);

        assertThat(updated.getName()).isEqualTo(request.getName());
        assertThat(updated.getSlug()).isEqualTo(request.getSlug());
        assertThat(updated.getCurrentPriceBrl()).isEqualByComparingTo("47.00");
        assertThat(updated.getTargetAudience()).isEqualTo(request.getTargetAudience());
        assertThat(updated.getMarketNiche()).isSameAs(niche);
    }
}
