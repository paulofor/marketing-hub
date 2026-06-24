package com.marketinghub.targeting.service;

import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationFailureRequest;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationPendingDto;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationResultRequest;
import com.marketinghub.targeting.dto.UpdateTargetingElementRequest;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Camada de serviço para elementos de segmentação.
 */
@Service
public class TargetingElementService {
    private static final Logger log = LoggerFactory.getLogger(TargetingElementService.class);

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

    /** Cria um elemento de segmentação garantindo dados mínimos para aprovação operacional. */
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
        validateReadyState(element);
        return repository.save(element);
    }

    /** Atualiza um elemento de segmentação sem permitir aprovação sem ID oficial da Meta. */
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

    /** Lista pendências de geração de públicos para o AI Worker consumir via backend. */
    @Transactional(readOnly = true)
    public List<TargetingElementGenerationPendingDto> listPendingGeneration(int limit) {
        int maxItems = Math.max(limit, 1);
        List<TargetingElementGenerationPendingDto> pending = new ArrayList<>();
        for (var niche : nicheRepository.findAllToGenerateInterests()) {
            addPending(pending, niche, TargetingElementType.INTEREST, niche.getInterestsToGenerate(), niche.getInterestModel(), maxItems);
            if (pending.size() >= maxItems) {
                return pending;
            }
        }
        for (var niche : nicheRepository.findAllToGenerateJobTitles()) {
            addPending(pending, niche, TargetingElementType.JOB_TITLE, niche.getJobTitlesToGenerate(), niche.getJobTitleModel(), maxItems);
            if (pending.size() >= maxItems) {
                return pending;
            }
        }
        for (var niche : nicheRepository.findAllToGenerateBehaviors()) {
            addPending(pending, niche, TargetingElementType.BEHAVIOR, niche.getBehaviorsToGenerate(), niche.getBehaviorModel(), maxItems);
            if (pending.size() >= maxItems) {
                return pending;
            }
        }
        return pending;
    }

    /** Persiste públicos gerados pelo AI Worker e zera a pendência processada no nicho. */
    @Transactional
    public void saveGeneratedElements(Long nicheId,
                                      TargetingElementType type,
                                      TargetingElementGenerationResultRequest request) {
        if (nicheId == null || type == null) {
            throw new IllegalArgumentException("nicheId e type são obrigatórios");
        }
        List<CreateTargetingElementRequest> items = request != null && request.items() != null
                ? request.items()
                : List.of();
        for (CreateTargetingElementRequest item : items) {
            item.setMarketNicheId(nicheId);
            item.setType(type);
            create(item);
        }
        var niche = nicheRepository.findById(nicheId).orElseThrow();
        resetPendingCounter(niche, type);
        nicheRepository.save(niche);
        log.info("AI Worker reportou {} públicos gerados para nicho {} e tipo {}", items.size(), nicheId, type);
    }

    /** Registra falha do AI Worker e zera a pendência para impedir reprocessamento infinito. */
    @Transactional
    public void markGenerationFailure(Long nicheId,
                                      TargetingElementType type,
                                      TargetingElementGenerationFailureRequest request) {
        if (nicheId == null || type == null) {
            throw new IllegalArgumentException("nicheId e type são obrigatórios");
        }
        var niche = nicheRepository.findById(nicheId).orElseThrow();
        resetPendingCounter(niche, type);
        nicheRepository.save(niche);
        log.warn("AI Worker reportou falha ao gerar públicos para nicho {} e tipo {}: {}",
                nicheId,
                type,
                request != null ? request.error() : null);
    }

    private String normalizeTerm(String term) {
        if (!StringUtils.hasText(term)) {
            return null;
        }
        return term.trim();
    }

    /** Valida se o elemento aprovado está pronto para uso em campanhas Meta Ads. */
    private void validateReadyState(TargetingElement element) {
        if (element.getStatus() == TargetingElementStatus.APPROVED && !StringUtils.hasText(element.getTerm())) {
            throw new IllegalArgumentException("Não é possível aprovar um elemento sem termo");
        }
        if (element.getStatus() == TargetingElementStatus.APPROVED
                && requiresMetaId(element.getType())
                && !StringUtils.hasText(element.getMetaId())) {
            throw new IllegalArgumentException("Não é possível aprovar um público sem ID oficial da Meta");
        }
    }

    /** Indica se o tipo de público depende de ID oficial da Meta para uso em campanha. */
    private boolean requiresMetaId(TargetingElementType type) {
        return type == TargetingElementType.INTEREST
                || type == TargetingElementType.JOB_TITLE
                || type == TargetingElementType.BEHAVIOR;
    }

    /** Adiciona uma pendência de geração ao contrato interno respeitando o limite solicitado. */
    private void addPending(List<TargetingElementGenerationPendingDto> pending,
                            com.marketinghub.niche.MarketNiche niche,
                            TargetingElementType type,
                            Integer quantity,
                            String model,
                            int maxItems) {
        if (pending.size() >= maxItems || niche == null || quantity == null || quantity <= 0) {
            return;
        }
        pending.add(new TargetingElementGenerationPendingDto(
                niche.getId(),
                niche.getName(),
                niche.getDescription(),
                niche.getBaseSegmentation(),
                niche.getInterests(),
                niche.getDemographicFilters(),
                niche.getExtraTips(),
                niche.getInterestCategory(),
                niche.getRoleCategory(),
                type,
                quantity,
                model));
    }

    /** Zera o contador pendente referente ao tipo processado pelo AI Worker. */
    private void resetPendingCounter(com.marketinghub.niche.MarketNiche niche, TargetingElementType type) {
        switch (type) {
            case INTEREST -> niche.setInterestsToGenerate(0);
            case JOB_TITLE -> niche.setJobTitlesToGenerate(0);
            case BEHAVIOR -> niche.setBehaviorsToGenerate(0);
        }
    }
}
