package com.marketinghub.deliverable.repository;

import com.marketinghub.deliverable.DeliverablePackage;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link DeliverablePackage} entities.
 */
public interface DeliverablePackageRepository extends JpaRepository<DeliverablePackage, Long> {
    List<DeliverablePackage> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);
    List<DeliverablePackage> findByHypothesisIdOrderByCreatedAtDesc(UUID hypothesisId);
    List<DeliverablePackage> findByExperimentHypothesisRefIdOrderByCreatedAtDesc(UUID hypothesisId);

    default List<DeliverablePackage> findAllLatestFirst() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
