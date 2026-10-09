package com.marketinghub.pde.kit.privateprototype.v1.service;

import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.Presentation;

/** Responsabilidade: explicar o estado real do kit privado e sua continuidade permitida. */
final class KitPrivatePresentation {
  /** Impede a criação de um auxiliar sem estado. */
  private KitPrivatePresentation() {}

  /** Explica somente a condição registrada, mantendo explícita a ausência de causa histórica. */
  static String safetyReason(String code) {
    return switch (code) {
      case "UNVERIFIED_VISUAL_ORIGIN" ->
          "A origem das imagens desta tentativa não foi comprovada. A preparação foi bloqueada antes da composição.";
      case "EXTERNAL_ACTION" ->
          "Esta tentativa pediu publicação, envio ou cobrança fora da prova privada. A ação foi bloqueada antes da composição.";
      default ->
          "O teste de segurança interrompeu a preparação. Esta sessão não registrou qual limite foi acionado; confira a revisão interna.";
    };
  }

  /**
   * Vincula título, explicação e ação interna ao mesmo estado, sem promessa de resultado ausente.
   */
  static Presentation resolve(String status, String safetyCase, Long productId, Long cycleId) {
    String cyclePath =
        "/business-process-chains/learning-cycles?productId=" + productId + "&cycleId=" + cycleId;
    return switch (status) {
      case "READY" ->
          new Presentation(
              "Seu próximo post, pronto para usar",
              "Confira uma aplicação completa: arte, legenda, mensagem e dia do calendário. Depois, guarde o pacote para continuar.",
              "",
              "",
              "",
              "");
      case "QUEUED", "RUNNING" ->
          new Presentation(
              "Seu kit está em preparação",
              "Aguarde a composição desta entrada. Você poderá conferir e guardar os arquivos quando a preparação terminar.",
              "",
              "",
              "",
              "");
      case "BLOCKED_SAFE" ->
          new Presentation(
              "A preparação deste kit foi bloqueada",
              "Nenhum pacote foi gerado nesta tentativa. Sua entrada permanece preservada para a revisão.",
              safetyCase,
              "UNVERIFIED_VISUAL_ORIGIN".equals(safetyCase)
                  ? "Na revisão deste ciclo, confira a origem das imagens e prepare uma entrada compatível. Esta tentativa continuará bloqueada."
                  : "EXTERNAL_ACTION".equals(safetyCase)
                      ? "Volte à revisão deste ciclo para conferir os limites. A prova privada não publica, envia mensagens nem cobra; esta tentativa continuará bloqueada."
                      : "Abra a revisão deste ciclo para identificar o limite acionado e a continuidade permitida. Esta tentativa continuará bloqueada.",
              "Abrir a revisão deste ciclo",
              cyclePath);
      case "FAILED" ->
          new Presentation(
              "A preparação do kit foi interrompida",
              "O pacote desta tentativa não ficou disponível. Sua entrada e o motivo da falha foram preservados.",
              "COMPOSITION_FAILED",
              "Confira a falha na revisão deste ciclo antes de uma nova tentativa.",
              "Abrir a revisão deste ciclo",
              cyclePath);
      default ->
          new Presentation(
              "Prepare o primeiro post do seu kit",
              "Informe o briefing para receber uma aplicação com arte, legenda, mensagem e calendário.",
              "",
              "",
              "",
              "");
    };
  }
}
