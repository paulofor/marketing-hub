package com.marketinghub.targeting.repository;

import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repositório principal dos elementos de segmentação.
 */
public interface TargetingElementRepository extends JpaRepository<TargetingElement, Long> {

    @Query("""
            select e from TargetingElement e
            where (:nicheId is null or e.niche.id = :nicheId)
              and (:type is null or e.type = :type)
              and (:status is null or e.status = :status)
            order by e.createdAt desc
            """)
    List<TargetingElement> findByFilters(@Param("nicheId") Long nicheId,
                                         @Param("type") TargetingElementType type,
                                         @Param("status") TargetingElementStatus status);

    @Query("""
            select e from TargetingElement e
            where e.niche.id = :nicheId
              and e.type = :type
              and e.status = com.marketinghub.targeting.TargetingElementStatus.APPROVED
              and (:hypothesisId is null or e.hypothesis is null or e.hypothesis.id = :hypothesisId)
            """)
    List<TargetingElement> findApprovedForExperiment(@Param("nicheId") Long nicheId,
                                                     @Param("type") TargetingElementType type,
                                                     @Param("hypothesisId") UUID hypothesisId);

    @Query("""
            select case when count(e) > 0 then true else false end
            from TargetingElement e
            where e.niche.id = :nicheId
              and e.type = :type
              and e.status = com.marketinghub.targeting.TargetingElementStatus.APPROVED
              and (:hypothesisId is null or e.hypothesis is null or e.hypothesis.id = :hypothesisId)
            """)
    boolean existsApprovedForExperiment(@Param("nicheId") Long nicheId,
                                        @Param("type") TargetingElementType type,
                                        @Param("hypothesisId") UUID hypothesisId);
}
