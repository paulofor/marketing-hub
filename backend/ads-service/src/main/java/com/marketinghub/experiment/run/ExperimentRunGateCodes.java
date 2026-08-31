package com.marketinghub.experiment.run;

/** Responsabilidade: centralizar os códigos persistidos dos gates canônicos de um run. */
public final class ExperimentRunGateCodes {
  public static final String LANDING_QUALITY_REVIEW_APPROVED = "LANDING_QUALITY_REVIEW_APPROVED";
  public static final String FORM_CAN_BE_SUBMITTED = "FORM_CAN_BE_SUBMITTED";
  public static final String CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED =
      "CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED";
  public static final String META_EFFECTIVE_STATUS_CONFIRMED = "META_EFFECTIVE_STATUS_CONFIRMED";
  public static final String DIRECT_CHANNEL_READINESS_CONFIRMED =
      "DIRECT_CHANNEL_READINESS_CONFIRMED";
  public static final String DATA_FRESHNESS_VALID = "DATA_FRESHNESS_VALID";

  /** Impede instanciação de uma classe que representa somente o vocabulário canônico. */
  private ExperimentRunGateCodes() {}
}
