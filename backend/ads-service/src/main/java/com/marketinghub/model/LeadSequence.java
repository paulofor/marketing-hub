package com.marketinghub.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Association between a lead and a sequence template.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    private SequenceTemplate sequenceTemplate;
}
