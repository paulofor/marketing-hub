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
     * Garante que o ciclo 1 esteja agendado para 14:45 de 15 de junho de 2026 no fuso de São Paulo.
     */
    @Test
    void shouldScheduleFirstCycleAtFourteenFortyFiveOnJuneFifteenth() throws NoSuchMethodException {
        Method method = HotmartCollectorScheduler.class
                .getDeclaredMethod("collectFirstCycleAtFourteenFortyFiveOnJuneFifteenth2026");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("0 45 14 15 6 *", scheduled.cron());
        assertEquals("America/Sao_Paulo", scheduled.zone());
    }

    /**
     * Garante que o ciclo 2 esteja agendado para 04:10 de 16 de junho de 2026 no fuso de São Paulo.
     */
    @Test
    void shouldScheduleSecondCycleAtFourTenOnJuneSixteenth() throws NoSuchMethodException {
        Method method = HotmartCollectorScheduler.class
                .getDeclaredMethod("collectSecondCycleAtFourTenOnJuneSixteenth2026");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("0 10 4 16 6 *", scheduled.cron());
        assertEquals("America/Sao_Paulo", scheduled.zone());
    }
}
