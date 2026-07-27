package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.ExperimentAudienceTest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório JPA responsável pela persistência dos testes de público do experimento. */
public interface ExperimentAudienceTestRepository
    extends JpaRepository<ExperimentAudienceTest, Long> {
  /** Lista os testes de público com itens e elementos carregados para montar a tela. */
  @Query(
      """
            select distinct test from ExperimentAudienceTest test
            left join fetch test.items item
            left join fetch item.targetingElement element
            where test.experiment.id = :experimentId
            order by test.createdAt desc
            """)
  List<ExperimentAudienceTest> findByExperimentIdWithItems(
      @Param("experimentId") Long experimentId);
}
