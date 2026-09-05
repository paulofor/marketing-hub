package com.marketinghub.facebookadsworker.facebookcampaign;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Protege o término de mídia autorizado sem depender do relógio da execução dos testes. */
class CampaignScheduleTest {
    /** Converte a data final inclusiva para o último segundo do dia em Brasília. */
    @Test
    void preservesAuthorizedEndDate() {
        var schedule = CampaignSchedule.from("2026-09-02", "2026-09-06", Instant.parse("2026-09-05T12:00:00Z"));
        assertNull(schedule.startTime());
        assertEquals("2026-09-07T02:59:59Z", schedule.endTime());
    }

    /** Agenda início futuro sem antecipar o período consentido. */
    @Test
    void preservesFutureStart() {
        var schedule = CampaignSchedule.from("2026-09-08", "2026-09-10", Instant.parse("2026-09-05T12:00:00Z"));
        assertEquals("2026-09-08T03:00:00Z", schedule.startTime());
    }

    /** Bloqueia datas ausentes, invertidas, inválidas e período vencido. */
    @Test
    void rejectsInvalidSchedule() {
        Instant now = Instant.parse("2026-09-07T03:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> CampaignSchedule.from(null, "2026-09-06", now));
        assertThrows(IllegalArgumentException.class, () -> CampaignSchedule.from("2026-09-02", "2026-09-06", now));
        assertThrows(IllegalArgumentException.class, () -> CampaignSchedule.from("2026-09-09", "2026-09-08", now));
        assertThrows(java.time.DateTimeException.class, () -> CampaignSchedule.from("invalid", "2026-09-08", now));
    }
}
