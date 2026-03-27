package com.marketinghub.experiment.learning.web;

import com.marketinghub.experiment.learning.dto.ExperimentLearningDto;
import com.marketinghub.experiment.learning.service.ExperimentLearningService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposição dos aprendizados estruturados de um experimento específico.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/learnings")
public class ExperimentLearningController {

    private final ExperimentLearningService service;

    public ExperimentLearningController(ExperimentLearningService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExperimentLearningDto> list(@PathVariable Long experimentId) {
        return service.listByExperiment(experimentId);
    }
}
