package com.marketinghub.salesvideo.service.clipplan;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.repository.jpa.salesvideo.*;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.autonomy.v1.*;
import com.marketinghub.salesvideo.service.*;
import com.marketinghub.salesvideo.service.providerpreflight.VideoProviderFinancialPreflightService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.*;

/**
 * Responsabilidade: homologar o editor real com comandos canônicos HTTP e dependências locais sem
 * gasto.
 */
@Configuration(proxyBeanMethods = false)
@Profile("studio-continuation-local")
@Import(com.marketinghub.web.ApiExceptionHandler.class)
@EnableAutoConfiguration(
    exclude = {
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
      JpaRepositoriesAutoConfiguration.class
    })
public class StudioContinuationLocalApplication {
  private final VideoProjectRepository recordingProjects = mock(VideoProjectRepository.class);

  /** Inicia somente em loopback; o cenário não possui credenciais nem clientes de providers. */
  public static void main(String[] args) {
    var app = new SpringApplication(StudioContinuationLocalApplication.class);
    app.setAdditionalProfiles("studio-continuation-local");
    app.setDefaultProperties(
        Map.of(
            "server.port",
            "18081",
            "server.address",
            "127.0.0.1",
            "spring.main.banner-mode",
            "off"));
    app.run(args);
  }

  /** Aplica o filtro oficial de tenant inclusive aos comandos novos. */
  @Bean
  com.marketinghub.salesvideo.tenant.TenantContextFilter tenantFilter() {
    return new com.marketinghub.salesvideo.tenant.TenantContextFilter();
  }

