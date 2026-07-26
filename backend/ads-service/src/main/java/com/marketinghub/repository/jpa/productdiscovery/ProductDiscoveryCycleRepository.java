package com.marketinghub.repository.jpa.productdiscovery;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório dos ciclos de descoberta de produtos PDE. */
public interface ProductDiscoveryCycleRepository extends JpaRepository<ProductDiscoveryCycle, Long> {

    /** Lista os ciclos mais recentes para a tela administrativa. */
    List<ProductDiscoveryCycle> findTop50ByOrderByUpdatedAtDesc();

    /** Lista ciclos pendentes para consumo pelo worker. */
    List<ProductDiscoveryCycle> findTop5ByStatusInOrderByUpdatedAtAsc(Collection<ProductDiscoveryCycleStatus> statuses);
}
