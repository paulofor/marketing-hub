package com.marketinghub.journey.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.journey.dto.JourneyRequest;
import com.marketinghub.journey.dto.JourneyUpdateRequest;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyStatus;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyRepository;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Application service orchestrating journey lifecycle operations.
 */
@Service
public class JourneyService {
    private final JourneyRepository journeyRepository;
    private final JourneyTemplateRepository templateRepository;
    private final MarketNicheRepository marketNicheRepository;
    private final ExperimentRepository experimentRepository;

    public JourneyService(JourneyRepository journeyRepository,
                          JourneyTemplateRepository templateRepository,
                          MarketNicheRepository marketNicheRepository,
                          ExperimentRepository experimentRepository) {
        this.journeyRepository = journeyRepository;
        this.templateRepository = templateRepository;
        this.marketNicheRepository = marketNicheRepository;
        this.experimentRepository = experimentRepository;
    }

    @Transactional(readOnly = true)
    public Page<Journey> list(Long templateId, JourneyStatus status, Pageable pageable) {
        if (templateId != null && status != null) {
            return journeyRepository.findByTemplateIdAndStatus(templateId, status, pageable);
        }
        if (templateId != null) {
            return journeyRepository.findByTemplateId(templateId, pageable);
        }
        if (status != null) {
            return journeyRepository.findByStatus(status, pageable);
        }
        return journeyRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Journey get(Long id) {
        return journeyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey not found"));
    }

    @Transactional
    public Journey create(JourneyRequest request) {
        JourneyTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey template not found"));

        Journey journey = Journey.builder()
                .template(template)
                .name(request.name())
                .description(request.description())
                .status(request.status() != null ? request.status() : JourneyStatus.DRAFT)
                .segmentReference(request.segmentReference())
                .segmentFilter(request.segmentFilter())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .build();

        journey.setMetadata(normaliseMetadata(request.metadata()));
        journey.setMarketNiche(resolveNiche(request.marketNicheId()));
        journey.setExperiment(resolveExperiment(request.experimentId()));

        return journeyRepository.save(journey);
    }

    @Transactional
    public Journey update(Long id, JourneyUpdateRequest request) {
        Journey journey = journeyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey not found"));

        if (request.templateId() != null && !Objects.equals(request.templateId(), journey.getTemplate().getId())) {
            JourneyTemplate template = templateRepository.findById(request.templateId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey template not found"));
            journey.setTemplate(template);
        }
        if (request.name() != null) {
            journey.setName(request.name());
        }
        if (request.description() != null) {
            journey.setDescription(request.description());
        }
        if (request.status() != null) {
            journey.setStatus(request.status());
        }
        if (request.segmentReference() != null) {
            journey.setSegmentReference(request.segmentReference());
        }
        if (request.segmentFilter() != null) {
            journey.setSegmentFilter(request.segmentFilter());
        }
        if (request.startAt() != null) {
            journey.setStartAt(request.startAt());
        }
        if (request.endAt() != null) {
            journey.setEndAt(request.endAt());
        }
        if (request.metadata() != null) {
            journey.setMetadata(normaliseMetadata(request.metadata()));
        }
        if (request.marketNicheId() != null) {
            journey.setMarketNiche(resolveNiche(request.marketNicheId()));
        }
        if (request.experimentId() != null) {
            journey.setExperiment(resolveExperiment(request.experimentId()));
        }

        return journeyRepository.save(journey);
    }

    @Transactional
    public void delete(Long id) {
        Journey journey = journeyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey not found"));
        journeyRepository.delete(journey);
    }

    private MarketNiche resolveNiche(Long nicheId) {
        if (nicheId == null) {
            return null;
        }
        return marketNicheRepository.findById(nicheId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Market niche not found"));
    }

    private Experiment resolveExperiment(Long experimentId) {
        if (experimentId == null) {
            return null;
        }
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found"));
    }

    private Map<String, String> normaliseMetadata(Map<String, String> metadata) {
        if (metadata == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(metadata);
    }
}
