package com.marketinghub.salesvideo.service.postproduction;

/** Seleciona uma amostra aprovada para um trecho narrado, sem aceitar URL ou hash do cliente. */
public record DeliveryExampleShot(Long visualAssetId, int captionCueIndex, String headline) {
  /** Preserva a seleção simples, usando a fala quando não houver título editorial explícito. */
  public DeliveryExampleShot(Long visualAssetId, int captionCueIndex) {
    this(visualAssetId, captionCueIndex, null);
  }
}
