package com.example.marketinghub.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * A single step in a message sequence.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SequenceStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer stepOrder;

    @Lob
    private String content;

    /** delay in seconds before sending this step */
    private Integer delaySeconds;

    private String condition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sequence_template_id")
    private SequenceTemplate sequenceTemplate;
}
