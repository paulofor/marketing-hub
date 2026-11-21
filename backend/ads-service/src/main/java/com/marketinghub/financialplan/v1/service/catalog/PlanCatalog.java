package com.marketinghub.financialplan.v1.service.catalog;

import com.marketinghub.financialplan.v1.FinancialPlanStructure;
import java.util.List;

/** Responsabilidade: fornecer seletores canônicos e relações de produto, tipo e plano comercial. */
public record PlanCatalog(
    List<ProductOption> products, List<TypeOption> productTypes, List<Option> commercialPlans) {
  /** Identidade do produto e seu tipo real, sem inferência pelo nome. */
  public record ProductOption(Long id, String name, Long productTypeId) {}

  /** Estrutura de planejamento vigente para produtos e modelos deste tipo. */
  public record TypeOption(Long id, String name, FinancialPlanStructure structure) {}

  /** Identidade e nome de uma referência selecionável. */
  public record Option(Long id, String name) {}
}
