package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.ProductScientificArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir artigos científicos associados ao mecanismo de produtos. */
public interface ProductScientificArticleRepository
    extends JpaRepository<ProductScientificArticle, Long> {
  /** Lista os artigos de um produto em ordem estável de cadastro. */
  List<ProductScientificArticle> findByProductIdOrderByIdAsc(Long productId);
}
