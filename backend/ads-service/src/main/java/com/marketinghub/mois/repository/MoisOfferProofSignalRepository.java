package com.marketinghub.mois.repository;

import com.marketinghub.mois.MoisOfferProofSignal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoisOfferProofSignalRepository extends JpaRepository<MoisOfferProofSignal, Long> {
    List<MoisOfferProofSignal> findByRequest_RequestIdOrderByCreatedAtAsc(String requestId);

    List<MoisOfferProofSignal> findByOfferCard_ArtifactIdOrderByCreatedAtAsc(String offerId);

    Optional<MoisOfferProofSignal> findByArtifactId(String artifactId);

    long countByRequest_RequestId(String requestId);
}
