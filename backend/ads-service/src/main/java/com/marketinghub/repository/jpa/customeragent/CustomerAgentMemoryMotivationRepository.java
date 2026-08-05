package com.marketinghub.repository.jpa.customeragent;

import com.marketinghub.customeragent.CustomerAgentMemoryMotivation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir o historico append-only dos vetores motivacionais. */
public interface CustomerAgentMemoryMotivationRepository
    extends JpaRepository<CustomerAgentMemoryMotivation, Long> {
  /** Lista vetores da persona na ordem auditavel mais recente. */
  List<CustomerAgentMemoryMotivation> findByPersonaIdOrderByCreatedAtDesc(Long personaId);
}
