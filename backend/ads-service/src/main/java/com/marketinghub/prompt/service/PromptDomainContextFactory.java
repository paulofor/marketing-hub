package com.marketinghub.prompt.service;

import com.marketinghub.prompt.PromptDomainObjectType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PromptDomainContextFactory {
    private static final int AVAILABLE_VARIABLE_LIMIT = 120;

    public Map<String, Object> buildSampleContext(List<PromptDomainObjectType> objectTypes) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("quantity", 1);
        List<PromptDomainObjectType> objects = objectTypes == null ? List.of() : objectTypes;
        boolean includeNiche = objects.contains(PromptDomainObjectType.NICHE);
        boolean includeDetailedDescription = objects.contains(PromptDomainObjectType.DETAILED_DESCRIPTION);
        boolean includeTechnology = objects.contains(PromptDomainObjectType.DIFFERENTIATED_TECHNOLOGY);
        boolean includeHypothesis = objects.contains(PromptDomainObjectType.HYPOTHESIS);
        boolean includeJourney = objects.contains(PromptDomainObjectType.JOURNEY);

        Map<String, Object> nicheContext = includeNiche ? buildNicheContext(includeDetailedDescription || includeTechnology) : null;
        if (includeNiche) {
            context.put("niche", nicheContext);
        }
        if (includeDetailedDescription) {
            context.put("detailedDescription", buildDetailedDescriptionContext());
        }
        if (includeTechnology) {
            context.put("technology", buildTechnologyContext());
        }
        if (includeHypothesis) {
            context.put("attributes", List.of(buildAttributeContext()));
            context.put("attributeNames", List.of("title", "promise"));
            context.put("defaultAttributes", List.of("title", "promise"));
        }
        if (includeJourney) {
            context.put("journey", buildJourneyContext());
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

    private Map<String, Object> buildNicheContext(boolean includeExtraFields) {
        Map<String, Object> niche = new LinkedHashMap<>();
        niche.put("id", 1L);
        niche.put("name", "Nicho Exemplo");
        niche.put("description", "Descrição");
        niche.put("baseSegmentation", "Segmentação");
        niche.put("interests", "Interesses");
        niche.put("demographicFilters", "Filtros");
        niche.put("extraTips", "Dicas");
        niche.put("interestCategory", "Categoria");
        niche.put("roleCategory", "Papel");
        if (includeExtraFields) {
            List<Map<String, Object>> detailedDescriptions = List.of(buildDetailedDescriptionContext());
            niche.put("detailedDescriptions", detailedDescriptions);
            niche.put("latestDetailedDescription", buildDetailedDescriptionContext());
            niche.put("hypothesisDetailedDescription", buildDetailedDescriptionContext());
            niche.put("differentiatedTechnology", buildDifferentiatedTechnologyContext());
        }
        return niche;
    }

    private Map<String, Object> buildDetailedDescriptionContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", 1L);
        context.put("title", "Título");
        context.put("description", "Descrição detalhada");
        context.put("pains", "Dores");
        context.put("desires", "Desejos");
        context.put("needs", "Necessidades");
        context.put("model", "gpt-4o-mini");
        context.put("prompt", "Prompt");
        context.put("createdAt", Instant.now());
        context.put("updatedAt", Instant.now());
        return context;
    }

    private Map<String, Object> buildTechnologyContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", 1L);
        context.put("name", "Tecnologia");
        context.put("description", "Descrição");
        context.put("promptText", "Instruções adicionais");
        context.put("createdAt", Instant.now());
        context.put("updatedAt", Instant.now());
        return context;
    }

    private Map<String, Object> buildDifferentiatedTechnologyContext() {
        return buildTechnologyContext();
    }

    private Map<String, Object> buildAttributeContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", 1L);
        context.put("name", "title");
        context.put("description", "Descrição do campo");
        return context;
    }

    private Map<String, Object> buildJourneyContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", 1L);
        context.put("name", "Jornada Exemplo");
        context.put("description", "Sequência automatizada de nurture");
        context.put("status", "DRAFT");
        context.put("segmentReference", "crm-segment-42");
        context.put("segmentFilter", "lead.score > 70");
        context.put("startAt", Instant.now());
        context.put("endAt", Instant.now().plusSeconds(86400));
        context.put("metadata", Map.of("canal", "WhatsApp", "objetivo", "agendamento"));
        return context;
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
