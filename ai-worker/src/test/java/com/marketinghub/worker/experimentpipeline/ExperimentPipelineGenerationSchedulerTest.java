package com.marketinghub.worker.experimentpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: proteger o isolamento operacional do scheduler de conteúdo dos experimentos. */
class ExperimentPipelineGenerationSchedulerTest {

    /** Garante que o polling comercial utilize exclusivamente seu agendador dedicado. */
    @Test
    void shouldUseDedicatedScheduler() throws NoSuchMethodException {
        Method runMethod = ExperimentPipelineGenerationScheduler.class.getDeclaredMethod("run");
        Scheduled scheduled = runMethod.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 */1 * * * *");
        assertThat(scheduled.scheduler()).isEqualTo("experimentPipelineTaskScheduler");
    }
}
