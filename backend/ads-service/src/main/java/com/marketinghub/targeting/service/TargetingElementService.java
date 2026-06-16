package com.marketinghub.targeting.service;

import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.targeting.dto.UpdateTargetingElementRequest;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Camada de serviço para elementos de segmentação.
 */
@Service
public class TargetingElementService {
    private final TargetingElementRepository repository;
    private final MarketNicheRepository nicheRepository;
    private final HypothesisRepository hypothesisRepository;

    public TargetingElementService(TargetingElementRepository repository,
                                   MarketNicheRepository nicheRepository,
                                   HypothesisRepository hypothesisRepository) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
    }

    @Transactional
    public TargetingElement create(CreateTargetingElementRequest request) {
        if (request.getMarketNicheId() == null) {
            throw new IllegalArgumentException("marketNicheId é obrigatório");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("type é obrigatório");
        }
        String term = normalizeTerm(request.getTerm());
        if (term == null) {
            throw new IllegalArgumentException("term é obrigatório");
        }
        var niche = nicheRepository.findById(request.getMarketNicheId()).orElseThrow();
        com.marketinghub.hypothesis.Hypothesis hypothesis = null;
        if (request.getHypothesisId() != null) {
            hypothesis = hypothesisRepository.findById(request.getHypothesisId()).orElseThrow();
        }
        TargetingElement element = TargetingElement.builder()
                .niche(niche)
                .hypothesis(hypothesis)
                .type(request.getType())
                .term(term)
                .description(request.getDescription())
                .prompt(request.getPrompt())
                .model(request.getModel())
                .source(request.getSource())
                .notes(request.getNotes())
                .lastReviewedBy(request.getLastReviewedBy())
                .metaId(request.getMetaId())
                .metaKey(request.getMetaKey())
                .metaAudienceSizeLowerBound(request.getMetaAudienceSizeLowerBound())
                .metaAudienceSizeUpperBound(request.getMetaAudienceSizeUpperBound())
                .confidence(request.getConfidence())
                .build();
        if (request.getStatus() != null) {
            element.setStatus(request.getStatus());
        }
        return repository.save(element);
    }

    @Transactional
    public TargetingElement update(Long id, UpdateTargetingElementRequest request) {
        TargetingElement element = repository.findById(id).orElseThrow();
        if (request.getType() != null) {
            element.setType(request.getType());
        }
        if (request.getTerm() != null) {
            element.setTerm(normalizeTerm(request.getTerm()));
        }
        if (request.getDescription() != null) {
            element.setDescription(request.getDescription());
        }
        if (request.getPrompt() != null) {
            element.setPrompt(request.getPrompt());
        }
        if (request.getModel() != null) {
            element.setModel(request.getModel());
        }
        if (request.getSource() != null) {
            element.setSource(request.getSource());
        }
        if (request.getStatus() != null) {
            element.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            element.setNotes(request.getNotes());
        }
        if (request.getLastReviewedBy() != null) {
            element.setLastReviewedBy(request.getLastReviewedBy());
        }
        if (request.getMetaId() != null) {
            element.setMetaId(request.getMetaId());
        }
        if (request.getMetaKey() != null) {
            element.setMetaKey(request.getMetaKey());
        }
        if (request.getMetaAudienceSizeLowerBound() != null) {
            element.setMetaAudienceSizeLowerBound(request.getMetaAudienceSizeLowerBound());
        }
        if (request.getMetaAudienceSizeUpperBound() != null) {
            element.setMetaAudienceSizeUpperBound(request.getMetaAudienceSizeUpperBound());
        }
        if (request.getConfidence() != null) {
            element.setConfidence(request.getConfidence());
        }
        validateReadyState(element);
        return element;
    }

    public TargetingElement get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public TargetingElement requestMetaAdsReprocessing(Long id) {
        TargetingElement element = repository.findById(id).orElseThrow();
        if (element.getType() != TargetingElementType.INTEREST
                && element.getType() != TargetingElementType.JOB_TITLE
                && element.getType() != TargetingElementType.BEHAVIOR) {
            throw new IllegalArgumentException("Tipo de elemento não suportado para reprocessamento Meta Ads");
        }
        element.setMetaId(null);
        element.setMetaKey(null);
        element.setMetaAudienceSizeLowerBound(null);
        element.setMetaAudienceSizeUpperBound(null);
        element.setMetaIdUnavailable(false);
        element.setMetaIdUnavailableReason(null);
        return repository.save(element);
    }

    public List<TargetingElement> list(TargetingElementType type, TargetingElementStatus status) {
        return repository.findByFilters(null, type, status);
    }

    public List<TargetingElement> listByNiche(Long nicheId,
                                              TargetingElementType type,
                                              TargetingElementStatus status) {
        return repository.findByFilters(nicheId, type, status);
    }

    public List<TargetingElement> findApprovedForExperiment(Long nicheId,
                                                             UUID hypothesisId,
                                                             TargetingElementType type) {
        return repository.findApprovedForExperiment(nicheId, type, hypothesisId);
    }

    public boolean existsApprovedForExperiment(Long nicheId,
                                               UUID hypothesisId,
                                               TargetingElementType type) {
        return repository.existsApprovedForExperiment(nicheId, type, hypothesisId);
    }

    private String normalizeTerm(String term) {
        if (!StringUtils.hasText(term)) {
            return null;
        }
        return term.trim();
    }

    private void validateReadyState(TargetingElement element) {
        if (element.getStatus() == TargetingElementStatus.APPROVED && !StringUtils.hasText(element.getTerm())) {
            throw new IllegalArgumentException("Não é possível aprovar um elemento sem termo");
        }
    }
}
