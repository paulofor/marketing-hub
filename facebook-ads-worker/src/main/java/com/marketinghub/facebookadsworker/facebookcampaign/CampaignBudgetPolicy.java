package com.marketinghub.facebookadsworker.facebookcampaign;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Calcula proteção nativa equivalente quando a Meta rejeita o mínimo de spend_cap. */
public final class CampaignBudgetPolicy {
    /** Impede instanciação da política determinística. */
    private CampaignBudgetPolicy() {}

    /** Limita o orçamento vitalício ao teto e aos dias ainda autorizados, sem recuperar dias perdidos. */
    public static String remainingLifetimeBudget(
            BigDecimal dailyBudget, BigDecimal totalLimit, String startDate, String endDate, Instant now) {
        if (dailyBudget == null || totalLimit == null || dailyBudget.signum() <= 0
                || totalLimit.compareTo(dailyBudget) < 0) {
            throw new IllegalArgumentException("Orçamento diário e teto total válidos são obrigatórios.");
        }
        CampaignSchedule.from(startDate, endDate, now);
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        LocalDate today = now.atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate();
        LocalDate effectiveStart = start.isAfter(today) ? start : today;
        long days = ChronoUnit.DAYS.between(effectiveStart, end) + 1;
        return dailyBudget.multiply(BigDecimal.valueOf(days)).min(totalLimit)
                .movePointRight(2).setScale(0, RoundingMode.DOWN).toPlainString();
    }
}
