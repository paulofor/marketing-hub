package com.marketinghub.targeting.repository;

import com.marketinghub.targeting.TargetingCandidate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TargetingCandidateRepository extends JpaRepository<TargetingCandidate, Long> {

    @EntityGraph(attributePaths = "options")
    List<TargetingCandidate> findByRequestIdOrderByCreatedAtAsc(UUID requestId);

    @EntityGraph(attributePaths = "options")
    Optional<TargetingCandidate> findDetailedById(Long id);
}
