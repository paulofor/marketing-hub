package com.marketinghub.agenttask;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: exigir a imagem compatível antes de reservar o planejamento de Atena. */
final class AtenaBpmWorkerContract {
  static final String VERSION = "ATENA_PDE_MARKET_STRATEGY_V1";

  /** Impede criação de uma política sem estado que só descreve o protocolo da fila. */
  private AtenaBpmWorkerContract() {}

  /** Reconhece somente a fila canônica da estratégia, sem bloquear outros módulos. */
  static boolean supports(String agent, String process, String activity) {
    return "experiment-strategist".equals(normalize(agent))
        && "pde-commercial-plan-offer".equals(normalize(process))
        && "marketStrategy".equals(normalize(activity));
  }

  /** Recusa contrato diferente e deixa a imagem legada sem trabalho durante o rollout. */
  static boolean accepts(String contract) {
    String value = normalize(contract);
    if (value == null) return false;
    if (!VERSION.equals(value))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Contrato versionado do worker incompatível com a fila solicitada.");
    return true;
  }

  /** Uniformiza espaços sem transformar ausência em autorização de execução. */
  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
