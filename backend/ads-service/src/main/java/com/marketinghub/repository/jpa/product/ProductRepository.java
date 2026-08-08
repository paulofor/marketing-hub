package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de produtos digitais. */
public interface ProductRepository extends JpaRepository<Product, Long> {
  /** Busca um produto pelo slug comercial público. */
  Optional<Product> findBySlug(String slug);

  /** Busca o produto operacional mais recente vinculado ao nicho informado. */
  Optional<Product> findFirstByMarketNiche_IdOrderByCreatedAtDesc(Long marketNicheId);

  /** Lista produtos do nicho para impedir que agentes misturem mapas quando houver ambiguidade. */
  List<Product> findAllByMarketNiche_Id(Long marketNicheId);
}
