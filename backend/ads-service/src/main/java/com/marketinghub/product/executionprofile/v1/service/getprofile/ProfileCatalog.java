package com.marketinghub.product.executionprofile.v1.service.getprofile;

import java.util.List;

/**
 * Responsabilidade: oferecer opções válidas de ficha sem o frontend inferir o percurso pelo
 * mineral.
 */
public record ProfileCatalog(
    List<Option> capabilities,
    List<Option> chains,
    List<Option> commercialPlans,
    String imageModel,
    List<Option> checkpoints) {
  /** Opção já validada pelo backend para seleção administrativa. */
  public record Option(String code, String name) {}
}
