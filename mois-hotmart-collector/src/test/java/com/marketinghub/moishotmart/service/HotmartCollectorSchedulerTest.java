package com.marketinghub.moishotmart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Valida os agendamentos automáticos configurados para os ciclos Hotmart.
 */
class HotmartCollectorSchedulerTest {

    /**
     * Garante que o ciclo 1 esteja agendado para 01:35 de 13 de junho de 2026 no fuso de São Paulo.
     */
    @Test
    void shouldScheduleFirstCycleAtOneThirtyFiveOnJuneThirteenth() throws NoSuchMethodException {
        Method method = HotmartCollectorScheduler.class
                .getDeclaredMethod("collectFirstCycleAtOneThirtyFiveOnJuneThirteenth2026");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("0 35 1 13 6 *", scheduled.cron());
        assertEquals("America/Sao_Paulo", scheduled.zone());
    }
}
