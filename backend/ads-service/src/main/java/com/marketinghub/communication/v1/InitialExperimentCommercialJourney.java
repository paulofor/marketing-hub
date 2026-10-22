package com.marketinghub.communication.v1;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.service.IntegratedPdeJourneyEvidenceService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: reconhecer a passagem do primeiro experimento para a integração comercial. */
@Component
@RequiredArgsConstructor
public class InitialExperimentCommercialJourney
    implements AgentProductProcessActivityReadinessProvider {
  private final ExperimentRepository experiments;
  private final LearningSalesCycleRepository cycles;
  private final PdeCommercialCommunicationDestination destination;
  private final IntegratedPdeJourneyEvidenceService integration;
  private final BusinessProcessActivityInstanceRepository instances;

  /**
   * Reconhece a preparação explícita do destino, sem promover provas privadas nem liberar mídia.
   */
  @Transactional(readOnly = true)
  public boolean applies(String reference) {
    return experiment(reference)
        .filter(value -> destination.readiness(value.getProduct(), reference).isPresent())
        .isPresent();
  }

  /** Restringe o novo aceite à integração canônica; destino mantém seu provedor existente. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return "pde-communication-sales-journey".equals(process.getProcessCode())
        && "integration".equals(activity.getActivityId());
  }

  /** Expõe falhas do destino comercial sem voltar silenciosamente à experiência privada. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    if (applies(reference)) {
      var ready = destination.readiness(product, reference).orElseThrow();
      return new AgentProductProcessActivityReadiness(ready.ready(), ready.reason());
    }
    return new AgentProductProcessActivityReadiness(
        true, "A integração conserva os controles do regime vigente.");
  }

  /** Reabre somente a integração concluída que ainda não comprova o destino comercial próprio. */
  @Override
  @Transactional(readOnly = true)
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    if (!supports(process, activity) || !applies(reference)) return false;
    return instances
        .findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), reference)
        .filter(value -> "COMPLETED".equals(value.getStatus()) && value.isObjectiveAchieved())
        .filter(
            value -> {
              var current = experiment(reference).orElseThrow();
              return !Objects.equals(product.getId(), current.getProduct().getId())
                  || !integration.isReady(current);
            })
        .isPresent();
  }

  /** Mantém ciclos, sucessores e experimentos encerrados fora desta passagem inicial. */
  private Optional<Experiment> experiment(String reference) {
    if (reference == null || !reference.matches("experiment:[1-9][0-9]{0,17}"))
      return Optional.empty();
    long id = Long.parseLong(reference.substring(11));
    if (cycles.findByExperimentId(id).isPresent()) return Optional.empty();
    return experiments
        .findById(id)
        .filter(value -> value.getProduct() != null && value.getSourceExperiment() == null)
        .filter(value -> value.getStatus() == ExperimentStatus.PLANNED);
  }
}
