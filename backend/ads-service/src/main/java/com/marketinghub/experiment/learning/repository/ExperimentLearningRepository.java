package com.marketinghub.experiment.learning.repository;

import com.marketinghub.experiment.learning.ExperimentLearning;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório com consultas de fácil acesso aos aprendizados registrados.
 */
public interface ExperimentLearningRepository extends JpaRepository<ExperimentLearning, Long> {
    List<ExperimentLearning> findTop5ByExperimentIdOrderByCompletedAtDesc(Long experimentId);

    List<ExperimentLearning> findTop50ByNicheIdOrderByCompletedAtDesc(Long nicheId);
}
