package com.marketinghub.product.executionprofile.v1.service.saveprofile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Responsabilidade: declarar o contrato imutável de entrega e economia de uma versão do produto.
 */
public record ProfileContract(
    @NotBlank @Size(max = 100) String productVersion,
    @NotNull Capability capability,
    @NotBlank @Size(max = 2000) String purchasedOutcome,
    @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 500) String> inputs,
    @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 500) String> deliverables,
    @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 500) String> qualityCriteria,
    @NotBlank @Size(max = 200) String deliveryMode,
    @NotBlank @Size(max = 200) String revenueModel,
    boolean audiovisualRequired,
    @Min(1) @Max(1000) int includedUnits,
    @Min(1) @Max(5000) int maximumAttempts,
    @NotBlank @Size(max = 100) String costModel,
    @NotBlank @Size(max = 100) String pricingRevision,
    @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0.000001") BigDecimal usdBrl,
    @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0") BigDecimal maximumAttemptCostBrl,
    @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0") BigDecimal maximumDeliveryCostBrl,
    @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0.01")
        BigDecimal minimumContributionBrl,
    @NotNull @Valid ProductionBudget productionBudget,
    @NotNull @Size(min = 3, max = 3) List<@NotNull @Valid Scenario> scenarios) {

  public static final String NO_VARIABLE_AI_COST = "NO_VARIABLE_AI_COST";

  /** Orçamento privado de produção por execução, independente do custo de cada venda. */
  public record ProductionBudget(
      @NotBlank @Size(max = 100) String costModel,
      @NotNull @Min(0) @Max(5000) Integer maximumAttempts,
      @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0") BigDecimal maximumAttemptCostBrl,
      @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0")
          BigDecimal maximumTotalCostBrl) {}

  /** Capacidade escolhida pelo contrato de entrega, independente do mineral do catálogo. */
  public enum Capability {
    PERSONALIZED_IMAGES,
    GUIDED_EXPERIENCE,
    AI_TOOL,
    DIGITAL_PACKAGE
  }

  /** Cenário de economia por pacote completo, com custos declarados na mesma moeda. */
  public record Scenario(
      @NotNull ScenarioCode code,
      @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0.01") BigDecimal priceBrl,
      @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0") BigDecimal acquisitionBrl,
      @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0") BigDecimal feesBrl,
      @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0") BigDecimal supportBrl,
      @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0") BigDecimal storageDeliveryBrl,
      @NotNull @Digits(integer = 6, fraction = 6) @DecimalMin("0") BigDecimal otherCostsBrl) {}

  /** Identifica os três cenários obrigatórios sem confundir projeção com resultado medido. */
  public enum ScenarioCode {
    FAVORABLE,
    BASE,
    CONSERVATIVE
  }
}
