package com.marketinghub.landinggeneratoragent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Responsabilidade: isolar produção, reconexão e telemetria agendadas de Dédalo. */
@Configuration
public class LandingGeneratorSchedulingConfig {

  /** Cria concorrência suficiente para autenticação nunca bloquear a correção de landing. */
  @Bean(name = "taskScheduler")
  public ThreadPoolTaskScheduler taskScheduler(
      @Value("${landing-generator-agent.scheduling.pool-size:3}") int poolSize) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(Math.max(3, poolSize));
    scheduler.setThreadNamePrefix("landing-generator-scheduled-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    return scheduler;
  }
}
