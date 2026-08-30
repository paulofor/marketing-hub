package com.marketinghub.repository.jpa.businessprocess;

import com.marketinghub.businessprocess.independent.IndependentBusinessProcessExecution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir a identidade das execuções de processo sem produto. */
public interface IndependentBusinessProcessExecutionRepository
    extends JpaRepository<IndependentBusinessProcessExecution, Long> {

  /** Localiza uma solicitação idempotente antes de criar trabalho operacional. */
  Optional<IndependentBusinessProcessExecution> findByRequestKey(String requestKey);

  /** Lista as execuções materializadas mais recentes para acompanhamento administrativo. */
  List<IndependentBusinessProcessExecution>
      findTop50BySourceReferenceIsNotNullOrderByCreatedAtDescIdDesc();
}
