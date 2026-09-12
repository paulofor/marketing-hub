package com.marketinghub.repository.jpa.learningcycle;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar comandos imutáveis e suas evidências por ciclo. */
public interface LearningSalesCycleEventRepository
    extends JpaRepository<LearningSalesCycleEvent, Long> {
  /** Lista somente as transições do ciclo solicitado, na ordem em que ocorreram. */
  List<LearningSalesCycleEvent> findByCycleIdOrderByRevisionAsc(Long cycleId);

  /** Filtra recibos de uma operação financeira no banco, preservando a ordem de substituição. */
  List<LearningSalesCycleEvent> findByCycleIdAndActionOrderByRevisionDesc(
      Long cycleId, String action);

  /** Lê o último resultado sem perder bloqueios cuja próxima instância já foi aberta. */
  Optional<LearningSalesCycleEvent> findFirstByCycleIdOrderByRevisionDesc(Long cycleId);

  /** Localiza o recibo de um comando para impedir efeitos duplicados. */
  Optional<LearningSalesCycleEvent> findByCycleIdAndRequestKey(Long cycleId, String requestKey);
}
