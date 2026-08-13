package com.marketinghub.repository.jpa.agent;

import com.marketinghub.agentmonitor.CodexAuthReconnect;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Responsabilidade: persistir e localizar solicitações auditáveis de reconexão Codex. */
public interface CodexAuthReconnectRepository extends JpaRepository<CodexAuthReconnect, Long> {
  /** Localiza a solicitação mais recente de um agente. */
  Optional<CodexAuthReconnect> findTopByAgentIdOrderByRequestedAtDesc(Long agentId);

  /** Localiza solicitações ainda não reservadas pelo executor. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<CodexAuthReconnect> findTop1ByAgentAgentKeyAndStatusOrderByRequestedAtAsc(
      String agentKey, String status);

  /** Detecta uma reconexão concorrente ainda ativa. */
  boolean existsByAgentIdAndStatusIn(Long agentId, List<String> statuses);
}
