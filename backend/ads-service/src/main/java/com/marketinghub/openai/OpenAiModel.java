package com.marketinghub.openai;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Catalog entry for OpenAI text/embedding models with pricing details.
 */
@Entity
@Table(name = "openai_model")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 128)
    private String code;

    @Column(name = "price_input_standard", nullable = false, precision = 12, scale = 5)
    private BigDecimal priceInputStandard;

    @Column(name = "price_input_cached_standard", nullable = false, precision = 12, scale = 5)
    private BigDecimal priceInputCachedStandard;

    @Column(name = "price_output_standard", nullable = false, precision = 12, scale = 5)
    private BigDecimal priceOutputStandard;

    @Column(name = "price_input_batch", nullable = false, precision = 12, scale = 5)
    private BigDecimal priceInputBatch;

    @Column(name = "price_input_cached_batch", nullable = false, precision = 12, scale = 5)
    private BigDecimal priceInputCachedBatch;

    @Column(name = "price_output_batch", nullable = false, precision = 12, scale = 5)
    private BigDecimal priceOutputBatch;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
