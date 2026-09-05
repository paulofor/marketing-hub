package com.marketinghub.product.privatereading;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.humanactivity.StandardHumanProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.product.Product;
import com.marketinghub.product.privatereading.controller.PdePrivateReadingController;
import com.marketinghub.product.privatereading.infrastructure.PdePrivateReadingClient;
import com.marketinghub.product.privatereading.service.PdePrivateReadingService;
import com.marketinghub.product.service.privatevalidation.PdePrivateReadingHumanActivityHandler;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: executar somente os componentes reais da leitura com persistência BPM simulada.
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration(
    exclude = {
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
      LiquibaseAutoConfiguration.class
    })
@Import({
  PdePrivateReadingController.class,
  PdePrivateReadingService.class,
  PdePrivateReadingClient.class,
  PdePrivateReadingHumanActivityHandler.class,
  StandardHumanProductProcessActivityExecutor.class,
  MiraReadingSandbox.Commands.class
})
public class MiraReadingSandbox {
  private final Map<Long, BusinessProcessActivityInstance> saved = new ConcurrentHashMap<>();

  /**
   * Inicia um processo exclusivo de testes, sem carregar configurações produtivas do repositório.
   */
  public static void main(String[] args) {
    System.setProperty(
        "spring.config.location", "optional:classpath:/mira-sandbox-empty.properties");
    SpringApplication.run(MiraReadingSandbox.class, args);
  }

  /** Fornece somente um produto sintético com a mesma versão de contrato de Mira. */
  @Bean
  ProductRepository products() {
    var repository = mock(ProductRepository.class);
    when(repository.findById(10L)).thenAnswer(call -> Optional.of(product()));
    return repository;
  }

