package com.marketinghub.repository.jpa.hypothesis;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório JPA responsável pela persistência de Hypothesis. */
public interface HypothesisRepository extends JpaRepository<Hypothesis, UUID> {
  /** Bloqueia a raiz da linhagem durante a reserva do próximo número de versão. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select h from Hypothesis h where h.id = :id")
  Optional<Hypothesis> findByIdForVersioning(@Param("id") UUID id);

  /** Retorna o maior número já usado na linhagem para criar a próxima versão. */
  @Query(
      "select coalesce(max(h.versionNumber), 1) from Hypothesis h where h.rootHypothesis.id = :rootId or h.id = :rootId")
  Integer findMaximumVersionNumber(@Param("rootId") UUID rootId);

  List<Hypothesis> findByMarketNicheId(Long marketNicheId);

  List<Hypothesis> findByMarketNicheIdAndStatus(Long marketNicheId, HypothesisStatus status);

  List<Hypothesis> findByStatus(HypothesisStatus status);

  /** Lista hipóteses por status com paginação executada pelo banco. */
  Page<Hypothesis> findByStatus(HypothesisStatus status, Pageable pageable);

  long countByMarketNicheId(Long marketNicheId);

  @Modifying
  @Query(
      """
            update Hypothesis h
            set h.totalCost = coalesce(h.totalCost, 0) + :delta
            where h.id = :id
            """)
  void incrementTotalCost(@Param("id") UUID id, @Param("delta") BigDecimal delta);
}
