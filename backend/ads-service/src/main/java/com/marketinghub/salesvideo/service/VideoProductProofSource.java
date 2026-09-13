package com.marketinghub.salesvideo.service;

import java.util.Optional;

/**
 * Porta de leitura de prova técnica, sem expor persistência ou serviços de outro módulo ao vídeo.
 */
public interface VideoProductProofSource {
  /** Localiza metadados imutáveis da captura e do resultado técnico proprietário. */
  Optional<Proof> find(Long taskId, Long evidenceId);

  /** Lê somente os pixels da captura já validada pelo contexto de vídeo. */
  byte[] read(Long taskId, Long evidenceId);

  /**
   * Transporta identidade e resultado persistidos sem depender das entidades do módulo de agentes.
   */
  record Proof(
      Long taskId,
      Long evidenceId,
      String status,
      String sourceReference,
      String resultJson,
      String sha256) {}
}
