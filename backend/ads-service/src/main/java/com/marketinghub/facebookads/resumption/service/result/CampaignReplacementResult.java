package com.marketinghub.facebookads.resumption.service.result;

import java.math.BigDecimal;
import java.util.List;

/** Descreve a campanha física substituta criada pela Meta sem trocar o experimento comercial. */
public record CampaignReplacementResult(
    String sourceCampaignId,
    String sourceAdSetId,
    String campaignId,
    String adSetId,
    Long lifetimeBudgetMinor,
    BigDecimal confirmedPriorSpend,
    List<ReplacementAdResult> ads) {

  /** Vincula cada anúncio antigo ao anúncio equivalente da campanha substituta. */
  public record ReplacementAdResult(String sourceAdId, String adId) {}
}
