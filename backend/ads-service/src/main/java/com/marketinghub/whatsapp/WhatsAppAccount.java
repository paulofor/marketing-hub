package com.marketinghub.whatsapp;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Meta WhatsApp Cloud account configuration persisted in the platform.
 */
@Entity
@Table(name = "whatsapp_account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "phone_number_id", nullable = false, unique = true)
    private String phoneNumberId;

    @Column(name = "business_account_id")
    private String businessAccountId;

    @Column(name = "access_token", columnDefinition = "LONGTEXT")
    private String accessToken;

    @Column(name = "verify_token")
    private String verifyToken;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "active")
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
