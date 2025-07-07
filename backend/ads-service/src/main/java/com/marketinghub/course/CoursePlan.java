package com.marketinghub.course;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entity representing a course plan proposed by Kajabi AI.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String targetAudience;
    private String transformation;

    @Lob
    private String macroTopics;

    @Lob
    private String modules;

    @Lob
    private String objectives;

    @Lob
    private String resources;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
