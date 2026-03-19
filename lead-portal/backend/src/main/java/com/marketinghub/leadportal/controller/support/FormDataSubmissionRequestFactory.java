package com.marketinghub.leadportal.controller.support;

import com.marketinghub.leadportal.dto.FlowSubmissionRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Component
public class FormDataSubmissionRequestFactory {

    private static final List<String> NAME_KEYS = List.of(
            "name", "nome", "full_name", "fullname", "first_name", "cliente_nome", "responsavel_nome");
    private static final List<String> EMAIL_KEYS = List.of(
            "email", "e-mail", "email_address", "contato_email", "email_contato", "responsavel_email");
    private static final List<String> CAMPAIGN_KEYS = List.of("campaignCode", "campaign", "utm_campaign");
    private static final List<String> IMAGE_KEYS = List.of("imageKey", "image_key", "imagemKey", "fotoKey");
    private static final Set<String> IGNORED_KEYS = Set.of("payload");

    public FlowSubmissionRequest fromFormData(String flowSlug, MultiValueMap<String, String> formData) {
        FlowSubmissionRequest request = new FlowSubmissionRequest();
        request.setName(resolveFirst(formData, NAME_KEYS).orElseGet(() -> defaultName(flowSlug)));
        request.setEmail(resolveFirst(formData, EMAIL_KEYS).orElseGet(() -> defaultEmail(flowSlug)));
        request.setCampaignCode(resolveFirst(formData, CAMPAIGN_KEYS).orElse(null));
        request.setImageKey(resolveFirst(formData, IMAGE_KEYS).orElse(null));
        request.setAnswers(extractAnswers(formData));
        return request;
    }

    private Map<String, Object> extractAnswers(MultiValueMap<String, String> formData) {
        Map<String, Object> answers = new LinkedHashMap<>();
        if (formData == null) {
            return answers;
        }
        formData.forEach((key, values) -> {
            if (!StringUtils.hasText(key) || IGNORED_KEYS.contains(key)) {
                return;
            }
            Object sanitized = sanitizeValues(values);
            if (sanitized != null) {
                answers.put(key, sanitized);
            }
        });
        return answers;
    }

    private Object sanitizeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> sanitized = values.stream()
                .map(value -> value == null ? null : value.trim())
                .filter(StringUtils::hasText)
                .toList();
        if (sanitized.isEmpty()) {
            return null;
        }
        return sanitized.size() == 1 ? sanitized.get(0) : sanitized;
    }

    private Optional<String> resolveFirst(MultiValueMap<String, String> formData, List<String> candidateKeys) {
        if (formData == null || candidateKeys == null) {
            return Optional.empty();
        }
        return candidateKeys.stream()
                .map(formData::getFirst)
                .map(value -> value == null ? null : value.trim())
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private String defaultName(String flowSlug) {
        if (StringUtils.hasText(flowSlug)) {
            return "Lead do fluxo " + flowSlug.trim();
        }
        return "Lead portal";
    }

    private String defaultEmail(String flowSlug) {
        String base = StringUtils.hasText(flowSlug) ? flowSlug.trim().toLowerCase(Locale.ROOT) : "lead-portal";
        String normalized = base.replaceAll("[^a-z0-9]+", "-");
        if (!StringUtils.hasText(normalized)) {
            normalized = "lead-portal";
        }
        return normalized + "@lead-portal.local";
    }
}
