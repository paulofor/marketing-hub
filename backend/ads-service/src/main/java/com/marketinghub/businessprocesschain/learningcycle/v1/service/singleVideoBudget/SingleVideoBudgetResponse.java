package com.marketinghub.businessprocesschain.learningcycle.v1.service.singleVideoBudget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Expõe o recibo integral sem apresentá-lo como prontidão de produção ou parecer de agente. */
public record SingleVideoBudgetResponse(
    Long productId,
    Long chainDefinitionId,
    Long cycleId,
    Long experimentId,
    String productVersion,
    long revision,
    String scope,
    String currency,
    boolean canAuthorize,
    String blocker,
    boolean startsProduction,
    String guidance,
    List<Authorization> authorizations) {
  /** Identifica um único projeto, incluindo todas as suas tentativas e revisões no mesmo teto. */
  public record Authorization(
      Long eventId,
      String reference,
      Long videoProjectId,
      Long profileId,
      BigDecimal totalLimitUsd,
      String operatorName,
      String authorizationSource,
      Instant authorizedAt,
      boolean current) {}
}
