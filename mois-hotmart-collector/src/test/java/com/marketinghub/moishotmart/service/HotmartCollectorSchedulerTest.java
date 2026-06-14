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
     * Garante que o ciclo 1 esteja agendado para 22:00 de 14 de junho de 2026 no fuso de São Paulo.
     */
    @Test
    void shouldScheduleFirstCycleAtTwentyTwoHundredOnJuneFourteenth() throws NoSuchMethodException {
        Method method = HotmartCollectorScheduler.class
                .getDeclaredMethod("collectFirstCycleAtTwentyTwoHundredOnJuneFourteenth2026");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("0 0 22 14 6 *", scheduled.cron());
        assertEquals("America/Sao_Paulo", scheduled.zone());
    }

    /**
     * Garante que o ciclo 2 esteja agendado para 12:20 de 13 de junho de 2026 no fuso de São Paulo.
     */
    @Test
    void shouldScheduleSecondCycleAtTwelveTwentyOnJuneThirteenth() throws NoSuchMethodException {
        Method method = HotmartCollectorScheduler.class
                .getDeclaredMethod("collectSecondCycleAtTwelveTwentyOnJuneThirteenth2026");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("0 20 12 13 6 *", scheduled.cron());
        assertEquals("America/Sao_Paulo", scheduled.zone());
    }
}
