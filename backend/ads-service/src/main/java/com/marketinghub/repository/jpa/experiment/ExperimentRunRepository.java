package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.run.ExperimentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositório JPA responsável por persistir execuções operacionais de experimentos.
 */
public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {
    /** Lista os runs de um experimento na ordem operacional. */
    List<ExperimentRun> findByExperimentIdOrderByRunNumberAsc(Long experimentId);

    /** Retorna o maior número de run já criado para um experimento. */
    @Query("select coalesce(max(run.runNumber), 0) from ExperimentRun run where run.experiment.id = :experimentId")
    int findMaxRunNumberByExperimentId(@Param("experimentId") Long experimentId);
}
