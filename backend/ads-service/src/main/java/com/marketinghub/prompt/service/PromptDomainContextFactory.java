package com.marketinghub.prompt.service;

import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.differentiatedtechnology.repository.DifferentiatedTechnologyRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.repository.JourneyRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.description.NicheDetailedDescription;
import com.marketinghub.niche.description.repository.NicheDetailedDescriptionRepository;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.PromptDomainObjectType;
import com.marketinghub.prompt.repository.PromptAttributeDescriptionRepository;
import com.marketinghub.prompt.repository.PromptAttributeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class PromptDomainContextFactory {
    private static final int AVAILABLE_VARIABLE_LIMIT = 120;
    private static final String DEFAULT_HYPOTHESIS_ENTITY = "hypothesis";

    private final MarketNicheRepository marketNicheRepository;
    private final NicheDetailedDescriptionRepository detailedDescriptionRepository;
    private final DifferentiatedTechnologyRepository technologyRepository;
    private final HypothesisRepository hypothesisRepository;
    private final JourneyRepository journeyRepository;
    private final PromptAttributeRepository promptAttributeRepository;
    private final PromptAttributeDescriptionRepository promptAttributeDescriptionRepository;

    public PromptDomainContextFactory(MarketNicheRepository marketNicheRepository,
                                      NicheDetailedDescriptionRepository detailedDescriptionRepository,
                                      DifferentiatedTechnologyRepository technologyRepository,
                                      HypothesisRepository hypothesisRepository,
                                      JourneyRepository journeyRepository,
                                      PromptAttributeRepository promptAttributeRepository,
                                      PromptAttributeDescriptionRepository promptAttributeDescriptionRepository) {
        this.marketNicheRepository = marketNicheRepository;
        this.detailedDescriptionRepository = detailedDescriptionRepository;
        this.technologyRepository = technologyRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.journeyRepository = journeyRepository;
        this.promptAttributeRepository = promptAttributeRepository;
        this.promptAttributeDescriptionRepository = promptAttributeDescriptionRepository;
    }

    public Map<String, Object> buildSampleContext(List<PromptDomainObjectType> objectTypes) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("quantity", 1);
        List<PromptDomainObjectType> objects = objectTypes == null ? List.of() : objectTypes;
        boolean includeNiche = objects.contains(PromptDomainObjectType.NICHE);
        boolean includeDetailedDescription = objects.contains(PromptDomainObjectType.DETAILED_DESCRIPTION);
        boolean includeTechnology = objects.contains(PromptDomainObjectType.DIFFERENTIATED_TECHNOLOGY);
        boolean includeHypothesis = objects.contains(PromptDomainObjectType.HYPOTHESIS);
        boolean includeJourney = objects.contains(PromptDomainObjectType.JOURNEY);

        Optional<MarketNiche> nicheSample = includeNiche ? findSampleNiche() : Optional.empty();
        Optional<NicheDetailedDescription> detailedSample = includeDetailedDescription
                ? findSampleDetailedDescription()
                : Optional.empty();
        Optional<DifferentiatedTechnology> techSample = includeTechnology ? findSampleTechnology() : Optional.empty();
        Optional<Hypothesis> hypothesisSample = includeHypothesis ? findSampleHypothesis() : Optional.empty();
        Optional<Journey> journeySample = includeJourney ? findSampleJourney() : Optional.empty();

        Map<String, Object> detailedContext = includeDetailedDescription
                ? buildDetailedDescriptionContext(detailedSample.orElse(null))
                : null;
        Map<String, Object> technologyContext = includeTechnology
                ? buildTechnologyContext(techSample.orElse(null))
                : null;
        Map<String, Object> nicheContext = includeNiche
                ? buildNicheContext(nicheSample.orElse(null), detailedContext, technologyContext,
                includeDetailedDescription || includeTechnology)
                : null;

        if (includeNiche) {
            context.put("niche", nicheContext);
        }
        if (includeDetailedDescription) {
            context.put("detailedDescription", detailedContext);
        }
        if (includeTechnology) {
            context.put("technology", technologyContext);
        }
        if (includeHypothesis) {
            List<Map<String, Object>> attributes = buildAttributeContexts();
            if (attributes.isEmpty()) {
                attributes = List.of(buildAttributeContext(null));
            }
            List<String> attributeNames = attributes.stream()
                    .map(attr -> Objects.toString(attr.get("name"), ""))
                    .filter(name -> !name.isBlank())
                    .toList();
            if (attributeNames.isEmpty()) {
                attributeNames = List.of("title", "promise");
            }
            context.put("attributes", attributes);
            context.put("attributeNames", attributeNames);
            context.put("defaultAttributes", attributeNames);
            if (hypothesisSample.isPresent()) {
                context.put("hypothesis", buildHypothesisContext(hypothesisSample.get()));
            }
        }
        if (includeJourney) {
            context.put("journey", buildJourneyContext(journeySample.orElse(null)));
        }
        // Preserve backwards compatibility for legacy keys sourced from the niche context
        if (includeDetailedDescription && nicheContext != null) {
            context.putIfAbsent("detailedDescription", nicheContext.get("hypothesisDetailedDescription"));
        }
        if (includeTechnology && nicheContext != null) {
            context.putIfAbsent("technology", nicheContext.get("differentiatedTechnology"));
        }
        return context;
    }

    public List<String> availableVariables(List<PromptDomainObjectType> objectTypes) {
        Map<String, Object> context = buildSampleContext(objectTypes);
        if (context.isEmpty()) {
            return List.of();
        }
        Set<String> vars = new LinkedHashSet<>();
        collectVariables("", context, vars, 3);
        if (vars.size() > AVAILABLE_VARIABLE_LIMIT) {
            return vars.stream().limit(AVAILABLE_VARIABLE_LIMIT).toList();
        }
        return new ArrayList<>(vars);
    }

    private Optional<MarketNiche> findSampleNiche() {
        return marketNicheRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .findFirst();
    }

    private Optional<NicheDetailedDescription> findSampleDetailedDescription() {
        return detailedDescriptionRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .findFirst();
    }

    private Optional<DifferentiatedTechnology> findSampleTechnology() {
        return technologyRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .findFirst();
    }

    private Optional<Hypothesis> findSampleHypothesis() {
        return hypothesisRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .findFirst();
    }

    private Optional<Journey> findSampleJourney() {
        return journeyRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .findFirst();
    }

    private Map<String, Object> buildNicheContext(MarketNiche niche,
                                                  Map<String, Object> detailedContext,
                                                  Map<String, Object> technologyContext,
                                                  boolean includeExtraFields) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", niche != null ? niche.getId() : 1L);
        context.put("name", textOrDefault(niche != null ? niche.getName() : null, "Nicho Exemplo"));
        context.put("description", textOrDefault(niche != null ? niche.getDescription() : null, "Descrição"));
        context.put("baseSegmentation", textOrDefault(niche != null ? niche.getBaseSegmentation() : null, "Segmentação"));
        context.put("interests", textOrDefault(niche != null ? niche.getInterests() : null, "Interesses"));
        context.put("demographicFilters",
                textOrDefault(niche != null ? niche.getDemographicFilters() : null, "Filtros"));
        context.put("extraTips", textOrDefault(niche != null ? niche.getExtraTips() : null, "Dicas"));
        context.put("interestCategory",
                textOrDefault(niche != null ? niche.getInterestCategory() : null, "Categoria"));
        context.put("roleCategory", textOrDefault(niche != null ? niche.getRoleCategory() : null, "Papel"));
        if (includeExtraFields) {
            Map<String, Object> fallbackDetailed = detailedContext != null ? detailedContext : buildDetailedDescriptionContext(null);
            Map<String, Object> fallbackTechnology = technologyContext != null ? technologyContext : buildTechnologyContext(null);
            context.put("detailedDescriptions", List.of(fallbackDetailed));
            context.put("latestDetailedDescription", fallbackDetailed);
            context.put("hypothesisDetailedDescription", fallbackDetailed);
            context.put("differentiatedTechnology", fallbackTechnology);
        }
        return context;
    }

    private Map<String, Object> buildDetailedDescriptionContext(NicheDetailedDescription description) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", description != null ? description.getId() : 1L);
        context.put("title", textOrDefault(description != null ? description.getTitle() : null, "Título"));
        context.put("description",
                textOrDefault(description != null ? description.getDescription() : null, "Descrição detalhada"));
        context.put("pains", textOrDefault(description != null ? description.getPains() : null, "Dores"));
        context.put("desires", textOrDefault(description != null ? description.getDesires() : null, "Desejos"));
        context.put("needs", textOrDefault(description != null ? description.getNeeds() : null, "Necessidades"));
        context.put("model", textOrDefault(description != null ? description.getModel() : null, "gpt-4o-mini"));
        context.put("prompt", textOrDefault(description != null ? description.getPrompt() : null, "Prompt"));
        context.put("createdAt", description != null && description.getCreatedAt() != null ? description.getCreatedAt() : Instant.now());
        context.put("updatedAt", description != null && description.getUpdatedAt() != null ? description.getUpdatedAt() : Instant.now());
        return context;
    }

    private Map<String, Object> buildTechnologyContext(DifferentiatedTechnology technology) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", technology != null ? technology.getId() : 1L);
        context.put("name", textOrDefault(technology != null ? technology.getName() : null, "Tecnologia"));
        context.put("description", textOrDefault(technology != null ? technology.getDescription() : null, "Descrição"));
        context.put("promptText",
                textOrDefault(technology != null ? technology.getPromptText() : null, "Instruções adicionais"));
        context.put("createdAt", technology != null && technology.getCreatedAt() != null ? technology.getCreatedAt() : Instant.now());
        context.put("updatedAt", technology != null && technology.getUpdatedAt() != null ? technology.getUpdatedAt() : Instant.now());
        return context;
    }

    private Map<String, Object> buildHypothesisContext(Hypothesis hypothesis) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", hypothesis.getId());
        context.put("title", textOrDefault(hypothesis.getTitle(), "Hipótese"));
        context.put("promise", textOrDefault(hypothesis.getPromise(), "Promessa"));
        context.put("problem", textOrDefault(hypothesis.getProblem(), "Problema"));
        context.put("persona", textOrDefault(hypothesis.getPersona(), "Persona"));
        context.put("mechanism", textOrDefault(hypothesis.getMechanism(), "Mecanismo"));
        context.put("uniqueMechanism", textOrDefault(hypothesis.getUniqueMechanism(), "Mecanismo único"));
        context.put("entrega", textOrDefault(hypothesis.getEntrega(), "Entrega"));
        context.put("prompt", textOrDefault(hypothesis.getPrompt(), "Prompt"));
        context.put("model", textOrDefault(hypothesis.getModel(), "gpt-4o-mini"));
        context.put("status", hypothesis.getStatus() != null ? hypothesis.getStatus().name() : "BACKLOG");
        context.put("generatedAt", hypothesis.getGeneratedAt());
        context.put("createdAt", hypothesis.getCreatedAt());
        context.put("updatedAt", hypothesis.getUpdatedAt());
        return context;
    }

    private Map<String, Object> buildAttributeContext(PromptAttribute attribute) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (attribute == null) {
            context.put("id", 1L);
            context.put("name", "title");
            context.put("description", "Descrição do campo");
            return context;
        }
        context.put("id", attribute.getId());
        context.put("name", textOrDefault(attribute.getName(), "title"));
        String description = promptAttributeDescriptionRepository
                .findByAttribute_IdAndActiveTrue(attribute.getId())
                .map(PromptAttributeDescription::getDescription)
                .orElse("Descrição do campo");
        context.put("description", textOrDefault(description, "Descrição do campo"));
        return context;
    }

    private List<Map<String, Object>> buildAttributeContexts() {
        List<PromptAttribute> attributes = promptAttributeRepository.findByEntity_Name(DEFAULT_HYPOTHESIS_ENTITY);
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (PromptAttribute attribute : attributes) {
            mapped.add(buildAttributeContext(attribute));
        }
        return mapped;
    }

    private Map<String, Object> buildJourneyContext(Journey journey) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", journey != null ? journey.getId() : 1L);
        context.put("name", textOrDefault(journey != null ? journey.getName() : null, "Jornada Exemplo"));
        context.put("description",
                textOrDefault(journey != null ? journey.getDescription() : null, "Sequência automatizada de nurture"));
        context.put("status", journey != null && journey.getStatus() != null ? journey.getStatus().name() : "DRAFT");
        context.put("segmentReference",
                textOrDefault(journey != null ? journey.getSegmentReference() : null, "crm-segment-42"));
        context.put("segmentFilter",
                textOrDefault(journey != null ? journey.getSegmentFilter() : null, "lead.score > 70"));
        context.put("startAt", journey != null ? journey.getStartAt() : Instant.now());
        context.put("endAt", journey != null ? journey.getEndAt() : Instant.now().plusSeconds(86400));
        Map<String, String> metadata = journey != null && journey.getMetadata() != null
                ? new LinkedHashMap<>(journey.getMetadata())
                : Map.of("canal", "WhatsApp", "objetivo", "agendamento");
        context.put("metadata", metadata);
        return context;
    }

    private String textOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private void collectVariables(String prefix, Object value, Set<String> acc, int depth) {
        if (depth < 0 || acc.size() >= AVAILABLE_VARIABLE_LIMIT || value == null) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                String path = prefix.isEmpty() ? key : prefix + "." + key;
                acc.add(path);
                collectVariables(path, entry.getValue(), acc, depth - 1);
                if (acc.size() >= AVAILABLE_VARIABLE_LIMIT) {
                    return;
                }
            }
        } else if (value instanceof List<?> list && !list.isEmpty()) {
            String listPrefix = prefix.isEmpty() ? "[]" : prefix + "[]";
            acc.add(listPrefix);
            Object first = list.get(0);
            if (first instanceof Map<?, ?> firstMap) {
                for (Map.Entry<?, ?> entry : firstMap.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        continue;
                    }
                    String path = listPrefix + "." + key;
                    acc.add(path);
                    collectVariables(path, entry.getValue(), acc, depth - 1);
                    if (acc.size() >= AVAILABLE_VARIABLE_LIMIT) {
                        return;
                    }
                }
            }
        }
    }
}
