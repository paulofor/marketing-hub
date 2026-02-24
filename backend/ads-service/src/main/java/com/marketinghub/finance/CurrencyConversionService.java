package com.marketinghub.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Utility service to normalize monetary values and convert AI usage costs to BRL.
 */
@Service
public class CurrencyConversionService {
    private final CurrencyConversionProperties properties;

    public CurrencyConversionService(CurrencyConversionProperties properties) {
        this.properties = properties;
    }

    /**
     * Converts a USD-denominated amount to BRL using the configured rate.
     */
    public BigDecimal usdToBrl(BigDecimal usdAmount) {
        if (usdAmount == null) {
            return null;
        }
        if (usdAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rate = properties.getUsdToBrl();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            rate = BigDecimal.ONE;
        }
        BigDecimal converted = usdAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        if (converted.compareTo(BigDecimal.ZERO) == 0 && usdAmount.compareTo(BigDecimal.ZERO) > 0) {
            return new BigDecimal("0.01");
        }
        return converted;
    }

    /**
     * Normalizes a BRL amount to two decimal places using HALF_UP rounding.
     */
    public BigDecimal normalizeBrl(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        if (amount.scale() == 2) {
            return amount;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
