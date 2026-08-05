package com.marketinghub.repository.jpa.customeragent;

import com.marketinghub.customeragent.CustomerAgentMemoryEvidence;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar metadados canônicos das evidencias pesadas do Agente Cliente. */
public interface CustomerAgentMemoryEvidenceRepository
    extends JpaRepository<CustomerAgentMemoryEvidence, Long> {
  /** Localiza evidencia identica da mesma persona e camada para evitar duplicacao no S3. */
  Optional<CustomerAgentMemoryEvidence> findByPersonaIdAndMemoryLayerAndSha256(
      Long personaId, String memoryLayer, String sha256);

  /** Lista evidencias de uma persona em ordem auditavel. */
  List<CustomerAgentMemoryEvidence> findByPersonaIdOrderByCreatedAtDesc(Long personaId);
}
