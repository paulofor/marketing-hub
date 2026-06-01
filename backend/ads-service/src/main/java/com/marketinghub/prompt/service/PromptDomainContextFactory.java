package com.marketinghub.prompt.service;

import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.repository.jpa.differentiatedtechnology.DifferentiatedTechnologyRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.repository.jpa.journey.JourneyRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.description.NicheDetailedDescription;
import com.marketinghub.repository.jpa.niche.description.NicheDetailedDescriptionRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.PromptDomainObjectType;
import com.marketinghub.repository.jpa.prompt.PromptAttributeDescriptionRepository;
import com.marketinghub.repository.jpa.prompt.PromptAttributeRepository;
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
import java.util.UUID;

@Component
public class PromptDomainContextFactory {
    private static final int AVAILABLE_VARIABLE_LIMIT = 120;
    private static final String DEFAULT_HYPOTHESIS_ENTITY = "hypothesis";

    private final MarketNicheRepository marketNicheRepository;
    private final NicheDetailedDescriptionRepository detailedDescriptionRepository;
    private final DifferentiatedTechnologyRepository technologyRepository;
    private final HypothesisRepository hypothesisRepository;
    private final ExperimentRepository experimentRepository;
    private final JourneyRepository journeyRepository;
    private final PromptAttributeRepository promptAttributeRepository;
    private final PromptAttributeDescriptionRepository promptAttributeDescriptionRepository;

    public PromptDomainContextFactory(MarketNicheRepository marketNicheRepository,
                                      NicheDetailedDescriptionRepository detailedDescriptionRepository,
                                      DifferentiatedTechnologyRepository technologyRepository,
                                      HypothesisRepository hypothesisRepository,
                                      ExperimentRepository experimentRepository,
                                      JourneyRepository journeyRepository,
                                      PromptAttributeRepository promptAttributeRepository,
                                      PromptAttributeDescriptionRepository promptAttributeDescriptionRepository) {
        this.marketNicheRepository = marketNicheRepository;
        this.detailedDescriptionRepository = detailedDescriptionRepository;
        this.technologyRepository = technologyRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.experimentRepository = experimentRepository;
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
        boolean includeExperiment = objects.contains(PromptDomainObjectType.EXPERIMENT);

        Optional<MarketNiche> nicheSample = includeNiche ? findSampleNiche() : Optional.empty();
        Optional<NicheDetailedDescription> detailedSample = includeDetailedDescription
                ? findSampleDetailedDescription()
                : Optional.empty();
        Optional<DifferentiatedTechnology> techSample = includeTechnology ? findSampleTechnology() : Optional.empty();
        Optional<Hypothesis> hypothesisSample = includeHypothesis ? findSampleHypothesis() : Optional.empty();
        Optional<Journey> journeySample = includeJourney ? findSampleJourney() : Optional.empty();
        Optional<Experiment> experimentSample = includeExperiment ? findSampleExperiment() : Optional.empty();

        MarketNiche nicheForExperiment = experimentSample
                .map(Experiment::getNiche)
                .orElse(nicheSample.orElse(null));
        Hypothesis hypothesisForExperiment = experimentSample
                .map(Experiment::getHypothesisRef)
                .orElse(hypothesisSample.orElse(null));

        Map<String, Object> detailedContext = includeDetailedDescription
                ? buildDetailedDescriptionContext(detailedSample.orElse(null))
                : null;
        Map<String, Object> technologyContext = includeTechnology
                ? buildTechnologyContext(techSample.orElse(null))
                : null;
        Map<String, Object> nicheContext = (includeNiche || includeExperiment)
                ? buildNicheContext(nicheForExperiment, detailedContext, technologyContext,
                includeDetailedDescription || includeTechnology)
                : null;

        if (includeNiche && nicheContext != null) {
            context.put("niche", nicheContext);
        }
        Map<String, Object> hypothesisContext = (includeHypothesis || includeExperiment)
                ? buildHypothesisContextOrSample(hypothesisForExperiment)
                : null;
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
            context.put("hypothesis", hypothesisContext != null ? hypothesisContext : buildHypothesisContextOrSample(null));
        }
        if (includeJourney) {
            context.put("journey", buildJourneyContext(journeySample.orElse(null)));
        }
        if (includeExperiment) {
            context.put("experiment", buildExperimentContext(
                    experimentSample.orElse(null),
                    nicheContext,
                    hypothesisContext));
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

    private Optional<Experiment> findSampleExperiment() {
        return experimentRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
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

    private Map<String, Object> buildExperimentContext(Experiment experiment,
                                                         Map<String, Object> nicheContext,
                                                         Map<String, Object> hypothesisContext) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", experiment != null ? experiment.getId() : 1L);
        context.put("name", textOrDefault(experiment != null ? experiment.getName() : null, "Experimento Exemplo"));
        context.put("hypothesisSummary", textOrDefault(experiment != null ? experiment.getHypothesis() : null, "Resumo do experimento"));
        context.put("status", experiment != null && experiment.getStatus() != null ? experiment.getStatus().name() : "DRAFT");
        context.put("platform", experiment != null && experiment.getPlatform() != null ? experiment.getPlatform().name() : "META");
        context.put("leadPortalFlowsToGenerate",
                experiment != null && experiment.getLeadPortalFlowsToGenerate() != null
                        ? experiment.getLeadPortalFlowsToGenerate()
                        : 3);
        context.put("followUpActionUrl", textOrDefault(
                experiment != null ? experiment.getFollowUpActionUrl() : null,
                "https://exemplo.com/acao"));
        context.put("createdAt", experiment != null && experiment.getCreatedAt() != null ? experiment.getCreatedAt() : Instant.now());
        context.put("updatedAt", experiment != null && experiment.getUpdatedAt() != null ? experiment.getUpdatedAt() : Instant.now());
        if (experiment != null && experiment.getJourneyTemplate() != null) {
            context.put("journeyTemplateId", experiment.getJourneyTemplate().getId());
        }
        context.put("hypothesis", hypothesisContext != null ? hypothesisContext : buildHypothesisContextOrSample(null));
        context.put("niche", nicheContext != null ? nicheContext : buildNicheContext(null, null, null, false));
        return context;
    }

    private Map<String, Object> buildHypothesisContextOrSample(Hypothesis hypothesis) {
        Hypothesis resolved = hypothesis;
        if (resolved == null) {
            resolved = new Hypothesis();
            resolved.setTitle("Hipótese exemplo");
            resolved.setPromise("Promessa que gera valor para o lead");
            resolved.setProblem("Principais dores que o lead enfrenta hoje");
            resolved.setPersona("Perfil de cliente ideal");
            resolved.setMechanism("Mecanismo principal utilizado pela solução");
            resolved.setUniqueMechanism("Elemento exclusivo que diferencia a oferta");
            resolved.setEntrega("Entregável oferecido");
            resolved.setSuccessRule("Critério utilizado para medir sucesso");
            resolved.setModel("gpt-4o-mini");
            resolved.setStatus(HypothesisStatus.BACKLOG);
            resolved.setGeneratedAt(Instant.now());
            resolved.setCreatedAt(Instant.now());
            resolved.setUpdatedAt(Instant.now());
            resolved.setId(UUID.randomUUID());
        }
        return buildHypothesisContext(resolved);
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
