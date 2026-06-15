package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a conversão monetária local do OPRM sem dependência do módulo financeiro. */
class OprmCurrencyConversionServiceTest {

  /** Deve converter dólares para reais usando a cotação configurada e arredondamento financeiro. */
  @Test
  void shouldConvertUsdToBrlUsingConfiguredRate() {
    OprmCurrencyConversionService service = new OprmCurrencyConversionService(new BigDecimal("5.0"));

    BigDecimal converted = service.usdToBrl(new BigDecimal("0.0473"));

    assertThat(converted).isEqualByComparingTo(new BigDecimal("0.24"));
  }

  /** Deve preservar um centavo mínimo quando custo positivo arredondaria para zero. */
  @Test
  void shouldPreserveMinimumCentForPositiveCost() {
    OprmCurrencyConversionService service = new OprmCurrencyConversionService(new BigDecimal("5.0"));

    BigDecimal converted = service.usdToBrl(new BigDecimal("0.0001"));

    assertThat(converted).isEqualByComparingTo(new BigDecimal("0.01"));
  }

  /** Deve usar cotação neutra quando a configuração recebida for inválida. */
  @Test
  void shouldUseNeutralRateWhenConfiguredRateIsInvalid() {
    OprmCurrencyConversionService service = new OprmCurrencyConversionService(BigDecimal.ZERO);

    BigDecimal converted = service.usdToBrl(new BigDecimal("2.345"));

    assertThat(converted).isEqualByComparingTo(new BigDecimal("2.35"));
  }
}
