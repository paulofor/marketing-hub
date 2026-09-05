package com.marketinghub.facebookadsworker.facebookcampaign;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Traduz as datas autorizadas do backend para o período efetivo do conjunto de anúncios. */
public record CampaignSchedule(String startTime, String endTime) {
    /** Bloqueia períodos ausentes, invertidos ou vencidos antes de qualquer criação na Meta. */
    public static CampaignSchedule from(String startDate, String endDate, Instant now) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("O período autorizado da campanha é obrigatório.");
        }
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        Instant beginsAt = start.atStartOfDay(zone).toInstant();
        Instant endsAt = end.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1);
        if (end.isBefore(start) || !endsAt.isAfter(now)) {
            throw new IllegalArgumentException("O período autorizado terminou ou está invertido; revise as datas no experimento.");
        }
        return new CampaignSchedule(beginsAt.isAfter(now) ? beginsAt.toString() : null, endsAt.toString());
    }
}
