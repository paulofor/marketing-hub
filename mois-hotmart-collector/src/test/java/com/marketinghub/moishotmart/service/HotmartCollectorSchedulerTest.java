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
     * Garante que o ciclo 1 esteja agendado diariamente para 05:00 no fuso de São Paulo.
     */
    @Test
    void shouldScheduleFirstCycleDaily() throws NoSuchMethodException {
        Method method = HotmartCollectorScheduler.class
                .getDeclaredMethod("collectFirstCycleDaily");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("0 0 5 * * *", scheduled.cron());
        assertEquals("America/Sao_Paulo", scheduled.zone());
    }

    /**
     * Garante que o ciclo 2 esteja agendado diariamente para 06:00 no fuso de São Paulo.
     */
    @Test
    void shouldScheduleSecondCycleDaily() throws NoSuchMethodException {
        Method method = HotmartCollectorScheduler.class
                .getDeclaredMethod("collectSecondCycleDaily");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("0 0 6 * * *", scheduled.cron());
        assertEquals("America/Sao_Paulo", scheduled.zone());
    }
}
