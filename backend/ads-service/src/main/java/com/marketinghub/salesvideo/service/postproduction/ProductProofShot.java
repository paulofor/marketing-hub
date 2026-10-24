package com.marketinghub.salesvideo.service.postproduction;

/** Vincula legenda, captura homologada, leitura e enquadramento opcional sem alterar a prova. */
public record ProductProofShot(
    String reference, int captionCueIndex, double minimumSeconds, String layout) {
  /** Preserva clientes antigos que deixam o enquadramento sob a regra original da montagem. */
  public ProductProofShot(String reference, int captionCueIndex, double minimumSeconds) {
    this(reference, captionCueIndex, minimumSeconds, null);
  }
}
