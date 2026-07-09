package com.marketinghub.repository.jpa.experiment.salespageab;

import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: acessar variantes de teste A/B de pagina de venda persistidas. */
public interface ExperimentSalesPageAbVariantRepository extends JpaRepository<ExperimentSalesPageAbVariant, Long> {
    /** Lista as variantes de um teste A/B. */
    List<ExperimentSalesPageAbVariant> findByTestIdOrderByVariantKeyAsc(Long testId);

    /** Busca uma variante garantindo que pertence ao experimento informado. */
    Optional<ExperimentSalesPageAbVariant> findByIdAndTestExperimentId(Long id, Long experimentId);
}
