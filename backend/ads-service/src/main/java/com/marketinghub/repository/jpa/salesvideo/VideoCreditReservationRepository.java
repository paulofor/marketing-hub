package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.VideoCreditReservation;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: acessar a reserva financeira única associada a cada ciclo de vídeo. */
public interface VideoCreditReservationRepository
    extends JpaRepository<VideoCreditReservation, Long> {
  /** Localiza a reserva financeira do ciclo informado. */
  Optional<VideoCreditReservation> findByVideoProductionCycleId(Long cycleId);

  /** Lê a versão corrente da reserva depois que a conta compartilhada foi travada. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select reservation from VideoCreditReservation reservation where reservation.videoProductionCycleId = :cycleId")
  Optional<VideoCreditReservation> findByVideoProductionCycleIdForUpdate(
      @Param("cycleId") Long cycleId);

  /** Lista reservas ainda não consumidas que venceram para uma conta já travada. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<VideoCreditReservation> findByProviderAccountIdAndStatusAndExpiresAtLessThanEqual(
      Long providerAccountId, String status, Instant expiresAt);
}
