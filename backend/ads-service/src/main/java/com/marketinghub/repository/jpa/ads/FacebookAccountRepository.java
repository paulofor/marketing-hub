package com.marketinghub.repository.jpa.ads;

import com.marketinghub.ads.FacebookAccount;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Repositório JPA responsável pela persistência de FacebookAccount. */
public interface FacebookAccountRepository extends JpaRepository<FacebookAccount, Long> {
  /** Localiza a conta habilitada para o executor de anúncios. */
  Optional<FacebookAccount> findFirstByWorkerEnabledTrue();

  /** Troca somente o token da configuração verificada, sem sobrescrever renovação concorrente. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query(
      """
      update fb_account a set a.accessToken = :candidate, a.tokenExpiresAt = :expiresAt,
        a.tokenLastRefreshedAt = :validatedAt, a.tokenRenewalStatus = 'NEVER_ATTEMPTED',
        a.tokenRenewalLastError = null, a.tokenRenewalLastAttemptAt = null
      where a.id = :id and (a.accessToken = :previous or (a.accessToken is null and :previous is null))
        and a.appId = :appId and a.appSecret = :appSecret and a.adAccountId = :adAccountId
        and (a.authorizedUserId = :userId or (a.authorizedUserId is null and :userId is null))
      """)
  int replaceVerifiedToken(
      @Param("id") Long id,
      @Param("previous") String previous,
      @Param("appId") String appId,
      @Param("appSecret") String appSecret,
      @Param("adAccountId") String adAccountId,
      @Param("userId") String userId,
      @Param("candidate") String candidate,
      @Param("expiresAt") LocalDateTime expiresAt,
      @Param("validatedAt") LocalDateTime validatedAt);
}
