package com.marketinghub.product.executionprofile.v1.service.getprofile;

import com.marketinghub.product.executionprofile.v1.service.saveprofile.ProfileContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: apresentar ficha, percurso, decisões e consumo sem inferência no frontend. */
public record ProfileView(
    Long id,
    Long productId,
    int revision,
    Long chainId,
    Long commercialPlanId,
    int commercialPlanVersion,
    String productType,
    String productFormat,
    ProfileContract contract,
    String routeName,
    List<Work> work,
    List<ProcessReference> processes,
    List<ScenarioResult> economics,
    List<String> blockers,
    List<Review> reviews,
    List<Binding> bindings,
    List<Consumption> consumption,
    Instant createdAt,
    String createdBy) {
  /** Subatividade especializada com responsável, critério e vínculo ao processo canônico. */
  public record Work(
      String processCode,
      String activityId,
      String name,
      String owner,
      List<String> requirements,
      boolean applicable,
      String applicabilityReason) {}

  /** Referência exata que permite ida e retorno sem selecionar outra versão publicada. */
  public record ProcessReference(
      Long id, String code, int version, String name, String parentCode) {}

  /** Projeção financeira por pacote, explicitamente distinta de uma venda medida. */
  public record ScenarioResult(
      String code,
      BigDecimal revenueBrl,
      BigDecimal fullCostBrl,
      BigDecimal contributionBrl,
      boolean viable) {}

  /** Decisão rastreável ao parecer de Plutus e à pessoa que a registrou. */
  public record Review(
      String checkpoint,
      Long financialExecutionId,
      String executionStatus,
      boolean approved,
      String decision,
      String reviewedBy,
      String rationale,
      Instant createdAt) {}

  /** Contexto congelado que adotou a ficha. */
  public record Binding(Long id, String sourceReference, Long learningCycleId, String createdBy) {}

  /** Consumo auditado, conservando separadamente reserva e custo efetivo. */
  public record Consumption(
      Long id,
      Long bindingId,
      String operationKey,
      String usageKey,
      int units,
      BigDecimal reservedBrl,
      BigDecimal actualBrl,
      String status,
      String evidence,
      boolean testData,
      Instant createdAt) {}
}
