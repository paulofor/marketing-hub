package com.marketinghub.leadportal;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Individual question that composes a lead portal flow.
 */
@Entity
@Table(name = "lead_portal_flow_question")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadPortalFlowQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flow_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private LeadPortalFlow flow;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "data_key", nullable = false, length = 120)
    private String dataKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LeadPortalQuestionType type;

    @Column(nullable = false)
    private boolean required;

    @Column(length = 500)
    private String description;

    @Column(length = 255)
    private String placeholder;

    @Column(name = "position_index", nullable = false)
    private int position;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "lead_portal_flow_question_option", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "option_order")
    @Column(name = "value", length = 255)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private List<String> options = new ArrayList<>();
}
