package com.marketinghub.repository.jpa.ads;

import com.marketinghub.ads.FacebookAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de FacebookAccount. */
public interface FacebookAccountRepository extends JpaRepository<FacebookAccount, Long> {
  Optional<FacebookAccount> findFirstByWorkerEnabledTrue();
}
