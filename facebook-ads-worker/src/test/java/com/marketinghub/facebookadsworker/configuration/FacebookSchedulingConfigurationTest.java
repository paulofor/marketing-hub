package com.marketinghub.facebookadsworker.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalScheduler;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Comprova isolamento da renovação sem introduzir concorrência entre operações de campanha. */
class FacebookSchedulingConfigurationTest {
    /** Ocupa a fila padrão e exige execução real da anotação de renovação em outra thread. */
    @Test
    void renewalRunsWhileOperationalQueueIsOccupied() throws Exception {
        CountDownLatch busy = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch renewed = new CountDownLatch(1);
        CountDownLatch secondOperation = new CountDownLatch(1);
        AtomicReference<String> renewalThread = new AtomicReference<>();
        FacebookTokenRenewalService service = mock(FacebookTokenRenewalService.class);
        doAnswer(call -> {
            if (busy.getCount() == 0) {
                renewalThread.set(Thread.currentThread().getName());
                renewed.countDown();
            }
            return null;
        }).when(service).renewTokensIfNeeded();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("facebook.token-renewal.scheduler.delay", "25")));
            context.registerBean(FacebookTokenRenewalService.class, () -> service);
            context.register(SchedulingEnabled.class, FacebookSchedulingConfiguration.class, FacebookTokenRenewalScheduler.class);
            context.refresh();
            ThreadPoolTaskScheduler operational = context.getBean("taskScheduler", ThreadPoolTaskScheduler.class);
            operational.schedule(() -> {
                busy.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }, Instant.now());
            try {
                assertThat(busy.await(2, TimeUnit.SECONDS)).isTrue();
                operational.schedule(secondOperation::countDown, Instant.now());
                assertThat(renewed.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(renewalThread.get()).startsWith("facebook-token-renewal-");
                assertThat(secondOperation.getCount()).isEqualTo(1);
            } finally {
                release.countDown();
            }
            assertThat(secondOperation.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
        }
    }

    /** O agendamento real precisa detectar token de uma hora antes de ele vencer. */
    @Test
    void defaultRenewalCadenceFitsShortLivedToken() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(FacebookTokenRenewalService.class, () -> mock(FacebookTokenRenewalService.class));
            context.register(SchedulingEnabled.class, FacebookSchedulingConfiguration.class, FacebookTokenRenewalScheduler.class);
            context.refresh();
            var tasks = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks();
            assertThat(tasks).hasSize(1);
            IntervalTask task = (IntervalTask) tasks.iterator().next().getTask();
            assertThat(task.getIntervalDuration()).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(5));
        }
    }

    /** Habilita o processamento real de agendamentos somente no contexto isolado. */
    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingEnabled {}
}
