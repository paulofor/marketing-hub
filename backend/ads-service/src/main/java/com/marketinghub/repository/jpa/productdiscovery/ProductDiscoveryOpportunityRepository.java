package com.marketinghub.repository.jpa.productdiscovery;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório das oportunidades de produtos PDE descobertas. */
public interface ProductDiscoveryOpportunityRepository extends JpaRepository<ProductDiscoveryOpportunity, Long> {

    /** Lista oportunidades do ciclo ordenadas pelo score comercial. */
    List<ProductDiscoveryOpportunity> findAllByCycleIdOrderByScoreDesc(Long cycleId);

    /** Remove oportunidades de um ciclo antes de registrar um novo resultado auditável. */
    void deleteAllByCycleId(Long cycleId);
}
