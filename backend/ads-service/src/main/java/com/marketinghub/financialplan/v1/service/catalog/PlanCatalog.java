package com.marketinghub.financialplan.v1.service.catalog;

import java.util.List;

/** Responsabilidade: fornecer seletores canônicos e relações de produto, tipo e plano comercial. */
public record PlanCatalog(
    List<ProductOption> products, List<Option> productTypes, List<Option> commercialPlans) {
  /** Identidade do produto e seu tipo real, sem inferência pelo nome. */
  public record ProductOption(Long id, String name, Long productTypeId) {}

  /** Identidade e nome de uma referência selecionável. */
  public record Option(Long id, String name) {}
}
