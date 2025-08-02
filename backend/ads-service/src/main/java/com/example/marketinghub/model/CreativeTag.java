package com.example.marketinghub.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tag applied to creatives for segmentation.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreativeTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
