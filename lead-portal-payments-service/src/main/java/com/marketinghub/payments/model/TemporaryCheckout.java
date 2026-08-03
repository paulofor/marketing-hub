package com.marketinghub.payments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Persiste a troca temporária de checkout e seu destino comercial de restauração. */
@Entity
@Table(name = "temporary_checkout")
public class TemporaryCheckout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_key", nullable = false, unique = true, length = 120)
    private String productKey;

    @Column(name = "product_name", nullable = false, length = 180)
    private String productName;

    @Column(name = "commercial_checkout_url", nullable = false, length = 1200)
    private String commercialCheckoutUrl;

    @Column(name = "temporary_checkout_url", nullable = false, length = 1200)
    private String temporaryCheckoutUrl;

    @Column(name = "mercado_pago_preference_id", nullable = false, length = 150)
    private String mercadoPagoPreferenceId;

    @Column(name = "test_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal testAmount;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "restored_at")
    private Instant restoredAt;

    public Long getId() { return id; }
    public String getProductKey() { return productKey; }
    public void setProductKey(String value) { productKey = value; }
    public String getProductName() { return productName; }
    public void setProductName(String value) { productName = value; }
    public String getCommercialCheckoutUrl() { return commercialCheckoutUrl; }
    public void setCommercialCheckoutUrl(String value) { commercialCheckoutUrl = value; }
    public String getTemporaryCheckoutUrl() { return temporaryCheckoutUrl; }
    public void setTemporaryCheckoutUrl(String value) { temporaryCheckoutUrl = value; }
    public String getMercadoPagoPreferenceId() { return mercadoPagoPreferenceId; }
    public void setMercadoPagoPreferenceId(String value) { mercadoPagoPreferenceId = value; }
    public BigDecimal getTestAmount() { return testAmount; }
    public void setTestAmount(BigDecimal value) { testAmount = value; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant value) { activatedAt = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { expiresAt = value; }
    public Instant getRestoredAt() { return restoredAt; }
    public void setRestoredAt(Instant value) { restoredAt = value; }
}
