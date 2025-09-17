package com.marketinghub.audience.repository;

import com.marketinghub.audience.Audience;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * Repository for persisting {@link Audience} entities.
 */
public interface AudienceRepository extends CrudRepository<Audience, Long> {
    List<Audience> findByNicheId(Long nicheId);

    @Query("""
            select a from Audience a
            left join fetch a.hypothesis
            where a.niche.id = :nicheId
            """)
    List<Audience> findDetailedByNicheId(@Param("nicheId") Long nicheId);
}
