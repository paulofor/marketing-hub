package com.marketinghub.opportunitydossier.service.evidence;

/** Responsabilidade: receber uma evidência verificável do dossiê. */
public record AddOpportunityEvidenceRequest(String sourceUrl, String summary, String createdBy) {}
