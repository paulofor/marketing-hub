package com.marketinghub.repository.jpa.agentmonitor;

import com.marketinghub.agentmonitor.AgentAutomaticExecutionControlEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir a trilha append-only das mudanças PLAY/STOP dos agentes. */
public interface AgentAutomaticExecutionControlEventRepository
    extends JpaRepository<AgentAutomaticExecutionControlEvent, Long> {}
