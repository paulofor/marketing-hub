package com.marketinghub.safira.commercial.v1.service;

/** Responsabilidade: levar a pendência Safira até a tela que altera sua fonte canônica. */
final class SafiraCommercialBlocker extends IllegalStateException {
  private final String actionLabel;
  private final String navigationUrl;

  /** Preserva o motivo do bloqueio e o destino já vinculado ao contexto validado. */
  SafiraCommercialBlocker(String reason, String actionLabel, String navigationUrl) {
    super(reason);
    this.actionLabel = actionLabel;
    this.navigationUrl = navigationUrl;
  }

  /** Expõe a ação concreta sem transformar orientação em autorização. */
  String actionLabel() {
    return actionLabel;
  }

  /** Expõe somente a rota relativa montada a partir das entidades já conferidas. */
  String navigationUrl() {
    return navigationUrl;
  }

  /** Recusa o requisito ausente e conserva sua ação de correção na mesma candidata. */
  static void requireAt(boolean valid, String reason, String actionLabel, String navigationUrl) {
    if (!valid) throw new SafiraCommercialBlocker(reason, actionLabel, navigationUrl);
  }
}
