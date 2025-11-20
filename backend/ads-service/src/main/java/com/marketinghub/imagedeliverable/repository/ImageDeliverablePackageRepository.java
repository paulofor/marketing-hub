package com.marketinghub.imagedeliverable.repository;

import com.marketinghub.imagedeliverable.ImageDeliverablePackage;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ImageDeliverablePackage} entities.
 */
public interface ImageDeliverablePackageRepository extends JpaRepository<ImageDeliverablePackage, Long> {
    List<ImageDeliverablePackage> findByLeadIdOrderByCreatedAtDesc(UUID leadId);

    default List<ImageDeliverablePackage> findAllLatestFirst() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
