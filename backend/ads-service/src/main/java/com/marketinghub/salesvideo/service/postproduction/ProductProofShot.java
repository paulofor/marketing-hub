package com.marketinghub.salesvideo.service.postproduction;

/** Vincula legenda, captura homologada, leitura e identificação editorial sem alterar a prova. */
public record ProductProofShot(
    String reference, int captionCueIndex, double minimumSeconds, String layout, String label) {
  /** Preserva clientes antigos que deixam o enquadramento sob a regra original da montagem. */
  public ProductProofShot(String reference, int captionCueIndex, double minimumSeconds) {
    this(reference, captionCueIndex, minimumSeconds, null, null);
  }

  /** Preserva clientes que definem enquadramento sem identificação editorial. */
  public ProductProofShot(
      String reference, int captionCueIndex, double minimumSeconds, String layout) {
    this(reference, captionCueIndex, minimumSeconds, layout, null);
  }
}
