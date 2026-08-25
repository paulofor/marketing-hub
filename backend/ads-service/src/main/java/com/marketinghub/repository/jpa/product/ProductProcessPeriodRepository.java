package com.marketinghub.repository.jpa.product;

import com.marketinghub.product.ProductProcessPeriod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar permanências de produtos nos macroprocessos. */
public interface ProductProcessPeriodRepository extends JpaRepository<ProductProcessPeriod, Long> {
  /** Lista o histórico do produto na ordem em que os macroprocessos foram iniciados. */
  List<ProductProcessPeriod> findByProductIdOrderByEnteredAtAscIdAsc(Long productId);

  /** Localiza a permanência aberta mais recente de um produto. */
  Optional<ProductProcessPeriod> findTopByProductIdAndExitedAtIsNullOrderByEnteredAtDescIdDesc(
      Long productId);
}
