package com.marketinghub.repository.jpa.experiment.salespageab;

import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbTest;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbTestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar planos de teste A/B de pagina de venda persistidos. */
public interface ExperimentSalesPageAbTestRepository extends JpaRepository<ExperimentSalesPageAbTest, Long> {
    /** Lista os testes A/B de um experimento em ordem recente. */
    List<ExperimentSalesPageAbTest> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);

    /** Busca o teste A/B ativo ou pronto mais recente de um experimento. */
    Optional<ExperimentSalesPageAbTest> findTopByExperimentIdAndStatusInOrderByUpdatedAtDesc(
            Long experimentId,
            List<ExperimentSalesPageAbTestStatus> statuses);
}
