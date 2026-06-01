package com.marketinghub.experiment.learning.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.learning.ExperimentLearning;
import com.marketinghub.experiment.learning.ExperimentLearningRequest;
import com.marketinghub.experiment.learning.dto.ExperimentLearningDto;
import com.marketinghub.experiment.learning.dto.ExperimentLearningPayloadDto;
import com.marketinghub.experiment.learning.mapper.ExperimentLearningMapper;
import com.marketinghub.repository.jpa.experiment.learning.ExperimentLearningRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operações de leitura e escrita para os aprendizados automáticos.
 */
@Service
public class ExperimentLearningService {

    private final ExperimentLearningRepository repository;
    private final ExperimentLearningJsonCodec codec;
    private final ExperimentLearningMapper mapper;

    public ExperimentLearningService(ExperimentLearningRepository repository,
                                     ExperimentLearningJsonCodec codec,
                                     ExperimentLearningMapper mapper) {
        this.repository = repository;
        this.codec = codec;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ExperimentLearningDto> listByExperiment(Long experimentId) {
        return repository.findTop5ByExperimentIdOrderByCompletedAtDesc(experimentId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public ExperimentLearning registerResult(ExperimentLearningRequest request,
                                             ExperimentLearningPayloadDto payload) {
        Experiment experiment = request.getExperiment();
        if (experiment == null) {
            throw new EntityNotFoundException("Experimento ausente na solicitação " + request.getId());
        }
        ExperimentLearning learning = ExperimentLearning.builder()
                .experiment(experiment)
                .niche(experiment.getNiche())
                .hypothesis(experiment.getHypothesisRef())
                .request(request)
                .stage(resolveStage(experiment, payload))
                .primaryMetric(payload != null ? payload.getPrimaryMetric() : null)
                .metricSignal(payload != null ? payload.getMetricSignal() : null)
                .summary(payload != null ? payload.getSummary() : null)
                .whatWorked(payload != null ? payload.getWhatWorked() : null)
                .whatBlocked(payload != null ? payload.getWhatBlocked() : null)
                .nextTest(payload != null ? payload.getNextTest() : null)
                .insightsJson(codec.writeInsights(payload != null ? payload.getInsights() : null))
                .suggestionsJson(codec.writeSuggestions(payload != null ? payload.getSuggestions() : null))
                .openAiRequestPayloadJson(codec.writeObject(payload != null ? payload.getOpenAiRequestPayload() : null))
                .completedAt(Instant.now())
                .build();
        return repository.save(learning);
    }

    private ExperimentStage resolveStage(Experiment experiment, ExperimentLearningPayloadDto payload) {
        if (payload != null && payload.getStage() != null) {
            return payload.getStage();
        }
        return experiment.getStage();
    }
}
