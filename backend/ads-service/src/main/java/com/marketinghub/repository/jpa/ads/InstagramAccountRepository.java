package com.marketinghub.repository.jpa.ads;

import com.marketinghub.ads.InstagramAccount;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de InstagramAccount.
 */
public interface InstagramAccountRepository extends JpaRepository<InstagramAccount, Long> {
}
