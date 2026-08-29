package com.marketinghub.repository.jpa.agenttask;

import com.marketinghub.agenttask.AgentTaskVisualEvidence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e localizar snapshots visuais segregados por tarefa e sessão. */
public interface AgentTaskVisualEvidenceRepository
    extends JpaRepository<AgentTaskVisualEvidence, Long> {
  /** Localiza a captura idempotente dentro de uma única tarefa e sessão. */
  Optional<AgentTaskVisualEvidence> findByTaskIdAndCaptureSessionIdAndEvidenceKey(
      Long taskId, String captureSessionId, String evidenceKey);

  /** Localiza o conteúdo somente quando ele pertence à tarefa solicitada. */
  Optional<AgentTaskVisualEvidence> findByIdAndTaskId(Long id, Long taskId);
}
