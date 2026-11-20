package com.marketinghub.financialplan.v1;

import com.marketinghub.producttype.ProductTypeDefinition;

/** Responsabilidade: associar a unidade de planejamento ao código estável do tipo cadastrado. */
public enum FinancialPlanStructure {
  PER_SALE,
  MONTHLY_SUBSCRIPTION;

  /** Propõe recorrência no consultor PWA e preserva a estrutura por venda dos demais tipos. */
  public static FinancialPlanStructure forType(ProductTypeDefinition type) {
    return type != null && "AI_PWA_CONSULTANT_PRODUCT".equals(type.getCode())
        ? MONTHLY_SUBSCRIPTION
        : PER_SALE;
  }
}
