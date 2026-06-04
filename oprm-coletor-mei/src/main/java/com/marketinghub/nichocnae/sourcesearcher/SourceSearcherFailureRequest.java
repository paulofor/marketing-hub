package com.marketinghub.nichocnae.sourcesearcher;

/** Representa a mensagem de falha enviada ao backend quando a busca pública não conclui. */
public record SourceSearcherFailureRequest(String errorMessage) {}
