package com.marketinghub.repository.jpa.agentmemory;

import com.marketinghub.agentmemory.PremiumAgentMemoryFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir feedback independente sobre memórias premium. */
public interface PremiumAgentMemoryFeedbackRepository
    extends JpaRepository<PremiumAgentMemoryFeedback, Long> {}
