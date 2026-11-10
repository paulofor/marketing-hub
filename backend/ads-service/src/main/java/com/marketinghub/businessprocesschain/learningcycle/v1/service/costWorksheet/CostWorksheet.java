package com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: apresentar a planilha calculada e suas fontes sem conceder autorização. */
public record CostWorksheet(
    String contractVersion,
    Long eventId,
    long revision,
    boolean editable,
    CostWorksheetRequest saved,
    List<Row> rows,
    List<Total> totals,
    BigDecimal projectedResultBrl,
    List<String> gaps,
    List<Receipt> history,
    List<CeilingReceipt> ceilingHistory) {
  /** Responsabilidade: expor cada grupo canônico e a origem da aprovação. */
  public record Row(
      String code,
      String label,
      String currency,
      String basis,
      String description,
      BigDecimal planned,
      BigDecimal reportedCost,
      BigDecimal estimatedCost,
      BigDecimal approved,
      String approvalStatus,
      BigDecimal forecast,
      BigDecimal variance,
      String source,
      String estimateSource,
      boolean canApprove,
      String approvalBlocker,
      String approvalScope,
      String executionBlocker) {}

  /** Responsabilidade: apresentar valor, autoria e vigência de uma decisão individual de teto. */
  public record CeilingReceipt(
      Long eventId,
      String code,
      BigDecimal approved,
      String operatorName,
      Instant recordedAt,
      String justification,
      boolean current,
      Long sourceCycleId,
      boolean carriedForward) {}

  /** Responsabilidade: manter subtotal conhecido e cobertura por moeda. */
  public record Total(
      String currency,
      BigDecimal planned,
      BigDecimal reportedCost,
      BigDecimal forecast,
      int missingPlanned,
      int missingReported,
      int missingForecast) {}

  /** Responsabilidade: preservar o histórico da planilha com premissas recuperáveis. */
  public record Receipt(
      Long eventId,
      long revision,
      Instant recordedAt,
      String operatorName,
      String productVersion,
      boolean current,
      boolean operatingBudgetsApproved,
      CostWorksheetRequest snapshot) {}
}
