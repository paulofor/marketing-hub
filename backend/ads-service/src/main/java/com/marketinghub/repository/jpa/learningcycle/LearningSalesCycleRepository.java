package com.marketinghub.repository.jpa.learningcycle;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir ciclos segregados por produto e serializar suas decisões. */
public interface LearningSalesCycleRepository extends JpaRepository<LearningSalesCycle, Long> {
  /** Lista o histórico do produto em ordem cronológica inversa. */
  List<LearningSalesCycle> findByProductIdOrderByIdDesc(Long productId);

  /** Filtra uma cadeia no SQL preservando todas as versões anteriores do mesmo código. */
  List<LearningSalesCycle> findByProductIdAndChainCodeOrderByIdDesc(
      Long productId, String chainCode);

  /** Retoma no SQL somente o ciclo aberto do próprio produto e da mesma cadeia. */
  Optional<LearningSalesCycle> findFirstByProductIdAndChainCodeAndOpenSlot(
      Long productId, String chainCode, Integer openSlot);

  /** Localiza a adoção anterior para responder idempotentemente. */
  Optional<LearningSalesCycle> findByProductIdAndRequestKey(Long productId, String requestKey);

  /** Localiza o contexto explícito de um experimento sem escolher outro pela data. */
  Optional<LearningSalesCycle> findByExperimentId(Long experimentId);

  /** Lista ciclos abertos do produto para tarefas privadas criadas durante a iteração. */
  List<LearningSalesCycle> findByProductIdAndOpenSlot(Long productId, Integer openSlot);

  /** Impede que duas iterações compartilhem o mesmo experimento. */
  boolean existsByExperimentId(Long experimentId);

  /** Impede duas iterações abertas da mesma cadeia para um produto. */
  boolean existsByProductIdAndChainCodeAndOpenSlot(
      Long productId, String chainCode, Integer openSlot);

  /** Localiza o sucessor único sem inferir por data ou status comercial. */
  Optional<LearningSalesCycle> findByPreviousCycleId(Long previousCycleId);

  /** Bloqueia o ciclo do próprio produto durante uma transição atômica. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from LearningSalesCycle c where c.id = :id and c.productId = :productId")
  Optional<LearningSalesCycle> findLocked(@Param("productId") Long productId, @Param("id") Long id);
}
