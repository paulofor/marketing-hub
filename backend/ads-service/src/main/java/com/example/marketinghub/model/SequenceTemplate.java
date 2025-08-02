package com.example.marketinghub.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Template defining a message sequence.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SequenceTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Lob
    private String content;
}
