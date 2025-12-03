package com.marketinghub.watermark.repository;

import com.marketinghub.watermark.entity.FlowSubmissionImagePackageEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowSubmissionImagePackageRepository extends JpaRepository<FlowSubmissionImagePackageEntity, Long> {

    @Query("select p.id from FlowSubmissionImagePackageEntity p where p.status = :status order by p.updatedAt asc")
    List<Long> findIdsByStatus(@Param("status") FlowSubmissionImagePackageEntity.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.asset", "items.watermark", "items.watermark.asset"})
    @Query("select p from FlowSubmissionImagePackageEntity p where p.id = :id")
    Optional<FlowSubmissionImagePackageEntity> findDetailedById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE flow_submission_image_package SET status = :newStatus, failure_reason = :failureReason, updated_at = CURRENT_TIMESTAMP WHERE id = :id AND status = :expectedStatus", nativeQuery = true)
    int updateStatusRaw(
            @Param("id") Long id,
            @Param("expectedStatus") String expectedStatus,
            @Param("newStatus") String newStatus,
            @Param("failureReason") String failureReason);

    default int updateStatus(
            Long id,
            FlowSubmissionImagePackageEntity.Status expectedStatus,
            FlowSubmissionImagePackageEntity.Status newStatus,
            String failureReason) {
        return updateStatusRaw(id, expectedStatus.name(), newStatus.name(), failureReason);
    }
}
