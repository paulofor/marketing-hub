package com.marketinghub.worker.adset;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a planned ad set returned by the ChatGPT client.
 */
public record AdSetPlan(
        String location,
        List<String> interests,
        List<String> jobTitles,
        List<String> behaviors,
        BigDecimal budget,
        Integer durationDays,
        String targetingJson,
        String prompt,
        String model
) {
}
