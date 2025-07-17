package com.marketinghub.experiment.service;

import com.marketinghub.experiment.*;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for experiments.
 */
@Service
public class ExperimentService {
    private final ExperimentRepository repository;

    public ExperimentService(ExperimentRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates and stores a new experiment.
     */
    @Transactional
    public Experiment create(CreateExperimentRequest request) {
        Experiment exp = Experiment.builder()
                .hypothesis(request.getHypothesis())
                .kpiGoal(request.getKpiGoal())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ExperimentStatus.PLANNED)
                .build();
        return repository.save(exp);
    }

    public Experiment get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<Experiment> list() {
        return repository.findAll();
    }
}
