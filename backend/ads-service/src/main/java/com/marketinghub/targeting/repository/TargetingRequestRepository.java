package com.marketinghub.targeting.repository;

import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.TargetingRequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TargetingRequestRepository extends JpaRepository<TargetingRequest, UUID> {
    @Query("select r from TargetingRequest r where r.status = :status order by r.createdAt asc")
    List<TargetingRequest> findByStatus(@Param("status") TargetingRequestStatus status);

    @EntityGraph(attributePaths = {"candidates", "candidates.options"})
    List<TargetingRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"candidates", "candidates.options"})
    List<TargetingRequest> findByStatusOrderByCreatedAtDesc(TargetingRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"candidates", "candidates.options"})
    Optional<TargetingRequest> findDetailedById(UUID id);
}
