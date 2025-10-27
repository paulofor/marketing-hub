package com.marketinghub.whatsapp;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Audit trail for WhatsApp messages exchanged through the platform.
 */
@Entity
@Table(name = "whatsapp_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private WhatsAppAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private WhatsAppMessageDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private WhatsAppMessageType messageType;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "from_number")
    private String fromNumber;

    @Column(name = "to_number")
    private String toNumber;

    @Column(name = "status")
    private String status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "text_body", columnDefinition = "LONGTEXT")
    private String textBody;

    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Column(name = "image_id")
    private String imageId;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "caption", columnDefinition = "LONGTEXT")
    private String caption;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "context_json", columnDefinition = "LONGTEXT")
    private String contextJson;

    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "status_payload_json", columnDefinition = "LONGTEXT")
    private String statusPayloadJson;

    @Column(name = "message_timestamp")
    private Instant messageTimestamp;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
