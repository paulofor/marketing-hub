package com.marketinghub.funnel;

import com.marketinghub.model.Lead;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response from a lead to a funnel step.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funnel_step_id")
    private FunnelStep funnelStep;

    @Enumerated(EnumType.STRING)
    private ActionType action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "json")
    private String payload;

    private BigDecimal revenue;

    private Instant occurredAt;
}
