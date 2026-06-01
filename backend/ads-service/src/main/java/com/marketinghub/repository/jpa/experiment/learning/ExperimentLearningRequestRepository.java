package com.marketinghub.repository.jpa.experiment.learning;

import com.marketinghub.experiment.learning.ExperimentLearningRequest;
import com.marketinghub.experiment.learning.ExperimentLearningStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório das solicitações de aprendizado de experimento.
 */
public interface ExperimentLearningRequestRepository extends JpaRepository<ExperimentLearningRequest, Long> {
    List<ExperimentLearningRequest> findTop5ByExperimentIdOrderByRequestedAtDesc(Long experimentId);

    boolean existsByExperimentIdAndStatusIn(Long experimentId, Collection<ExperimentLearningStatus> statuses);

    List<ExperimentLearningRequest> findByStatusInOrderByRequestedAtAsc(Collection<ExperimentLearningStatus> statuses);

    List<ExperimentLearningRequest> findAllByOrderByRequestedAtDesc();
}
