package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Serviço OPRM responsável por converter custos operacionais em dólar para real sem acoplar o módulo financeiro. */
@Service
public class OprmCurrencyConversionService {
  private final BigDecimal usdToBrlRate;

  /** Inicializa o conversor com a cotação configurada para custos de identificação do OPRM. */
  public OprmCurrencyConversionService(@Value("${app.currency.usd-to-brl:5.0}") BigDecimal usdToBrlRate) {
    this.usdToBrlRate = usdToBrlRate;
  }

  /** Converte um valor em dólar para real mantendo o arredondamento financeiro usado pelo backend. */
  public BigDecimal usdToBrl(BigDecimal usdAmount) {
    if (usdAmount == null) {
      return null;
    }
    if (usdAmount.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    BigDecimal effectiveRate = effectiveUsdToBrlRate();
    BigDecimal converted = usdAmount.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
    if (converted.compareTo(BigDecimal.ZERO) == 0 && usdAmount.compareTo(BigDecimal.ZERO) > 0) {
      return new BigDecimal("0.01");
    }
    return converted;
  }

  /** Retorna a cotação configurada ou uma cotação neutra quando a configuração estiver inválida. */
  private BigDecimal effectiveUsdToBrlRate() {
    if (usdToBrlRate == null || usdToBrlRate.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ONE;
    }
    return usdToBrlRate;
  }
}
