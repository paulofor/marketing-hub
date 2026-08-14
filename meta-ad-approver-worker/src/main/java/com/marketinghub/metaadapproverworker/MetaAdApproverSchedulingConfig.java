package com.marketinghub.metaadapproverworker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Responsabilidade: isolar as rotinas agendadas de revisão e reconexão do Aprovador Meta. */
@Configuration
public class MetaAdApproverSchedulingConfig {

  /** Cria concorrência suficiente para uma reconexão Codex não bloquear a fila de pareceres. */
  @Bean(name = "taskScheduler")
  public ThreadPoolTaskScheduler taskScheduler(
      @Value("${meta-ad-approver.scheduling.pool-size:2}") int poolSize) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(Math.max(2, poolSize));
    scheduler.setThreadNamePrefix("meta-ad-approver-scheduled-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    return scheduler;
  }
}
