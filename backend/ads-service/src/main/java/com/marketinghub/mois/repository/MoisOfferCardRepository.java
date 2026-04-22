package com.marketinghub.mois.repository;

import com.marketinghub.mois.MoisOfferCard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MoisOfferCardRepository extends JpaRepository<MoisOfferCard, Long>, JpaSpecificationExecutor<MoisOfferCard> {
    List<MoisOfferCard> findByRequest_RequestIdOrderByCreatedAtDesc(String requestId);

    Optional<MoisOfferCard> findByArtifactId(String artifactId);

    long countByRequest_RequestId(String requestId);
}
