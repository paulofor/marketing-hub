package com.marketinghub.audience.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.audience.*;
import com.marketinghub.audience.dto.AudienceTargetingSeedRequest;
import com.marketinghub.audience.dto.CreateAudienceRequest;
import com.marketinghub.audience.dto.UpdateAudienceTargetingRequest;
import com.marketinghub.audience.repository.AudienceRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for audiences.
 */
@Service
public class AudienceService {
    private final AudienceRepository repository;
    private final MarketNicheRepository nicheRepository;
    private final HypothesisRepository hypothesisRepository;
    private final ObjectMapper objectMapper;

    public AudienceService(AudienceRepository repository,
                           MarketNicheRepository nicheRepository,
                           HypothesisRepository hypothesisRepository,
                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates an audience associated with a niche or hypothesis.
     */
    @Transactional
    public Audience create(CreateAudienceRequest request) {
        com.marketinghub.niche.MarketNiche niche = null;
        if (request.getMarketNicheId() != null) {
            niche = nicheRepository.findById(request.getMarketNicheId()).orElseThrow();
        }
        com.marketinghub.hypothesis.Hypothesis hypothesis = null;
        if (request.getHypothesisId() != null) {
            hypothesis = hypothesisRepository.findById(request.getHypothesisId()).orElseThrow();
        }
        Audience audience = Audience.builder()
                .name(request.getName())
                .description(request.getDescription())
                .prompt(request.getPrompt())
                .model(request.getModel())
                .niche(niche)
                .hypothesis(hypothesis)
                .source(request.getSource())
                .targetingStatus(TargetingStatus.DRAFT)
                .build();
        return repository.save(audience);
    }

    /**
     * Atualiza o status de aprovação de um público existente.
     */
    @Transactional
    public Audience updateApproval(Long id, boolean approved) {
        Audience audience = repository.findById(id).orElseThrow();
        audience.setApproved(approved);
        return audience;
    }

    /**
     * Atualiza o targeting estruturado, seus seeds e o status de revisão.
     */
    @Transactional
    public Audience updateTargeting(Long id, UpdateAudienceTargetingRequest request) {
        Audience audience = repository.findById(id).orElseThrow();
        if (request.getTargetingSpec() != null) {
            String spec = request.getTargetingSpec().trim();
            validateTargetingSpec(spec);
            audience.setTargetingSpec(spec);
            if (audience.getTargetingStatus() == TargetingStatus.DRAFT && request.getStatus() == null) {
                audience.setTargetingStatus(TargetingStatus.NEEDS_REVIEW);
            }
        }
        if (request.getNotes() != null) {
            audience.setTargetingNotes(request.getNotes());
        }
        if (request.getLastReviewedBy() != null) {
            audience.setLastReviewedBy(request.getLastReviewedBy());
        }
        if (request.getSource() != null) {
            audience.setSource(request.getSource());
        }
        if (request.getStatus() != null) {
            audience.setTargetingStatus(request.getStatus());
        }
        if (audience.getTargetingStatus() == TargetingStatus.READY && (audience.getTargetingSpec() == null || audience.getTargetingSpec().isBlank())) {
            throw new IllegalArgumentException("Não é possível aprovar targeting sem targeting_spec preenchido");
        }
        if (request.getSeeds() != null) {
            if (audience.getTargetingSeeds() == null) {
                audience.setTargetingSeeds(new ArrayList<>());
            }
            audience.getTargetingSeeds().clear();
            for (AudienceTargetingSeedRequest seedRequest : request.getSeeds()) {
                if (seedRequest.getType() == null || seedRequest.getValue() == null || seedRequest.getValue().isBlank()) {
                    continue;
                }
                AudienceTargetingSeed seed = AudienceTargetingSeed.builder()
                        .audience(audience)
                        .type(seedRequest.getType())
                        .value(seedRequest.getValue())
                        .metaId(seedRequest.getMetaId())
                        .key(seedRequest.getKey())
                        .confidence(seedRequest.getConfidence())
                        .status(seedRequest.getStatus() != null ? seedRequest.getStatus() : TargetingSeedStatus.DRAFT)
                        .build();
                audience.getTargetingSeeds().add(seed);
            }
        }
        return audience;
    }

    /**
     * Marca o targeting para reprocessamento, limpando status e devolvendo seeds a DRAFT.
     */
    @Transactional
    public Audience markSeedsForReprocess(Long id) {
        Audience audience = repository.findById(id).orElseThrow();
        audience.setTargetingStatus(TargetingStatus.DRAFT);
        audience.setTargetingSpec(null);
        List<AudienceTargetingSeed> seeds = audience.getTargetingSeeds();
        if (seeds != null) {
            for (AudienceTargetingSeed seed : seeds) {
                seed.setStatus(TargetingSeedStatus.DRAFT);
            }
        }
        return audience;
    }

    public Audience get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<Audience> list() {
        return repository.findAll();
    }

    public Iterable<Audience> listByMarketNiche(Long nicheId) {
        return repository.findByNicheId(nicheId);
    }

    private void validateTargetingSpec(String spec) {
        try {
            JsonNode root = objectMapper.readTree(spec);
            if (!root.isObject()) {
                throw new IllegalArgumentException("targeting_spec deve ser um objeto JSON");
            }
            JsonNode geo = root.get("geo_locations");
            if (geo == null || geo.isNull() || !hasGeoContent(geo)) {
                throw new IllegalArgumentException("targeting_spec precisa de geo_locations válidos");
            }
            if (!hasPrimaryCriterion(root)) {
                throw new IllegalArgumentException("targeting_spec precisa de pelo menos um critério principal (interesse, comportamento, custom audience ou Advantage+)");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("targeting_spec inválido: não foi possível interpretar o JSON", e);
        }
    }

    private boolean hasGeoContent(JsonNode geo) {
        return hasNonEmptyArray(geo, "countries") || hasNonEmptyArray(geo, "regions") ||
                hasNonEmptyArray(geo, "cities") || hasNonEmptyArray(geo, "zips") ||
                hasNonEmptyArray(geo, "custom_locations") || hasNonEmptyArray(geo, "geo_markets");
    }

    private boolean hasPrimaryCriterion(JsonNode root) {
        return hasNonEmptyArray(root, "interests") || hasNonEmptyArray(root, "behaviors") ||
                hasNonEmptyArray(root, "custom_audiences") || hasNonEmptyArray(root, "product_audience_specs") ||
                hasNonEmptyArray(root, "dynamic_audience_ids") || hasNonEmptyArray(root, "flexible_spec") ||
                hasAdvantageAudience(root.path("targeting_automation")) || root.has("saved_audience_id");
    }

    private boolean hasAdvantageAudience(JsonNode automation) {
        if (automation == null || automation.isNull()) {
            return false;
        }
        JsonNode advantage = automation.get("advantage_audience");
        if (advantage == null || advantage.isNull()) {
            return false;
        }
        if (advantage.isBoolean()) {
            return advantage.booleanValue();
        }
        if (advantage.isNumber()) {
            return advantage.intValue() != 0;
        }
        String value = advantage.asText(null);
        return value != null && !value.isBlank() && !value.equalsIgnoreCase("false") && !"0".equals(value.trim());
    }

    private boolean hasNonEmptyArray(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isArray() && value.size() > 0;
    }
}
