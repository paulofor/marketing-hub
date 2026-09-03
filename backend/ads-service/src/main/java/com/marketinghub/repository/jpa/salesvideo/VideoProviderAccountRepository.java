package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.VideoProviderAccount;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: acessar contas agregadoras e serializar suas reservas financeiras. */
public interface VideoProviderAccountRepository extends JpaRepository<VideoProviderAccount, Long> {
  /** Localiza a conta pelo identificador estável de configuração. */
  Optional<VideoProviderAccount> findByAccountKey(String accountKey);

  /** Bloqueia a linha da conta durante atualização de snapshot ou reserva de créditos. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select account from VideoProviderAccount account where account.accountKey = :accountKey")
  Optional<VideoProviderAccount> findByAccountKeyForUpdate(@Param("accountKey") String accountKey);

  /** Bloqueia diretamente a conta vinculada ao ciclo antes de consultar reservas concorrentes. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select account from VideoProviderAccount account, VideoProviderPreflight preflight "
          + "where preflight.videoProductionCycleId = :cycleId and preflight.providerAccountId = account.id")
  Optional<VideoProviderAccount> findByVideoProductionCycleIdForUpdate(
      @Param("cycleId") Long cycleId);

  /** Lista as contas agregadoras para o monitor financeiro administrativo. */
  List<VideoProviderAccount> findAllByOrderByDisplayNameAsc();
}
