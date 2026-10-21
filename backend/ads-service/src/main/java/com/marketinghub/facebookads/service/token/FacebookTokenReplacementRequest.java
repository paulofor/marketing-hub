package com.marketinghub.facebookads.service.token;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Recebe uma credencial somente para escrita, sem incluí-la em diagnósticos do objeto. */
public record FacebookTokenReplacementRequest(
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String accessToken) {
  /** Oculta o segredo inclusive quando o DTO é registrado por infraestrutura de diagnóstico. */
  @Override
  public String toString() {
    return "FacebookTokenReplacementRequest[accessToken=OCULTO]";
  }
}
