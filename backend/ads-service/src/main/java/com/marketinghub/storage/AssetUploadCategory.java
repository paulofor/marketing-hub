package com.marketinghub.storage;

import java.util.Locale;

/** Categorias reconhecidas para upload de arquivos no storage compartilhado. */
public enum AssetUploadCategory {
  EXPERIMENT_CREATIVE("experiments/creatives"),
  LANDING_PAGE_IMAGE("experiments/landing-images"),
  LEAD_PORTAL_FORM("lead-portal/forms"),
  PRODUCT_VIDEO_IMAGE("products/video-images"),
  SALES_VIDEO("sales-videos"),
  AGENT_PORTRAIT("agents/portraits"),
  COMMERCIAL_PLAN_DELIVERABLE("commercial-plans/deliverables"),
  GENERIC("uploads");

  private final String rootFolder;

  /** Associa a categoria ao diretório versionado correspondente. */
  AssetUploadCategory(String rootFolder) {
    this.rootFolder = rootFolder;
  }

  /** Retorna o diretório raiz seguro da categoria. */
  public String getRootFolder() {
    return rootFolder;
  }

  /** Resolve uma chave externa ou retorna a categoria genérica por compatibilidade. */
  public static AssetUploadCategory fromKey(String raw) {
    if (raw == null || raw.isBlank()) {
      return GENERIC;
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    for (AssetUploadCategory category : values()) {
      if (category.name().equals(normalized)) {
        return category;
      }
    }
    return GENERIC;
  }
}
