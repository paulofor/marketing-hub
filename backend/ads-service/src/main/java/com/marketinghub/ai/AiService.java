package com.marketinghub.ai;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing an AI service used in the marketing plan.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Lob
    private String objective;

    private String url;

    /** Phase of the marketing process where this service is used. */
    private String phase;

    private BigDecimal price;

    private BigDecimal cost;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
