package com.marketinghub.audience.repository;

import com.marketinghub.audience.Audience;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

/**
 * Repository for persisting {@link Audience} entities.
 */
public interface AudienceRepository extends CrudRepository<Audience, Long> {
    List<Audience> findByNicheId(Long nicheId);

    @Query("""
            select a from Audience a
            left join fetch a.hypothesis
            where a.niche.id = :nicheId
              and a.approved = true
            """)
    List<Audience> findDetailedByNicheId(@Param("nicheId") Long nicheId);

    @Query("""
            select case when count(a) > 0 then true else false end
            from Audience a
            where a.niche.id = :nicheId
              and a.approved = true
              and (:hypothesisId is null or a.hypothesis is null or a.hypothesis.id = :hypothesisId)
            """)
    boolean existsApprovedByNicheAndMatchingHypothesis(@Param("nicheId") Long nicheId,
                                                       @Param("hypothesisId") UUID hypothesisId);
}
