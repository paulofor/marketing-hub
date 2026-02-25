package com.marketinghub.leadportal.repository;

import com.marketinghub.leadportal.entity.FlowSubmissionImagePackageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlowSubmissionImagePackageRepository
        extends JpaRepository<FlowSubmissionImagePackageEntity, Long> {

    List<FlowSubmissionImagePackageEntity> findBySubmissionIdOrderByCreatedAtDesc(UUID submissionId);

    @Query("""
            select p from FlowSubmissionImagePackageEntity p
            where p.submissionId in (
                select s.id from FlowSubmissionEntity s where s.flowSlug = :slug
            )
            order by p.createdAt desc
            """)
    List<FlowSubmissionImagePackageEntity> findRecentByFlowSlug(
            @Param("slug") String slug, Pageable pageable);
}
