package com.marketinghub.repository.jpa.productdiscovery;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryMetaAttempt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar os vínculos auditáveis entre tentativas e investigações Meta. */
public interface ProductDiscoveryMetaAttemptRepository
    extends JpaRepository<ProductDiscoveryMetaAttempt, Long> {

  /** Localiza a investigação imutável de uma tentativa do ciclo. */
  Optional<ProductDiscoveryMetaAttempt> findByCycleIdAndAttemptNumber(
      Long cycleId, int attemptNumber);

  /** Informa se a investigação já foi consumida por outra tentativa do mesmo ciclo. */
  boolean existsByCycleIdAndInvestigationId(Long cycleId, Long investigationId);

  /** Informa se outra tentativa do ciclo já executou a mesma consulta normalizada. */
  boolean existsByCycleIdAndSearchQueryIgnoreCase(Long cycleId, String searchQuery);
}
