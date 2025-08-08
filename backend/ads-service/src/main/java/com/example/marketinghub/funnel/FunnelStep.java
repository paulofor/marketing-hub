package com.example.marketinghub.funnel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Step within a sales funnel.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunnelStep {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funnel_id")
    @JsonIgnore
    private SalesFunnel funnel;

    private Integer orderIdx;

    @Enumerated(EnumType.STRING)
    private StimulusType stimulusType;

    private String channel;
    private String templateId;

    @Enumerated(EnumType.STRING)
    private ActionType expectedAction;

    private Integer scoreInc;

    private BigDecimal revenueTarget;

    @CreationTimestamp
    private Instant createdAt;

    private Boolean isActive;
}
