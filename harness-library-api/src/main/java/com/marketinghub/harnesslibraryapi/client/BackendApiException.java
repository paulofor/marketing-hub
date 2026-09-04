package com.marketinghub.harnesslibraryapi.client;

import org.springframework.http.HttpStatus;

/** Transporta ao contrato público apenas status e mensagem seguros da falha do backend. */
public class BackendApiException extends RuntimeException {
  private final HttpStatus publicStatus;

  /** Cria uma falha já traduzida para o status que a API externa pode revelar. */
  public BackendApiException(HttpStatus publicStatus, String message, Throwable cause) {
    super(message, cause);
    this.publicStatus = publicStatus;
  }

  /** Informa o status sanitizado a ser devolvido ao cliente. */
  public HttpStatus getPublicStatus() {
    return publicStatus;
  }
}
