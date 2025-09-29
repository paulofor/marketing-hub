package com.marketinghub.ads;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacebookAccountRepository extends JpaRepository<FacebookAccount, Long> {
    Optional<FacebookAccount> findFirstByWorkerEnabledTrue();
}
