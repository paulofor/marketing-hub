package com.marketinghub.repository.jpa.targeting;

import com.marketinghub.targeting.TargetingResolutionJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA responsável pela persistência de TargetingResolutionJob.
 */
public interface TargetingResolutionJobRepository extends JpaRepository<TargetingResolutionJob, Long> {
    Optional<TargetingResolutionJob> findByCandidateId(Long candidateId);

    List<TargetingResolutionJob> findByRequestIdIn(Collection<UUID> requestIds);
}
