package com.marketinghub.experiment.service;

import com.marketinghub.experiment.*;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer for experiments.
 */
@Service
public class ExperimentService {
    private final ExperimentRepository repository;
    private final MarketNicheRepository nicheRepository;
    private final com.marketinghub.hypothesis.repository.HypothesisRepository hypothesisRepository;
    private final MetricPresetService metricPresetService;
    private final EntityManager entityManager;

    public ExperimentService(ExperimentRepository repository, MarketNicheRepository nicheRepository,
                             com.marketinghub.hypothesis.repository.HypothesisRepository hypothesisRepository,
                             MetricPresetService metricPresetService,
                             EntityManager entityManager) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.metricPresetService = metricPresetService;
        this.entityManager = entityManager;
    }

    /**
     * Obtains a managed reference to {@link MarketNiche} without hitting the database.
     * getReference() avoids {@code detached entity passed to persist} by associating
     * the proxy with the current persistence context.
     *
     * @throws EntityNotFoundException if the id does not exist
     */
    private MarketNiche attachNiche(Long nicheId) {
        if (!nicheRepository.existsById(nicheId)) {
            throw new EntityNotFoundException("MarketNiche not found: " + nicheId);
        }
        return entityManager.getReference(MarketNiche.class, nicheId);
    }

    private com.marketinghub.hypothesis.Hypothesis attachHypothesis(java.util.UUID id) {
        if (!hypothesisRepository.existsById(id)) {
            throw new EntityNotFoundException("Hypothesis not found: " + id);
        }
        return entityManager.getReference(com.marketinghub.hypothesis.Hypothesis.class, id);
    }

    /**
     * Creates and stores a new experiment.
     */
    @Transactional
    public Experiment create(Long nicheId, CreateExperimentRequest request) {
        MarketNiche niche = attachNiche(nicheId);
        if (request.getHypothesisId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hypothesisId required");
        }
        com.marketinghub.hypothesis.Hypothesis hyp = attachHypothesis(request.getHypothesisId());
        if (!hyp.getMarketNiche().getId().equals(nicheId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hypothesis and experiment niche mismatch");
        }
        if (request.getStartDate() != null && request.getEndDate() != null &&
                request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }
        if (repository.existsByNicheAndName(niche, request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name already exists for niche");
        }
        if (request.getKpiTargetCpl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kpiTargetCpl required");
        }
        if (request.getMetricPresetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "metricPresetId required");
        }
        MetricPreset preset = metricPresetService.get(request.getMetricPresetId());
        java.math.BigDecimal computedStopLoss = request.getKpiTargetCpl().multiply(preset.getStopLossFactor());
        if (request.getSampleSize() != null && request.getSampleSize() < 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sampleSize must be at least 100");
        }
        if (request.getBaselineCvr() != null && request.getTargetCvr() != null &&
                request.getBaselineCvr().compareTo(request.getTargetCvr()) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baselineCvr must be < targetCvr");
        }
        Experiment exp = Experiment.builder()
                .niche(niche)
                .name(request.getName())
                .hypothesis(request.getHypothesis())
                .hypothesisRef(hyp)
                .kpiTargetCpl(request.getKpiTargetCpl())
                .metricPreset(preset)
                .stopLossCpl(computedStopLoss)
                .sampleSize(request.getSampleSize())
                .baselineCvr(request.getBaselineCvr())
                .targetCvr(request.getTargetCvr())
                .mdePercent(request.getMdePercent())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ExperimentStatus.PLANNED)
                .platform(ExperimentPlatform.FACEBOOK)
                .creativesToGenerate(request.getCreativesToGenerate())
                .build();
        return repository.save(exp);
    }

    @Transactional
    public Experiment create(CreateExperimentRequest request) {
        if (request.getMarketNicheId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "marketNicheId required");
        }
        return create(request.getMarketNicheId(), request);
    }

    public Experiment get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<Experiment> list() {
        return repository.findAll();
    }

    public Iterable<Experiment> listByNiche(Long nicheId) {
        return repository.findByNicheId(nicheId);
    }

    @Transactional
    public Experiment duplicate(Long id) {
        Experiment original = repository.findById(id).orElseThrow();
        Experiment copy = Experiment.builder()
                .niche(original.getNiche())
                .name(original.getName() + " copy")
                .hypothesis(original.getHypothesis())
                .hypothesisRef(original.getHypothesisRef())
                .kpiTargetCpl(original.getKpiTargetCpl())
                .metricPreset(original.getMetricPreset())
                .stopLossCpl(original.getStopLossCpl())
                .sampleSize(original.getSampleSize())
                .baselineCvr(original.getBaselineCvr())
                .targetCvr(original.getTargetCvr())
                .mdePercent(original.getMdePercent())
                .startDate(original.getStartDate())
                .endDate(original.getEndDate())
                .status(ExperimentStatus.PLANNED)
                .platform(original.getPlatform())
                .creativesToGenerate(original.getCreativesToGenerate())
                .build();
        return repository.save(copy);
    }

    /**
     * Updates the status of an experiment.
     */
    @Transactional
    public Experiment updateStatus(Long id, ExperimentStatus status) {
        Experiment exp = repository.findById(id).orElseThrow();
        if (status == ExperimentStatus.RUNNING) {
            if (exp.getKpiTargetCpl() == null || exp.getStopLossCpl() == null || exp.getSampleSize() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "financial fields not set");
            }
        }
        exp.setStatus(status);
        return exp;
    }

    /**
     * Updates mutable fields of an experiment.
     */
    @Transactional
    public Experiment update(Long id, UpdateExperimentRequest request) {
        Experiment exp = repository.findById(id).orElseThrow();

        if (request.getName() == null || request.getKpiTargetCpl() == null
                || request.getHypothesis() == null || request.getMetricPresetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "required fields missing");
        }

        if (!exp.getName().equals(request.getName()) &&
                repository.existsByNicheAndName(exp.getNiche(), request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name already exists for niche");
        }

        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }

        if (request.getSampleSize() != null && request.getSampleSize() < 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sampleSize must be at least 100");
        }

        MetricPreset preset = metricPresetService.get(request.getMetricPresetId());

        exp.setName(request.getName());
        exp.setHypothesis(request.getHypothesis());
        exp.setKpiTargetCpl(request.getKpiTargetCpl());
        exp.setMetricPreset(preset);
        if (request.getSampleSize() != null) {
            exp.setSampleSize(request.getSampleSize());
        } else {
            exp.setSampleSize(preset.getSampleSize());
        }
        if (request.getMdePercent() != null) {
            exp.setMdePercent(request.getMdePercent());
        } else {
            exp.setMdePercent(preset.getDefaultMdePp());
        }
        if (request.getKpiTargetCpl() != null && preset.getStopLossFactor() != null) {
            exp.setStopLossCpl(request.getKpiTargetCpl().multiply(preset.getStopLossFactor()));
        }
        exp.setStartDate(request.getStartDate());
        exp.setEndDate(request.getEndDate());
        if (request.getCreativesToGenerate() != null) {
            exp.setCreativesToGenerate(request.getCreativesToGenerate());
        }
        return exp;
    }

    /**
     * Requests generation of new creatives by setting the pending quantity.
     */
    @Transactional
    public Experiment requestCreatives(Long id, int quantity) {
        Experiment exp = repository.findById(id).orElseThrow();
        exp.setCreativesToGenerate(quantity);
        return exp;
    }
}
