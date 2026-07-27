package com.marketinghub.repository.jpa.experiment.salespagetype;

import com.marketinghub.experiment.salespagetype.ExperimentSalesPageTypeSelection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir os tipos de pagina de venda selecionados por experimento. */
public interface ExperimentSalesPageTypeSelectionRepository
    extends JpaRepository<ExperimentSalesPageTypeSelection, Long> {
  /** Lista as selecoes de um experimento mantendo a ordem de variantes A/B. */
  List<ExperimentSalesPageTypeSelection> findByExperimentIdOrderByVariantKeyAsc(Long experimentId);

  /** Verifica se o experimento selecionou um tipo ativo de pagina de venda. */
  @Query(
      """
            select case when count(selection) > 0 then true else false end
            from ExperimentSalesPageTypeSelection selection
            where selection.experiment.id = :experimentId
              and selection.salesPageType.code = :salesPageTypeCode
              and selection.active = true
            """)
  boolean existsByExperimentIdAndSalesPageTypeCodeAndActiveTrue(
      @Param("experimentId") Long experimentId,
      @Param("salesPageTypeCode") String salesPageTypeCode);

  /** Remove as selecoes anteriores antes de gravar uma nova configuracao A/B. */
  void deleteByExperimentId(Long experimentId);
}
