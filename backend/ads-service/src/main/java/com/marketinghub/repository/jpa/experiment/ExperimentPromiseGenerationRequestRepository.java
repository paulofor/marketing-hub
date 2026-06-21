package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.promise.ExperimentPromiseGenerationRequest;
import com.marketinghub.experiment.promise.ExperimentPromiseGenerationRequestStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e consultar solicitações de geração de promessa de experimento. */
public interface ExperimentPromiseGenerationRequestRepository extends JpaRepository<ExperimentPromiseGenerationRequest, Long> {
    /** Lista solicitações por status em ordem de criação para consumo pelo endpoint pending. */
    List<ExperimentPromiseGenerationRequest> findByStatusOrderByCreatedAtAsc(
            ExperimentPromiseGenerationRequestStatus status,
            Pageable pageable);

    /** Busca a solicitação mais recente em status retomável para a tela recuperar pelo backend. */
    Optional<ExperimentPromiseGenerationRequest> findFirstByStatusInOrderByCreatedAtDesc(
            Collection<ExperimentPromiseGenerationRequestStatus> statuses);

    /** Soma o custo em dólar de solicitações concluídas para compor o custo inicial do experimento criado. */
    @Query("""
            select coalesce(sum(r.costUsd), 0)
            from ExperimentPromiseGenerationRequest r
            where r.id in :ids
              and r.status = com.marketinghub.experiment.promise.ExperimentPromiseGenerationRequestStatus.COMPLETED
            """)
    BigDecimal sumCompletedCostUsdByIdIn(@Param("ids") Collection<Long> ids);
}
