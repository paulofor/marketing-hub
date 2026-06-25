package com.marketinghub.nichocnaev3.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

/** Valida o contrato de agendamento único do executor NichoCNAE v3. */
class NichoCnaeV3PendingExecutionSchedulerTest {
    /** Confirma que a varredura v3 roda a cada três minutos. */
    @Test
    void shouldRunVersionThreePendingScanEveryThreeMinutes() throws NoSuchMethodException {
        Method method = NichoCnaeV3PendingExecutionScheduler.class.getDeclaredMethod("processPendingStageExecutions");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals("0 */3 * * * *", scheduled.cron());
    }
}
