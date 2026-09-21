package com.marketinghub.financialplan.v1.service.saveplan;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Responsabilidade: congelar premissas econômicas, fontes e limites por cliente e período. */
public record PlanAssumptions(
    @Size(max = 100) String productVersion,
    @Min(1) @Max(3660) @JsonDeserialize(using = WholeNumberDeserializer.class) Integer periodDays,
    @NotNull LocalDate validUntil,
    @NotBlank @Size(max = 6000) String evidence,
    @NotNull @Valid AiCost ai,
    @NotNull @Valid Costs costs,
    @DecimalMin("0.01") @Digits(integer = 9, fraction = 6) BigDecimal priceBrl,
    @DecimalMin("0.01") @DecimalMax("99.99") BigDecimal minimumMarginPercent,
    @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal maximumCacBrl,
    @NotNull @Size(min = 3, max = 3) List<@NotNull @Valid Scenario> scenarios,
    @Valid Preparation preparation,
    @Valid VariableCostEnvelope variableCostEnvelope,
    @Valid FixedCostEnvelope fixedCostEnvelope) {
  /** Escolhas operacionais; suporte não equivale ao período econômico ou ao acesso vendido. */
  public record Preparation(
      @NotNull @Min(1) @Max(3660) @JsonDeserialize(using = WholeNumberDeserializer.class)
          Integer supportDays,
      @NotNull Boolean personalizedAi) {}

  /** Preserva custo variável agregado sem declarar seus componentes desconhecidos como zero. */
  public record VariableCostEnvelope(
      @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal amountPerCustomerBrl,
      @NotNull VariableCostCoverage coverage,
      @NotBlank @Size(max = 1000) String sourceReference,
      @NotNull LocalDate checkedOn) {}

  /** Preserva o custo fixo total do período sem transformar sua composição em valores zero. */
  public record FixedCostEnvelope(
      @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal amountPerPeriodBrl,
      @NotNull FixedCostCoverage coverage,
      @NotBlank @Size(max = 1000) String sourceReference,
      @NotNull LocalDate checkedOn) {}

  /** Premissas do pacote completo: tarifa por tentativa, câmbio documentado e teto por cliente. */
  public record AiCost(
      @NotNull Currency currency,
      @Size(max = 200) String providerModel,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal perAttempt,
      @Size(max = 1000) String pricingSource,
      LocalDate pricingCheckedOn,
      @DecimalMin("0.000001") @Digits(integer = 6, fraction = 6) BigDecimal usdBrl,
      @Size(max = 1000) String exchangeSource,
      @Min(1) @Max(100000) @JsonDeserialize(using = WholeNumberDeserializer.class)
          Integer includedUnits,
      @Min(0) @Max(1000000) @JsonDeserialize(using = WholeNumberDeserializer.class)
          Integer maximumAttempts,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal maximumCostPerCustomerBrl) {}

  /** Componentes financeiros; nulo indica fonte ainda ausente e zero exige premissa explícita. */
  public record Costs(
      @DecimalMin("0") @DecimalMax("100") BigDecimal feePercent,
      @DecimalMin("0") @DecimalMax("100") BigDecimal taxPercent,
      @DecimalMin("0") @DecimalMax("100") BigDecimal commissionPercent,
      @DecimalMin("0") @DecimalMax("100") BigDecimal refundPercent,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal fixedFeeBrl,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal supportBrl,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal storageBrl,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal deliveryBrl,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal otherVariableBrl,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal initialAiBrl,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal initialOtherBrl,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal fixedPerPeriodBrl) {}

  /** Cenário de demanda, aquisição e tentativas cobradas por cliente no mesmo período. */
  public record Scenario(
      @NotNull ScenarioCode code,
      @Min(0) @Max(10000000) @JsonDeserialize(using = WholeNumberDeserializer.class)
          Integer customers,
      @Min(0) @Max(1000000) @JsonDeserialize(using = WholeNumberDeserializer.class)
          Integer attemptsPerCustomer,
      @DecimalMin("0") @Digits(integer = 9, fraction = 6) BigDecimal cacBrl) {}

  /** Moedas aceitas sem câmbio implícito. */
  public enum Currency {
    BRL,
    USD
  }

  /** Cenários declarados; uso intenso é calculado adicionalmente pelo backend. */
  public enum ScenarioCode {
    CONSERVATIVE,
    BASE,
    OPTIMISTIC
  }

  /** Cobertura declarada pelo campo agregado, sem inferir a decomposição interna. */
  public enum VariableCostCoverage {
    ALL_VARIABLE_COSTS_EXCLUDING_CAC
  }

  /** Cobertura declarada pelo custo fixo planejado da versão comercial. */
  public enum FixedCostCoverage {
    ALL_FIXED_OPERATIONAL_COSTS_FOR_PERIOD
  }
}
