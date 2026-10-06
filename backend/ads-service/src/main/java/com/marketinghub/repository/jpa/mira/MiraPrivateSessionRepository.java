package com.marketinghub.repository.jpa.mira;

import com.marketinghub.pde.mira.privateprototype.v1.MiraPrivateSession;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

/** Responsabilidade: serializar o uso dos pacotes privados e recuperar provas no banco canônico. */
public interface MiraPrivateSessionRepository extends JpaRepository<MiraPrivateSession, String> {
  /** Reserva o pacote durante alterações para impedir consumo concorrente duplicado. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<MiraPrivateSession> findBySessionHash(String hash);

  /** Reserva a revogação para não sobrescrever um resultado gravado simultaneamente. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from MiraPrivateSession s where s.id = :id")
  Optional<MiraPrivateSession> findLockedById(
      @org.springframework.data.repository.query.Param("id") String id);

  /** Lista somente as provas do ciclo solicitado. */
  List<MiraPrivateSession> findByCycleIdOrderByCreatedAtAsc(Long cycleId);
}
