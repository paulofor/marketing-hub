package com.marketinghub.repository.jpa.productexecution;

import com.marketinghub.product.executionprofile.v1.ExecutionProfileReview;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: preservar o diário de decisões financeiras da ficha. */
public interface ExecutionProfileReviewRepository
    extends JpaRepository<ExecutionProfileReview, Long> {
  /** Consulta o histórico para projetar a decisão mais recente de cada checkpoint. */
  List<ExecutionProfileReview> findByProfileIdOrderByIdAsc(Long profileId);
}
