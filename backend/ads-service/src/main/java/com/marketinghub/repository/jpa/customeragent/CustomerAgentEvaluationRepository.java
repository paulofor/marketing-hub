package com.marketinghub.repository.jpa.customeragent;

import com.marketinghub.customeragent.CustomerAgentEvaluation;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar avaliacoes auditaveis do Agente Cliente. */
public interface CustomerAgentEvaluationRepository
    extends JpaRepository<CustomerAgentEvaluation, Long> {
  /** Busca avaliacoes pendentes para o executor. */
  List<CustomerAgentEvaluation> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
