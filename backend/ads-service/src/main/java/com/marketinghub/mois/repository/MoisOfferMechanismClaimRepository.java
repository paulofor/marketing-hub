package com.marketinghub.mois.repository;

import com.marketinghub.mois.MoisOfferMechanismClaim;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoisOfferMechanismClaimRepository extends JpaRepository<MoisOfferMechanismClaim, Long> {
    List<MoisOfferMechanismClaim> findByRequest_RequestIdOrderByCreatedAtAsc(String requestId);

    List<MoisOfferMechanismClaim> findByOfferCard_ArtifactIdOrderByCreatedAtAsc(String offerId);

    Optional<MoisOfferMechanismClaim> findByArtifactId(String artifactId);

    long countByRequest_RequestId(String requestId);
}
