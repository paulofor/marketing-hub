package com.marketinghub.chat;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Stored ChatGPT dialog link.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDialog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;

    @Lob
    private String description;

    private String theme;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

