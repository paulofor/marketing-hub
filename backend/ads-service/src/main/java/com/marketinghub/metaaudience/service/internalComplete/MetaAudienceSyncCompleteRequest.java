package com.marketinghub.metaaudience.service.internalComplete;

/** Contrato interno usado pelo worker para registrar o resultado da sincronização da audiência. */
public record MetaAudienceSyncCompleteRequest(String facebookAudienceId, long syncedContacts, String status, String errorMessage) {}
