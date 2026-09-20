package com.marketinghub.facebookads.resumption.service.result;

import com.fasterxml.jackson.databind.JsonNode;

/** Recebe resultado e evidência estruturada do executor, vinculados à reserva exclusiva. */
public record ResumeCampaignResult(
    String leaseToken, boolean success, String error, JsonNode evidence) {}
