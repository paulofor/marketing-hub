package com.marketinghub.repository.jpa.whatsapp;

import com.marketinghub.whatsapp.WhatsAppAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * JPA repository for WhatsApp accounts.
 */
public interface WhatsAppAccountRepository extends JpaRepository<WhatsAppAccount, Long> {
    Optional<WhatsAppAccount> findFirstByActiveTrue();

    Optional<WhatsAppAccount> findByPhoneNumberId(String phoneNumberId);

    Optional<WhatsAppAccount> findByVerifyToken(String verifyToken);

    @Modifying
    @Query("update WhatsAppAccount a set a.active = false where a.active = true")
    void deactivateAll();

    @Modifying
    @Query("update WhatsAppAccount a set a.active = false where a.id <> :id and a.active = true")
    void deactivateAllExcept(@Param("id") Long id);
}
