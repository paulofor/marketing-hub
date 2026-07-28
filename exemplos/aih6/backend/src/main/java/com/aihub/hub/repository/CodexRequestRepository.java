package com.aihub.hub.repository;

import com.aihub.hub.domain.CodexIntegrationProfile;
import com.aihub.hub.domain.CodexRequest;
import com.aihub.hub.domain.CodexRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Repositório responsável por consultas e persistência das solicitações Codex. */
public interface CodexRequestRepository extends JpaRepository<CodexRequest, Long> {
    List<CodexRequest> findAllByOrderByCreatedAtDesc();
    Page<CodexRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<CodexRequest> findAllByRatingOrderByCreatedAtDesc(Integer rating, Pageable pageable);
    List<CodexRequest> findAllByRatingOrderByCreatedAtDesc(Integer rating);
    List<CodexRequest> findByProblemIdOrderByCreatedAtDesc(Long problemId);
    List<CodexRequest> findByWorkBatchKeyOrderByCreatedAtAsc(String workBatchKey);
    Optional<CodexRequest> findByExternalId(String externalId);
    boolean existsByProfileAndStatusInAndExternalIdIsNotNull(CodexIntegrationProfile profile, Collection<CodexRequestStatus> statuses);
    Optional<CodexRequest> findFirstByProfileAndStatusAndExternalIdIsNullOrderByCreatedAtAsc(CodexIntegrationProfile profile, CodexRequestStatus status);

    /** Soma volume de solicitações e duração em um intervalo operacional. */
    @Query("SELECT COUNT(c), COALESCE(SUM(c.durationMs), 0) FROM CodexRequest c WHERE c.createdAt >= :startsAt AND c.createdAt < :endsAt")
    Object[] summarizeOperationalPeriod(@Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt);
}
