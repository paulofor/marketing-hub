package com.marketinghub.repository.jpa.productdiscovery;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCustomerInterview;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persiste entrevistas anônimas vinculadas ao aprofundamento factual de Argos. */
public interface ProductDiscoveryCustomerInterviewRepository
    extends JpaRepository<ProductDiscoveryCustomerInterview, Long> {

  /** Lista as entrevistas do ciclo na ordem em que foram registradas. */
  List<ProductDiscoveryCustomerInterview> findAllByCycleIdOrderByIdAsc(Long cycleId);

  /** Impede que o mesmo código anônimo seja contado duas vezes no mesmo ciclo. */
  boolean existsByCycleIdAndAnonymousParticipantCodeIgnoreCase(
      Long cycleId, String anonymousParticipantCode);
}
