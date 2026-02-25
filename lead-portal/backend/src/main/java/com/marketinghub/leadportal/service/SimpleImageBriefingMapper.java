package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.model.FlowSubmission;
import com.marketinghub.leadportal.model.SimpleImageBriefing;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SimpleImageBriefingMapper {

    private static final String SIMPLE_FORM_PREFIX = "formulario-simples-";

    public Optional<SimpleImageBriefing> map(String flowSlug, FlowSubmission submission) {
        if (!StringUtils.hasText(flowSlug) || !flowSlug.startsWith(SIMPLE_FORM_PREFIX) || submission == null) {
            return Optional.empty();
        }

        Map<String, Object> answers = submission.answers() == null ? Map.of() : submission.answers();
        Map<String, Object> normalizedAnswers = new LinkedHashMap<>(answers);

        String professionalName = firstNonBlank(valueAsString(answers.get("nome")), submission.name());
        String studioName = firstNonBlank(
                valueAsString(answers.get("studio")),
                valueAsString(answers.get("studio_nome")),
                valueAsString(answers.get("academia_ou_studio")),
                valueAsString(answers.get("empresa")));
        String location = firstNonBlank(
                valueAsString(answers.get("local_trabalho")),
                valueAsString(answers.get("onde_trabalha")),
                composeLocation(answers));

        List<String> services = valueAsList(answers.get("tipo_aulas"));
        List<String> customServices = splitFreeText(firstNonBlank(
                valueAsString(answers.get("outras_aulas")),
                valueAsString(answers.get("servicos_digitados"))));

        String contactChannel = firstNonBlank(
                valueAsString(answers.get("forma_contato")),
                valueAsString(answers.get("melhor_contato")),
                valueAsString(answers.get("canal_contato")));
        String contactDetail = firstNonBlank(
                valueAsString(answers.get("whatsapp")),
                valueAsString(answers.get("telefone")),
                valueAsString(answers.get("instagram")),
                valueAsString(answers.get("contato")),
                valueAsString(answers.get("email")),
                submission.email());

        String activityType = resolveActivityType(flowSlug);

        return Optional.of(new SimpleImageBriefing(
                flowSlug,
                activityType,
                professionalName,
                submission.email(),
                studioName,
                location,
                services,
                customServices,
                contactChannel,
                contactDetail,
                normalizedAnswers));
    }

    private static String resolveActivityType(String slug) {
        String normalized = slug.substring(SIMPLE_FORM_PREFIX.length());
        normalized = normalized.replace('-', ' ').trim();
        if (!StringUtils.hasText(normalized)) {
            return "profissional";
        }
        return normalized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String composeLocation(Map<String, Object> answers) {
        String city = valueAsString(answers.get("cidade"));
        String neighborhood = valueAsString(answers.get("bairro"));
        String state = valueAsString(answers.get("estado"));
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(city)) {
            builder.append(city.trim());
        }
        if (StringUtils.hasText(state)) {
            if (builder.length() > 0) {
                builder.append(" - ");
            }
            builder.append(state.trim());
        }
        if (StringUtils.hasText(neighborhood)) {
            if (builder.length() > 0) {
                builder.append(", bairro ");
            }
            builder.append(neighborhood.trim());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private static String valueAsString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list && !list.isEmpty()) {
            return valueAsString(list.get(0));
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static List<String> valueAsList(Object value) {
        if (value == null) {
            return List.of();
        }
        List<String> collected = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                String resolved = valueAsString(item);
                if (resolved != null) {
                    collected.add(resolved);
                }
            }
        } else {
            String resolved = valueAsString(value);
            if (resolved != null) {
                collected.add(resolved);
            }
        }
        return collected;
    }

    private static List<String> splitFreeText(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        String[] tokens = value.split("[,\\n]");
        List<String> normalized = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (StringUtils.hasText(token)) {
                normalized.add(token.trim());
            }
        }
        return normalized;
    }
}
