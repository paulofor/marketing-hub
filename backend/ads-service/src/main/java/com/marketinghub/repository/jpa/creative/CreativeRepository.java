package com.marketinghub.repository.jpa.creative;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for creatives.
 */
public interface CreativeRepository extends JpaRepository<Creative, Long> {
    List<Creative> findByExperimentId(Long experimentId);

    boolean existsByExperimentIdAndStatus(Long experimentId, CreativeStatus status);

    @Query("select c from Creative c join fetch c.experiment where c.id = :id")
    Optional<Creative> findByIdWithExperiment(@Param("id") Long id);

    long countByExperimentId(Long experimentId);
}
