package com.marketinghub.repository.jpa.kit;

import com.marketinghub.pde.kit.privateprototype.v1.KitPrivateArtifact;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: reservar composições e ler sua fila sem carregar os arquivos ZIP. */
public interface KitPrivateArtifactRepository extends JpaRepository<KitPrivateArtifact, String> {
  /** Reutiliza somente entrada, fixture e ciclo rigorosamente iguais. */
  Optional<KitPrivateArtifact> findByCycleIdAndFixtureOwnerAndInputSha256(
      Long cycleId, String owner, String hash);

  /** Conta reservas duráveis antes de autorizar composição. */
  long countByCycleId(Long cycleId);

  /** Limita a recuperação a duas composições por fixture. */
  long countByCycleIdAndFixtureOwner(Long cycleId, String owner);

  /** Confere resultado aceito da identidade exata sem carregar o pacote no registro da prova. */
  boolean existsByCycleIdAndProductIdAndExperimentIdAndPrototypeVersionAndProfileCodeAndStatus(
      Long cycleId,
      Long productId,
      Long experimentId,
      String version,
      String profile,
      String status);

  /**
   * Lê somente os manifestos aceitos da identidade exata, sem carregar ZIPs ou dados de entrada.
   */
  @Query(
      "select a.manifestJson from KitPrivateArtifact a where a.cycleId = :cycleId and a.productId = :productId and a.experimentId = :experimentId and a.prototypeVersion = :version and a.profileCode = :profile and a.status = 'READY'")
  List<String> acceptedManifests(
      @Param("cycleId") Long cycleId,
      @Param("productId") Long productId,
      @Param("experimentId") Long experimentId,
      @Param("version") String version,
      @Param("profile") String profile);

  /** Seleciona trabalho enfileirado cujo ciclo continua aberto, sem hidratar artefatos. */
  @Query(
      "select a.id from KitPrivateArtifact a, LearningSalesCycle c where a.cycleId = c.id and c.status = 'OPEN' and a.status = 'QUEUED' order by a.createdAt")
  List<String> pending(Pageable page);

  /** Serializa claim e callback para impedir resultados duplicados. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from KitPrivateArtifact a where a.id = :id")
  Optional<KitPrivateArtifact> locked(@Param("id") String id);

  /** Lê a posição operacional sem retransmitir ZIP, entrada ou prompt ao resumo do ciclo. */
  @Query(
      "select a.status as status, a.createdAt as createdAt, a.startedAt as startedAt, a.finishedAt as finishedAt, a.zipSha256 as zipSha256 from KitPrivateArtifact a where a.cycleId = :cycleId order by a.createdAt")
  List<Summary> summaries(@Param("cycleId") Long cycleId);

  /** Responsabilidade: expor somente estado, horários e identidade do resultado utilizável. */
  interface Summary {
    /** Retorna o estado persistido da composição. */
    String getStatus();

    /** Retorna quando a composição foi solicitada. */
    Instant getCreatedAt();

    /** Retorna quando o executor assumiu a composição. */
    Instant getStartedAt();

    /** Retorna quando a tentativa encerrou. */
    Instant getFinishedAt();

    /** Retorna o hash do pacote íntegro, quando existente. */
    String getZipSha256();
  }
}
