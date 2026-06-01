package com.marketinghub.repository.jpa.whatsapp;

import com.marketinghub.whatsapp.WhatsAppMessage;
import com.marketinghub.whatsapp.WhatsAppMessageDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * JPA repository for WhatsApp messages.
 */
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
    Optional<WhatsAppMessage> findByMessageId(String messageId);

    Page<WhatsAppMessage> findByDirectionOrderByCreatedAtDesc(WhatsAppMessageDirection direction, Pageable pageable);

    Page<WhatsAppMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
