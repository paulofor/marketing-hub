package com.marketinghub.repository.jpa.deliverable;

import com.marketinghub.deliverable.DeliverablePackage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link DeliverablePackage} entities. */
public interface DeliverablePackageRepository extends JpaRepository<DeliverablePackage, Long> {
  List<DeliverablePackage> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);

  List<DeliverablePackage> findByHypothesisIdOrderByCreatedAtDesc(UUID hypothesisId);

  List<DeliverablePackage> findByExperimentHypothesisRefIdOrderByCreatedAtDesc(UUID hypothesisId);

  default List<DeliverablePackage> findAllLatestFirst() {
    return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
  }
}
