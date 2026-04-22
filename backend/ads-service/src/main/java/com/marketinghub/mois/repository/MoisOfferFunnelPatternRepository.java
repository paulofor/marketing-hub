package com.marketinghub.mois.repository;

import com.marketinghub.mois.MoisOfferFunnelPattern;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoisOfferFunnelPatternRepository extends JpaRepository<MoisOfferFunnelPattern, Long> {
    List<MoisOfferFunnelPattern> findByRequest_RequestIdOrderByCreatedAtAsc(String requestId);

    List<MoisOfferFunnelPattern> findByOfferCard_ArtifactIdOrderByCreatedAtAsc(String offerId);

    Optional<MoisOfferFunnelPattern> findByArtifactId(String artifactId);

    long countByRequest_RequestId(String requestId);
}
