package com.marketinghub.experiment.service.publicationhistory;

import java.time.Instant;

/** Expõe a referência datada de publicação sem transportar a entidade interna de Facebook Ads. */
public record HistoricalCampaignReceipt(String campaignId, Instant recordedAt) {}
