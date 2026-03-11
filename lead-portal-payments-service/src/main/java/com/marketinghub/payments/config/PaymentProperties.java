package com.marketinghub.payments.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payments")
public class PaymentProperties {

    private BigDecimal defaultAmount = new BigDecimal("49.90");
    private String defaultCurrency = "BRL";
    private Duration checkoutTtl = Duration.ofHours(72);
    private List<String> supportedCurrencies = new ArrayList<>(Collections.singletonList("BRL"));

    public BigDecimal getDefaultAmount() {
        return defaultAmount;
    }

    public void setDefaultAmount(BigDecimal defaultAmount) {
        this.defaultAmount = defaultAmount;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public Duration getCheckoutTtl() {
        return checkoutTtl;
    }

    public void setCheckoutTtl(Duration checkoutTtl) {
        this.checkoutTtl = checkoutTtl;
    }

    public List<String> getSupportedCurrencies() {
        return supportedCurrencies;
    }

    public void setSupportedCurrencies(List<String> supportedCurrencies) {
        if (supportedCurrencies == null) {
            this.supportedCurrencies = new ArrayList<>();
        } else {
            this.supportedCurrencies = new ArrayList<>(supportedCurrencies);
        }
    }
}
