package com.marketinghub.leadportal.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record SimpleImageBriefing(
        String flowSlug,
        String activityType,
        String professionalName,
        String email,
        String studioName,
        String location,
        List<String> services,
        List<String> customServices,
        String contactChannel,
        String contactDetail,
        Map<String, Object> answers) {

    public SimpleImageBriefing {
        services = services == null ? List.of() : List.copyOf(services);
        customServices = customServices == null ? List.of() : List.copyOf(customServices);
        answers = answers == null ? Map.of() : Collections.unmodifiableMap(answers);
    }

    public List<String> resolvedServices() {
        if (!services.isEmpty()) {
            return services;
        }
        if (!customServices.isEmpty()) {
            return customServices;
        }
        return activityType == null ? List.of() : List.of(activityType);
    }

    public String contactSummary() {
        String channel = contactChannel;
        String value = contactDetail;
        if (isBlank(value) && !isBlank(email)) {
            value = email;
            channel = isBlank(channel) ? "E-mail" : channel;
        }
        if (isBlank(value)) {
            return "Contato não informado";
        }
        if (isBlank(channel)) {
            return value;
        }
        return channel + ": " + value;
    }

    public Map<String, Object> answers() {
        return answers;
    }

    public String resolvedLocation() {
        if (!isBlank(location)) {
            return location;
        }
        if (!isBlank(studioName)) {
            return studioName;
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
