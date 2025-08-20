package com.marketinghub.funnel;

import com.marketinghub.model.Lead;
import jakarta.persistence.*;
import lombok.*;

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

    @Lob
    @Column(name = "payload", columnDefinition = "LONGTEXT")
    private String payload;

    private BigDecimal revenue;

    private Instant occurredAt;
}
