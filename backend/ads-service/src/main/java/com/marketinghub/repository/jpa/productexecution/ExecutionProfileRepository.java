package com.marketinghub.repository.jpa.productexecution;

import com.marketinghub.product.executionprofile.v1.ExecutionProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar fichas somente no escopo explícito do produto. */
public interface ExecutionProfileRepository extends JpaRepository<ExecutionProfile, Long> {
  /** Lista as revisões em ordem decrescente para consulta histórica. */
  List<ExecutionProfile> findByProductIdOrderByRevisionNumberDesc(Long productId);

  /** Resolve uma ficha sem aceitar o identificador de outro produto. */
  Optional<ExecutionProfile> findByIdAndProductId(Long id, Long productId);
}
