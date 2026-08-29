package com.marketinghub.pde.harness.v1.consultant;

/** Identifica os dois canais canônicos de consultoria reutilizáveis pelos PDEs. */
public enum PdeConsultantChannel {
  PWA("AI_PWA_CONSULTANT_PRODUCT", true),
  WHATSAPP("AI_SANDBOX_CONVERSATIONAL_PRODUCT", false);

  private final String productTypeCode;
  private final boolean reactExperienceRequired;

  /** Vincula o canal ao tipo do catálogo e à necessidade de experiência React própria. */
  PdeConsultantChannel(String productTypeCode, boolean reactExperienceRequired) {
    this.productTypeCode = productTypeCode;
    this.reactExperienceRequired = reactExperienceRequired;
  }

  /** Devolve o código estável do tipo de produto correspondente. */
  public String productTypeCode() {
    return productTypeCode;
  }

  /** Informa se o canal exige a camada React do SDK para atender o cliente. */
  public boolean reactExperienceRequired() {
    return reactExperienceRequired;
  }
}
