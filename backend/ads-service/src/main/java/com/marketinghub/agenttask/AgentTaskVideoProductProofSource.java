package com.marketinghub.agenttask;

import com.marketinghub.repository.jpa.agenttask.AgentTaskVisualEvidenceRepository;
import com.marketinghub.salesvideo.service.VideoProductProofSource;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapta a prova técnica privada ao contrato de leitura do vídeo sem criar ou aprovar evidências.
 */
@Component
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class AgentTaskVideoProductProofSource implements VideoProductProofSource {
  private final AgentTaskVisualEvidenceRepository repository;
  private final AgentTaskVisualEvidenceService service;

  /** Conecta os repositórios e o storage pertencentes ao próprio módulo de agentes. */
  public AgentTaskVideoProductProofSource(
      AgentTaskVisualEvidenceRepository repository, AgentTaskVisualEvidenceService service) {
    this.repository = repository;
    this.service = service;
  }

  /** Traduz metadados da captura e da tarefa para a porta de prova técnica do vídeo. */
  @Override
  public Optional<Proof> find(Long taskId, Long evidenceId) {
    return repository
        .findByIdAndTaskId(evidenceId, taskId)
        .map(
            proof ->
                new Proof(
                    proof.getTask().getId(),
                    proof.getId(),
                    proof.getTask().getStatus(),
                    proof.getTask().getSourceReference(),
                    proof.getTask().getResultJson(),
                    proof.getSha256()));
  }

  /** Reutiliza o leitor privado existente sem permitir acesso direto do executor ao storage. */
  @Override
  public byte[] read(Long taskId, Long evidenceId) {
    return service.read(taskId, evidenceId).bytes();
  }
}
