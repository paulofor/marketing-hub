package com.marketinghub.salesvideo.service.postproduction;

/** Vincula um trecho da legenda a um recorte de captura homologada e ao tempo mínimo de leitura. */
public record ProductProofShot(String reference, int captionCueIndex, double minimumSeconds) {}
