package com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Responsabilidade: receber premissas monetárias e decisão explícita da mesma planilha. */
public record CostWorksheetRequest(
    UUID requestKey,
    Long expectedRevision,
    Long chainDefinitionId,
    Long experimentId,
    String productVersion,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal expectedRevenueBrl,
    BigDecimal usdBrl,
    String exchangeSource,
    LocalDate exchangeCheckedOn,
    String operatorName,
    String justification,
    boolean approveOperatingBudgets,
    List<Line> lines) {
  /** Responsabilidade: declarar totais por grupo sem confundir custo, orçamento e previsão. */
  public record Line(
      String code,
      BigDecimal planned,
      BigDecimal reportedCost,
      BigDecimal forecast,
      String source) {}
}
