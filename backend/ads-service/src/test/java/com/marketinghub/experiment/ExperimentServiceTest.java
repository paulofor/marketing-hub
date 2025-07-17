package com.marketinghub.experiment;

import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.experiment.service.ExperimentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic tests for {@link ExperimentService}.
 */
@DataJpaTest
@Import(ExperimentService.class)
class ExperimentServiceTest {

    @Autowired
    private ExperimentService service;
    @Autowired
    private ExperimentRepository repository;

    @Test
    void createExperimentPersistsEntity() {
        CreateExperimentRequest req = new CreateExperimentRequest();
        req.setHypothesis("Test hyp");
        req.setKpiGoal(BigDecimal.ONE);
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(7));

        Experiment saved = service.create(req);

        assertThat(repository.findById(saved.getId())).isPresent();
    }
}
