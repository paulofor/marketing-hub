package com.marketinghub.communication.v1;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: escolher entre destino privado aprovado e produção de landing comercial. */
@Service
@RequiredArgsConstructor
public class CommunicationDestinationActivityExecutor
    implements BackendProductProcessActivityExecutor {
  private final PrivateCommunicationJourney privateJourney;
  private final BusinessProcessDefinitionRepository processes;

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
    if (!privateJourney.applies(reference))
      throw new IllegalStateException(
          "Execute o subprocesso oficial para produzir a landing deste contrato.");
    return privateJourney.complete(process, activity, product, reference);
  }
}
