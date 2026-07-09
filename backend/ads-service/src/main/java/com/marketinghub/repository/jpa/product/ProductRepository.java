package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de produtos digitais.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
    /** Busca o produto operacional mais recente vinculado ao nicho informado. */
    Optional<Product> findFirstByMarketNiche_IdOrderByCreatedAtDesc(Long marketNicheId);
}