  /** Mantém o catálogo real já usado pela homologação de tomadas. */
  @Bean
  com.marketinghub.salesvideo.controller.SalesVideoController catalogController() {
    var controller = new StudioLocalApplication().controller();
    var repository = mock(VideoProductRecordingRepository.class);
    Map<Long, VideoProductRecording> rows = new LinkedHashMap<>();
    when(repository.findByVideoProjectIdAndSha256(any(), any()))
        .thenAnswer(
            a ->
                rows.values().stream()
                    .filter(
                        r ->
                            Objects.equals(r.getVideoProjectId(), a.getArgument(0))
                                && Objects.equals(r.getSha256(), a.getArgument(1)))
                    .findFirst());
    when(repository.findById(any()))
        .thenAnswer(a -> Optional.ofNullable(rows.get(a.getArgument(0))));
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            a -> {
              var row = (VideoProductRecording) a.getArgument(0);
              row.setId((long) rows.size() + 91000);
              rows.put(row.getId(), row);
              return row;
            });
    var recordings =
        new VideoProductRecordingService(
            repository,
            recordingProjects,
            mock(VideoProductionSpendingPolicy.class),
            new ObjectMapper());
    org.springframework.test.util.ReflectionTestUtils.setField(
        org.springframework.test.util.ReflectionTestUtils.getField(controller, "salesVideoService"),
        "recordings",
        recordings);
    return controller;
  }

  /**
   * Exercita controller e coordenador reais com persistência e executores sintéticos explícitos.
   */
  @Bean
  VideoProductionCycleController cycleController() {
    var projects = recordingProjects;
    var cycles = mock(VideoProductionCycleRepository.class);
    var jobs = mock(SalesVideoJobRepository.class);
    var tasks = mock(AgentTaskService.class);
    var facade = mock(SalesVideoService.class);
    var financial = mock(com.marketinghub.financialagent.service.FinancialAgentService.class);
    var ledger = mock(com.marketinghub.financialagent.service.StudioCostLedgerService.class);
    var preflights = mock(VideoProviderFinancialPreflightService.class);
    Map<Long, VideoProject> projectRows = new LinkedHashMap<>();
    Map<Long, VideoProductionCycle> cycleRows = new LinkedHashMap<>();
    for (long offset : List.of(0L, 100L, 200L, 400L)) {
      for (long base : List.of(14L, 81L)) {
        long projectId = base + offset;
        var project =
            VideoProject.builder()
                .id(projectId)
                .tenantId("studio-test")
                .productId(base == 14 ? 4L : 44L)
                .experimentId(base == 14 ? 114L : 215L)
                .salesVideoProfileId(projectId + 55)
                .campaignKey("fixture-local-" + projectId)
                .title("Projeto sintético " + projectId)
                .targetDurationSeconds(32)
                .targetChannel("SOCIAL_REELS_STORIES")
                .providerPlan("Provider escolhido no Estudio: Runway Gen-4.5 (RUNWAY).")
                .scenePlan("Gancho\nResultado\nMecanismo\nProva\nCTA")
                .build();
        projectRows.put(projectId, project);
        var cycle = new VideoProductionCycle();
        cycle.setId(projectId + 1000);
        cycle.setVideoProjectId(projectId);
        cycle.setProductId(project.getProductId());
        cycle.setExperimentId(project.getExperimentId());
        cycle.setBudgetLimitUsd(new BigDecimal("15"));
        cycle.setKnownCostUsd(BigDecimal.ZERO);
        cycle.setCreatedAt(Instant.now());
        cycle.setUpdatedAt(Instant.now());
        cycle.setStatus(
            offset >= 400
                ? "PENDING_PRODUCT_PROOF"
                : base == 14 ? "QUEUED_FOR_APOLLO" : "PROVIDER_PREFLIGHT_BLOCKED");
        if (base == 14 && offset < 400) {
          cycle.setFinancialDecision("APPROVED");
          cycle.setSalesVideoJobId(projectId + 23000);
          var profile = new SalesVideoProfile();
          profile.setId(project.getSalesVideoProfileId());
          var job = new SalesVideoJob();
          job.setId(cycle.getSalesVideoJobId());
          job.setProfile(profile);
          when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        }
        cycleRows.put(cycle.getId(), cycle);
        when(projects.findById(projectId)).thenReturn(Optional.of(project));
        when(projects.findByIdForUpdate(projectId)).thenReturn(Optional.of(project));
        when(cycles.findByIdForUpdate(cycle.getId())).thenReturn(Optional.of(cycle));
        when(cycles.findByVideoProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(cycle));
      }
    }
    when(projects.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    when(cycles.save(any())).thenAnswer(call -> call.getArgument(0));
    when(financial.intelligence(any())).thenReturn(Map.of("fixture", "LOCAL_ONLY"));
    when(ledger.cycleLedger(any())).thenReturn(Map.of("segregated", true));
    when(preflights.financialContext(any())).thenReturn(Map.of("fixture", "LOCAL_ONLY"));
    when(preflights.reserve(any())).thenReturn(new VideoCreditReservation());
    when(preflights.snapshot(any()))
        .thenAnswer(
            call -> {
              var cycle = cycleRows.get((Long) call.getArgument(0));
              if (!"PROVIDER_PREFLIGHT_BLOCKED".equals(cycle.getStatus())) return null;
              return new ObjectMapper()
                  .convertValue(
                      Map.of(
                          "id",
                          cycle.getId(),
                          "status",
                          "READY_WITH_BLOCKER",
                          "productionProfile",
                          "FINAL_CAMPAIGN",
                          "failureCode",
                          "PROVIDER_FULL_COST_PLAN_REQUIRED",
                          "failureDetail",
                          "Fixture: previsão integral pendente"),
                      com.marketinghub.salesvideo.service.providerpreflight
                          .VideoProviderFinancialPreflightData.Snapshot.class);
            });
    when(preflights.reassessFullCosts(any()))
        .thenAnswer(
            call -> {
              var cycle = (VideoProductionCycle) call.getArgument(0);
              var project = projectRows.get(cycle.getVideoProjectId());
              if (project.getProductionCostPlanJson() == null)
                throw new IllegalArgumentException("Previsão não persistida");
              var result = new VideoProviderPreflight();
              result.setStatus("READY");
              return result;
            });
    var task = mock(AgentTaskResponse.class);
    when(task.id()).thenReturn(90001L);
    when(tasks.createGateByAgent(any(), any())).thenReturn(task);
    return new VideoProductionCycleController(
        new VideoProductionCycleService(
            cycles,
            projects,
            jobs,
            tasks,
            facade,
            financial,
            ledger,
            preflights,
            new ObjectMapper().findAndRegisterModules()));
  }
}
