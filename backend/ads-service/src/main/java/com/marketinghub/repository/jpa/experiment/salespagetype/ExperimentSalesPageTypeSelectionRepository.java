package com.marketinghub.repository.jpa.experiment.salespagetype;

import com.marketinghub.experiment.salespagetype.ExperimentSalesPageTypeSelection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir os tipos de pagina de venda selecionados por experimento. */
public interface ExperimentSalesPageTypeSelectionRepository
        extends JpaRepository<ExperimentSalesPageTypeSelection, Long> {
    /** Lista as selecoes de um experimento mantendo a ordem de variantes A/B. */
    List<ExperimentSalesPageTypeSelection> findByExperimentIdOrderByVariantKeyAsc(Long experimentId);

    /** Remove as selecoes anteriores antes de gravar uma nova configuracao A/B. */
    void deleteByExperimentId(Long experimentId);
}
