package com.marketinghub.experiment.service;

import com.marketinghub.experiment.*;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
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
    private final EntityManager entityManager;

    public ExperimentService(ExperimentRepository repository, MarketNicheRepository nicheRepository,
                             com.marketinghub.hypothesis.repository.HypothesisRepository hypothesisRepository,
                             EntityManager entityManager) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
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
        if (request.getStopLossCpl() != null && request.getStopLossCpl().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stopLossCpl must be positive");
        }
        if (request.getKpiTargetCpl() != null && request.getStopLossCpl() != null &&
                request.getStopLossCpl().compareTo(request.getKpiTargetCpl()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stopLossCpl must be >= kpiTargetCpl");
        }
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
                .stopLossCpl(request.getStopLossCpl())
                .sampleSize(request.getSampleSize())
                .baselineCvr(request.getBaselineCvr())
                .targetCvr(request.getTargetCvr())
                .mdePercent(request.getMdePercent())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ExperimentStatus.PLANNED)
                .platform(ExperimentPlatform.FACEBOOK)
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
                .stopLossCpl(original.getStopLossCpl())
                .sampleSize(original.getSampleSize())
                .baselineCvr(original.getBaselineCvr())
                .targetCvr(original.getTargetCvr())
                .mdePercent(original.getMdePercent())
                .startDate(original.getStartDate())
                .endDate(original.getEndDate())
                .status(ExperimentStatus.PLANNED)
                .platform(original.getPlatform())
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
}
