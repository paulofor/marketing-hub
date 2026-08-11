package com.marketinghub.opportunitydossier.service.status;

import com.marketinghub.opportunitydossier.OpportunityDossierStatus;

/** Responsabilidade: receber uma transição governada do dossiê. */
public record UpdateOpportunityStatusRequest(OpportunityDossierStatus status, String decidedBy) {}
