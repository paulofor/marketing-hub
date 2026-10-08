package com.marketinghub.repository.jpa.kit;

import com.marketinghub.pde.kit.privateprototype.v1.KitPrivateSession;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: consultar sessões privadas sem compartilhar acessos entre produtos. */
public interface KitPrivateSessionRepository extends JpaRepository<KitPrivateSession, String> {
  /** Resolve exclusivamente o hash do acesso opaco apresentado. */
  Optional<KitPrivateSession> findBySessionHash(String hash);

  /** Recupera a emissão idempotente do ciclo exato. */
  Optional<KitPrivateSession> findByCycleIdAndRequestKey(Long cycleId, String requestKey);

  /** Serializa ações e transferências da mesma sessão. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from KitPrivateSession s where s.sessionHash = :hash")
  Optional<KitPrivateSession> locked(@Param("hash") String hash);

  /** Entrega a auditoria segregada por ciclo sem consultar outro experimento. */
  List<KitPrivateSession> findByCycleIdOrderByCreatedAtAsc(Long cycleId);
}
