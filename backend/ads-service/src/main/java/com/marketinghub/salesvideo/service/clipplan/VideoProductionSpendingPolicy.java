package com.marketinghub.salesvideo.service.clipplan;

import com.marketinghub.salesvideo.VideoProject;

/** Responsabilidade: consultar impedimentos financeiros antes de permitir consumo de vídeo. */
public interface VideoProductionSpendingPolicy {
  /** Retorna a causa persistida do bloqueio ou ausência de impedimento deste contrato. */
  String paidExecutionBlocker(VideoProject project);

  /** Confere o handshake antes de entregar tarefas a uma imagem antiga do executor. */
  default boolean workerCanConsume(VideoProject project) {
    return true;
  }

  /** Impede substituir a rota governada por outro provedor na entrada administrativa. */
  default String requestBlocker(VideoProject project, String providerName) {
    return paidExecutionBlocker(project);
  }

  /**
   * Confere entrada privada de Íris e decisão independente de Plutus sem aprovação editorial
   * fictícia.
   */
  default boolean privateScriptAllowed(Long profileId, Long scriptId) {
    return false;
  }

  /** Apresenta o controle integral na tela sem deduzir saldo de um snapshot de clipes. */
  default java.util.Map<String, Object> report(VideoProject project) {
    return java.util.Map.of();
  }

  /** Confere a fonte autorizada da captura funcional sem aprová-la como peça comercial. */
  default void validateProductRecording(
      VideoProject project, com.fasterxml.jackson.databind.JsonNode manifest) {
    throw new IllegalStateException("Fonte da gravação funcional não autorizada neste contexto.");
  }

  /** Inclui a reserva real dos clipes no teto comum antes de qualquer agente financeiro pago. */
  default String holdProviderCredits(
      VideoProject project,
      com.marketinghub.salesvideo.VideoProductionCycle cycle,
      java.math.BigDecimal maximumUsd) {
    String blocker = paidExecutionBlocker(project);
    return blocker;
  }

  /** Consulta autoridade de consumo do job sem aceitar contexto enviado pelo navegador. */
  default com.marketinghub.videocontract.v1.BoundedSpendContracts.Context jobContext(
      Long jobId, String purpose) {
    throw new IllegalStateException("Política de consumo do job indisponível.");
  }

  /** Reserva uma chamada finita no mesmo teto de agentes e revisões. */
  default com.marketinghub.videocontract.v1.BoundedSpendContracts.Lease reserveJob(
      Long jobId,
      String purpose,
      com.marketinghub.videocontract.v1.BoundedSpendContracts.Reserve input) {
    throw new IllegalStateException("Reserva de consumo do job indisponível.");
  }

  /** Adquire o direito de uma chamada concreta antes de enviar dados ao provedor. */
  default com.marketinghub.videocontract.v1.BoundedSpendContracts.Lease beginJob(
      Long jobId, String purpose, Long reservationId) {
    throw new IllegalStateException("Admissão de consumo do job indisponível.");
  }

  /** Preserva resposta ou incerteza sem liberar o custo máximo da chamada. */
  default com.marketinghub.videocontract.v1.BoundedSpendContracts.Lease recordJob(
      Long jobId,
      String purpose,
      Long reservationId,
      com.marketinghub.videocontract.v1.BoundedSpendContracts.Result input) {
    throw new IllegalStateException("Auditoria de consumo do job indisponível.");
  }
}
