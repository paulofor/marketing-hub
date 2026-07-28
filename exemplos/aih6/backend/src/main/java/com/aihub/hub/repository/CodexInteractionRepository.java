package com.aihub.hub.repository;

import com.aihub.hub.domain.CodexInteractionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** Repositório responsável por consultas e persistência das interações das solicitações Codex. */
public interface CodexInteractionRepository extends JpaRepository<CodexInteractionRecord, Long> {

    boolean existsBySandboxInteractionId(String sandboxInteractionId);

    int countByCodexRequestId(Long codexRequestId);

    List<CodexInteractionRecord> findAllByCodexRequestIdOrderBySequenceAscIdAsc(Long codexRequestId);

    @Query("SELECT i.codexRequest.id, COUNT(i) FROM CodexInteractionRecord i WHERE i.codexRequest.id IN :requestIds GROUP BY i.codexRequest.id")
    List<Object[]> countByCodexRequestIds(@Param("requestIds") Collection<Long> requestIds);

    /** Conta interações vinculadas a solicitações criadas em um intervalo operacional. */
    @Query("SELECT COUNT(i) FROM CodexInteractionRecord i WHERE i.codexRequest.createdAt >= :startsAt AND i.codexRequest.createdAt < :endsAt")
    long countByRequestCreatedAtPeriod(@Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt);
}
