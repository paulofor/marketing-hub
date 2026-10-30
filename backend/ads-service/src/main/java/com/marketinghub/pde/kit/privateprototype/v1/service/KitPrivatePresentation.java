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
          "A preparação foi interrompida por segurança. Esta tentativa não registrou qual limite foi acionado; o motivo precisa ser conferido pela equipe do produto.";
    };
  }

  /**
   * Explica quem resolve a pendência e mantém a continuação na experiência, sem delegar controles
   * administrativos à cliente nem prometer um resultado ausente.
   */
  static Presentation resolve(String status, String safetyCase, Long productId, Long cycleId) {
    return switch (status) {
      case "READY" ->
          new Presentation(
              "Seu próximo post, pronto para usar",
              "Confira uma aplicação completa: arte, legenda, mensagem e dia do calendário. Depois, guarde o pacote para continuar.",
              "Seu kit está pronto. Copie a legenda ou baixe os arquivos.",
              "",
              "",
              "",
              "");
      case "QUEUED", "RUNNING" ->
          new Presentation(
              "Seu kit está em preparação",
              "Estamos preparando as artes e os textos com os dados informados.",
              "Aguarde um momento. Seu kit aparecerá aqui quando estiver pronto.",
              "",
              "",
              "",
              "");
      case "BLOCKED_SAFE" ->
          new Presentation(
              "A preparação deste kit foi bloqueada",
              "Nenhum pacote foi gerado nesta tentativa. Seus dados continuam guardados.",
              safetyReason(safetyCase),
              safetyCase,
              "UNVERIFIED_VISUAL_ORIGIN".equals(safetyCase)
                  ? "A verificação da origem das imagens cabe à equipe do produto. Você não precisa alterar seus dados nem conferir a biblioteca. Esta tentativa permanece bloqueada e não disponibiliza um pacote."
                  : "EXTERNAL_ACTION".equals(safetyCase)
                      ? "Esta demonstração não publica, envia mensagens nem cobra. A equipe do produto é responsável por conferir qualquer ação fora desses limites. Nenhuma correção dos seus dados libera esta tentativa; ela permanece bloqueada."
                      : "A equipe do produto precisa conferir o motivo antes de permitir uma nova preparação. Você não precisa reenviar seus dados. Esta tentativa permanece bloqueada.",
              "UNVERIFIED_VISUAL_ORIGIN".equals(safetyCase)
                  ? "Entender a verificação das imagens"
                  : "Entender o limite desta demonstração",
              "#kit-continuation");
      case "FAILED" ->
          new Presentation(
              "A preparação do kit foi interrompida",
              "O pacote desta tentativa não ficou disponível. Sua entrada e o motivo da falha foram preservados.",
              "Não foi possível preparar este kit. Seus dados estão guardados para retomar com segurança.",
              "COMPOSITION_FAILED",
              "A equipe do produto precisa conferir a interrupção antes de uma nova preparação. Você não precisa reenviar seus dados; esta tentativa ainda não disponibiliza um pacote.",
              "Entender como continuar",
              "#kit-continuation");
      default ->
          new Presentation(
              "Prepare o primeiro post do seu kit",
              "Escolha os dados de exemplo para conferir uma arte, sua legenda e a divulgação da semana.",
              "",
              "",
              "",
              "",
              "");
    };
  }
}
