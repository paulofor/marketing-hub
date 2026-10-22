package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.bigidea.v1.ProductBigIdeaBinding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persiste a versão da Big Idea fixada por contexto comercial e produto. */
public interface ProductBigIdeaBindingRepository
    extends JpaRepository<ProductBigIdeaBinding, Long> {
  /** Recupera a referência exata sem compartilhar versões entre produtos. */
  Optional<ProductBigIdeaBinding> findByProductIdAndSourceReference(
      Long productId, String sourceReference);
}
