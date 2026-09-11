package com.marketinghub.repository.jpa.vega;

import com.marketinghub.pde.vega.privateprototype.v1.VegaPrivateSession;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

/** Responsabilidade: persistir leituras privadas e serializar as ações de cada participante. */
public interface VegaPrivateSessionRepository extends JpaRepository<VegaPrivateSession, String> {
  /** Resolve uma sessão pelo hash sem persistir a credencial em claro. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<VegaPrivateSession> findBySessionHash(String hash);

  /** Troca um convite pelo acesso à própria leitura. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<VegaPrivateSession> findByGrantHash(String hash);

  /** Lista as leituras do ciclo no banco, sem misturar a experiência comercial. */
  List<VegaPrivateSession> findByCycleIdOrderByCreatedAtAsc(Long cycleId);

  /** Protege a criação de convites humanos da mesma leitura dentro de um ciclo. */
  boolean existsByCycleIdAndPrototypeVersionAndOriginAndReadingNumberAndRevokedFalse(
      Long cycleId, String version, String origin, Integer readingNumber);
}
