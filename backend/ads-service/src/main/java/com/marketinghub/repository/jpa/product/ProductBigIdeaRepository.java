package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.bigidea.v1.ProductBigIdea;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persiste revisões da Big Idea isoladas por produto. */
public interface ProductBigIdeaRepository extends JpaRepository<ProductBigIdea, Long> {
  /** Consulta o histórico completo do produto, da revisão mais recente à inicial. */
  List<ProductBigIdea> findByProductIdOrderByRevisionDesc(Long productId);
}
