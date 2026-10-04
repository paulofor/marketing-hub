package com.marketinghub.creative.service.videoreview;

/** Distingue decisões humanas disponíveis, bloqueios e registros preservados para auditoria. */
public enum VideoReviewState {
  AWAITING_REVIEW,
  BLOCKED,
  HISTORICAL,
  APPROVED,
  REJECTED
}
