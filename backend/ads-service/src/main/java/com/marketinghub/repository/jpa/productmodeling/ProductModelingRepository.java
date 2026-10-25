package com.marketinghub.repository.jpa.productmodeling;

import com.marketinghub.product.modeling.v1.ProductModeling;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/** Consulta modelagens e serializa a abertura da pesquisa sem duplicar execução. */
public interface ProductModelingRepository extends JpaRepository<ProductModeling, Long> {
  /** Recupera uma preparação repetida pela mesma chave. */
  Optional<ProductModeling> findByRequestKey(String requestKey);

  /** Recupera a origem exata do dossiê para o handoff. */
  Optional<ProductModeling> findByDiscoveryCycleId(Long cycleId);

  /** Encontra somente a modelagem adotada pelo experimento exato. */
  Optional<ProductModeling> findByExperimentId(Long experimentId);

  /** Encontra somente a modelagem adotada pelo ciclo exato. */
  Optional<ProductModeling> findByLearningCycleId(Long cycleId);

  /** Lista preparações recentes, sem carregar resultados de modelos. */
  List<ProductModeling> findTop50ByOrderByIdDesc();

  /** Protege a transição idempotente para a pesquisa. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from ProductModeling m where m.id = :id")
  Optional<ProductModeling> findLockedById(@Param("id") Long id);
}
