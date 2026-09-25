package com.marketinghub.facebookads.resumption.service.result;

import com.fasterxml.jackson.databind.JsonNode;

/** Recebe resultado e evidência estruturada do executor, vinculados à reserva exclusiva. */
public record ResumeCampaignResult(
    String leaseToken,
    boolean success,
    String error,
    JsonNode evidence,
    CampaignReplacementResult replacement) {

  /** Preserva o contrato anterior para retomadas que mantêm a mesma campanha física. */
  public ResumeCampaignResult(String leaseToken, boolean success, String error, JsonNode evidence) {
    this(leaseToken, success, error, evidence, null);
  }
}