  /** Simula gravação confirmada de BPM; os eventos PDE continuam em MySQL real e isolado. */
  @Bean
  BusinessProcessActivityInstanceRepository instances() {
    var repository = mock(BusinessProcessActivityInstanceRepository.class);
    when(repository.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), anyString()))
        .thenAnswer(call -> Optional.ofNullable(saved.get(call.getArgument(0))));
    when(repository
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                anyLong(), anyString()))
        .thenAnswer(call -> List.copyOf(saved.values()));
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              BusinessProcessActivityInstance value = call.getArgument(0);
              value.setId(value.getActivityDefinition().getId());
              return value;
            });
    when(repository.save(any()))
        .thenAnswer(
            call -> {
              BusinessProcessActivityInstance value = call.getArgument(0);
              saved.put(value.getActivityDefinition().getId(), value);
              return value;
            });
    return repository;
  }

  /**
   * Simula somente o predecessor, preservando a exigência de concluir a primeira antes da segunda.
   */
  @Bean
  ProductProcessActivityPredecessorService predecessor() {
    var service = mock(ProductProcessActivityPredecessorService.class);
    when(service.readiness(any(), any(), anyString()))
        .thenAnswer(
            call -> {
              BusinessProcessActivityDefinition activity = call.getArgument(1);
              boolean ready =
                  !"privateReading2".equals(activity.getActivityId())
                      || saved.values().stream()
                          .anyMatch(
                              value ->
                                  value.isObjectiveAchieved()
                                      && value.getActivityDefinition().getId() == 601L);
              return new ProductProcessActivityPredecessorReadiness(
                  ready,
                  ready ? "Predecessor confirmado localmente" : "Conclua a primeira leitura");
            });
    return service;
  }

  /** Cria o produto da homologação sem URL produtiva, pessoa real ou orçamento. */
  static Product product() {
    String accepted = Instant.now().minusSeconds(3600).toString();
    return Product.builder()
        .id(10L)
        .commercialStatus("PLANNED")
        .internalName("Mira — teste local")
        .validationDefinitionVersion("PDE_PRIVATE_VALIDATION_V1")
        .validationDefinitionJson(
            """
        {"privateValidationPlan":{"minimumIndependentReadings":2,"minimumEligibleParticipantsPerReading":1,
        "criteriaDeclaredAt":"%s","sourceMaxAgeDays":30,"minimumExperienceStartRate":1,"minimumValueMomentRate":1,
        "minimumReadyResultUseRate":1,"minimumPrototypePreferenceRate":1,"minimumCheckoutStartRate":1,
        "requiredSignals":["EXPERIENCE_STARTED","VALUE_MOMENT","READY_RESULT_USED","PREFERRED_OVER_FREE","CHECKOUT_STARTED"]},
        "privatePrototype":{"checkoutMode":"SIMULATED_NO_CHARGE"},"privatePrototypeAcceptance":{
        "status":"READY","sourceQualityPassed":true,"sourceQualityEvaluatedAt":"%s","acceptedAt":"%s",
        "privateAccessUrl":"https://mira.sandbox.local/mira-private","prototypeVersion":"mira-private-v1",
        "instrumentationReference":"events:local","sourceEvidenceReference":"fixture:local","privateAccessConfirmed":true,
        "desktopValidated":true,"mobileValidated":true,"paymentEnabled":false,"published":false,"mediaSpendBrl":0,
        "eventSource":"FIRST_PARTY_EVENTS","testMarker":"PRIVATE_PROTOTYPE"}}
        """
                .formatted(accepted, accepted, accepted))
        .build();
  }

  /** Cria a versão sintética do processo sem modificar nenhum catálogo produtivo. */
  static BusinessProcessDefinition process() {
    var process = new BusinessProcessDefinition();
    process.setId(68L);
    process.setProcessCode("pde-construction-approval");
    process.setStatus("PUBLISHED");
    return process;
  }

  /** Resolve somente as duas atividades da homologação isolada. */
  static BusinessProcessActivityDefinition activity(String code) {
    if (!List.of("privateReading1", "privateReading2").contains(code))
      throw new IllegalArgumentException("Atividade local inválida");
    var activity = new BusinessProcessActivityDefinition();
    activity.setId("privateReading1".equals(code) ? 601L : 602L);
    activity.setActivityId(code);
    activity.setOwnerName("Operador humano");
    activity.setProcessDefinition(process());
    return activity;
  }

  /** Responsabilidade: conectar a interface de homologação ao executor BPM real. */
  @RestController
  public static class Commands {
    private final StandardHumanProductProcessActivityExecutor executor;

    /** Recebe o executor de decisões humanas que será exercitado pelo navegador. */
    Commands(StandardHumanProductProcessActivityExecutor executor) {
      this.executor = executor;
    }

    /** Projeta o controle real da atividade numa página exclusiva da homologação. */
    @GetMapping("/api/local/mira/activities/{code}")
    Map<String, Object> activityControl(@PathVariable String code) {
      var readiness =
          executor.readiness(
              process(), activity(code), product(), "product:10@private-validation-v1");
      Map<String, Object> control = new LinkedHashMap<>();
      control.put("executorType", "HUMAN");
      control.put("interactionType", "APPROVAL");
      control.put("actionAvailable", readiness.ready());
      control.put("actionLabel", readiness.actionLabel());
      control.put("availabilityReason", readiness.reason());
      control.put("description", readiness.description());
      control.put("confirmationTitle", readiness.confirmationTitle());
      control.put("confirmationMessage", readiness.confirmationMessage());
      control.put("confirmationToken", readiness.confirmationToken());
      control.put("workspaceCode", readiness.workspaceCode());
      control.put("workspaceReferenceId", readiness.workspaceReferenceId());
      control.put("decisionMode", readiness.decisionMode());
      control.put("requirements", readiness.requirements());
      return Map.of(
          "activityId",
          code,
          "activityName",
          "Leitura privada — homologação",
          "operationalState",
          "NOT_STARTED",
          "executionControl",
          control);
    }

    /** Delega o comando do navegador ao mesmo executor usado pela API produtiva. */
    @PostMapping("/api/business-processes/68/products/10/activities/{code}/execution-requests")
    Object execute(
        @PathVariable String code, @RequestBody ProductProcessActivityExecutionRequest request) {
      return executor.execute(
          process(), activity(code), product(), "product:10@private-validation-v1", request);
    }

    /** Mantém a falha legível sem esconder recusa de contrato no teste integrado. */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<Map<String, String>> invalid(RuntimeException ex) {
      org.slf4j.LoggerFactory.getLogger(Commands.class)
          .warn("Recusa de comando na homologação local de Mira", ex);
      return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
  }
}
