package com.marketinghub.mois.repository;

import com.marketinghub.mois.MoisDiscoveryRequest;
import com.marketinghub.mois.MoisDiscoveryRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MoisDiscoveryRequestRepository extends JpaRepository<MoisDiscoveryRequest, Long>, JpaSpecificationExecutor<MoisDiscoveryRequest> {
    Optional<MoisDiscoveryRequest> findByRequestId(String requestId);

    List<MoisDiscoveryRequest> findTop100ByOrderByCreatedAtDesc();

    List<MoisDiscoveryRequest> findTop100ByStatusOrderByCreatedAtDesc(MoisDiscoveryRequestStatus status);
}
