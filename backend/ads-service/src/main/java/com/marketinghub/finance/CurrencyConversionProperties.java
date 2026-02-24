package com.marketinghub.finance;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Holds configurable conversion rates for monetary values.
 */
@Component
@ConfigurationProperties(prefix = "app.currency")
@Getter
@Setter
public class CurrencyConversionProperties {
    /**
     * Conversion rate used to translate USD-denominated AI costs into BRL.
     */
    private BigDecimal usdToBrl = new BigDecimal("5.0");
}
