package com.marketinghub.repository.jpa.agentmemory;

import com.marketinghub.agentmemory.PremiumAgentMemory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e recuperar memórias premium segregadas dos agentes. */
public interface PremiumAgentMemoryRepository extends JpaRepository<PremiumAgentMemory, Long> {
  /** Localiza conteúdo idêntico no mesmo escopo para deduplicação. */
  Optional<PremiumAgentMemory> findByAgentKeyAndTenantKeyAndScopeTypeAndScopeIdAndContentSha256(
      String agentKey, String tenantKey, String scopeType, String scopeId, String contentSha256);

  /** Recupera somente memórias utilizáveis do agente e escopo solicitados. */
  @Query(
      """
      select m from PremiumAgentMemory m
      where m.agentKey = :agentKey and m.scopeType = :scopeType and m.scopeId = :scopeId
        and m.tenantKey = :tenantKey
        and m.status in ('CONFIRMED', 'CANDIDATE')
        and (m.validUntil is null or m.validUntil > :now)
      order by case when m.status = 'CONFIRMED' then 0 else 1 end,
        m.confidence desc, m.createdAt desc
      """)
  List<PremiumAgentMemory> retrieve(
      @Param("agentKey") String agentKey,
      @Param("tenantKey") String tenantKey,
      @Param("scopeType") String scopeType,
      @Param("scopeId") String scopeId,
      @Param("now") Instant now,
      Pageable pageable);
}
