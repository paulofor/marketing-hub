package com.marketinghub.mois.repository;

import com.marketinghub.mois.MoisOfferPromiseSignal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoisOfferPromiseSignalRepository extends JpaRepository<MoisOfferPromiseSignal, Long> {
    List<MoisOfferPromiseSignal> findByRequest_RequestIdOrderByCreatedAtAsc(String requestId);

    List<MoisOfferPromiseSignal> findByOfferCard_ArtifactIdOrderByCreatedAtAsc(String offerId);

    Optional<MoisOfferPromiseSignal> findByArtifactId(String artifactId);

    long countByRequest_RequestId(String requestId);
}
