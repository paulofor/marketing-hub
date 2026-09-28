package com.marketinghub.communication.v1;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: escolher entre destino privado aprovado e produção de landing comercial. */
@Service
public class CommunicationDestinationActivityExecutor
    implements BackendProductProcessActivityExecutor {
  private final PrivateCommunicationJourney privateJourney;
  private final BusinessProcessDefinitionRepository processes;
  private final PdeCommercialCommunicationDestination commercialDestination;

  /** Configura as rotas privada, comercial publicada e de geração de landing. */
  @Autowired
  public CommunicationDestinationActivityExecutor(
      PrivateCommunicationJourney privateJourney,
      BusinessProcessDefinitionRepository processes,
      PdeCommercialCommunicationDestination commercialDestination) {
    this.privateJourney = privateJourney;
    this.processes = processes;
    this.commercialDestination = commercialDestination;
  }

  /** Mantém testes focados na rota privada sem exigir um slot comercial. */
  CommunicationDestinationActivityExecutor(
      PrivateCommunicationJourney privateJourney, BusinessProcessDefinitionRepository processes) {
    this(privateJourney, processes, null);
  }

  /** Reconhece somente a decisão de destino do processo canônico de comunicação. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return "pde-communication-sales-journey".equals(process.getProcessCode())
        && "destination".equals(activity.getActivityId());
  }

  /** Reutiliza a prova privada ou preserva a chamada canônica da landing nos demais contratos. */
  @Override
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    if (privateJourney.applies(reference))
      return privateJourney.readiness(process, activity, product, reference);
    if (commercialDestination != null) {
      var published = commercialDestination.readiness(product, reference);
      if (published.isPresent()) return published.orElseThrow();
    }
    if (activity.getSubprocessCode() == null || activity.getSubprocessCode().isBlank())
      return new BackendProductProcessActivityReadiness(
          false, "O destino não possui subprocesso configurado.");
    return processes
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            activity.getSubprocessCode(), "PUBLISHED")
        .map(
            child ->
                new BackendProductProcessActivityReadiness(
                    true,
                    "O contrato requer a geração e aprovação do destino no subprocesso oficial.",
                    "Abrir subprocesso",
                    "Produz e aprova a landing preservando seus gates independentes.",
                    null,
                    null,
                    List.of(),
                    child.getId(),
                    null))
        .orElseGet(
            () ->
                new BackendProductProcessActivityReadiness(
                    false, "O subprocesso do destino ainda não possui uma definição publicada."));
  }

  /** Registra somente a reutilização privada; delegações continuam controladas pelo processo. */
  @Override
  @Transactional
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    if (privateJourney.applies(reference))
      return privateJourney.complete(process, activity, product, reference);
    if (commercialDestination != null
        && commercialDestination.readiness(product, reference).isPresent()) {
      return commercialDestination.complete(process, activity, product, reference);
    }
    throw new IllegalStateException(
        "Execute o subprocesso oficial para produzir a landing deste contrato.");
  }
}
