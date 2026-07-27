package com.marketinghub.repository.jpa.imagedeliverable;

import com.marketinghub.imagedeliverable.ImageDeliverablePackage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link ImageDeliverablePackage} entities. */
public interface ImageDeliverablePackageRepository
    extends JpaRepository<ImageDeliverablePackage, Long> {
  List<ImageDeliverablePackage> findByLeadIdOrderByCreatedAtDesc(UUID leadId);

  default List<ImageDeliverablePackage> findAllLatestFirst() {
    return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
  }
}
