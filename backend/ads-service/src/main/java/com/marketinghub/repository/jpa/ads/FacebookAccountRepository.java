package com.marketinghub.repository.jpa.ads;

import com.marketinghub.ads.FacebookAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório JPA responsável pela persistência de FacebookAccount.
 */
public interface FacebookAccountRepository extends JpaRepository<FacebookAccount, Long> {
    Optional<FacebookAccount> findFirstByWorkerEnabledTrue();
}
