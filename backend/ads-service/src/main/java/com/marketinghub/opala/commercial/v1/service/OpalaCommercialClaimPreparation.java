package com.marketinghub.opala.commercial.v1.service;

import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskClaimPreparationHook;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: conferir a candidata comercial antes de abrir ou reservar trabalho Opala. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpalaCommercialClaimPreparation implements AgentTaskClaimPreparationHook {
  private static final Set<String> ACTIVITIES =
      Set.of("entry", "creative", "checkout", "targeting");
  private final OpalaCommercialContext context;
  private final OpalaCommercialVersionContract versionContract;

  /** Limita a proteção às quatro atividades de materialização comercial do próprio subprocesso. */
  @Override
  public boolean supports(AgentTask task) {
    return task != null
        && task.getProcessDefinition() != null
        && OpalaCommercialContext.CODE.equals(task.getProcessDefinition().getProcessCode())
        && supportsActivity(task.getProcessActivityId());
  }

  /** Identifica as atividades que precisam do mesmo contrato versionado entregue ao executor. */
  public boolean supportsActivity(String activityId) {
    return activityId != null && ACTIVITIES.contains(activityId);
  }

  /** Reconfere tarefas antigas ainda pendentes sem reservar nem reconstruir sua candidata. */
  @Override
  public Preparation prepare(AgentTask task) {
    return inspect(task.getSourceReference(), null);
  }

  /**
   * Expõe a mesma causa na abertura da atividade e na consulta da fila antes de qualquer modelo.
   */
  public Preparation inspect(String sourceReference, Long productId) {
    try {
      var scope = context.scope(sourceReference);
      OpalaCommercialContext.require(
          productId == null || Objects.equals(productId, scope.cycle().getProductId()),
          "O subprocesso pertence a outro produto.");
      var candidate = context.candidate(scope);
      versionContract.resolve(scope, candidate);
      OpalaCommercialContext.require(
          candidate.slots().size() <= 1,
          "Há mais de uma candidata para a versão e o experimento; resolva a ambiguidade.");
      URI destination = URI.create(Objects.requireNonNullElse(candidate.destinationUrl(), ""));
      OpalaCommercialContext.require(
          "https".equals(destination.getScheme())
              && destination.getHost() != null
              && destination.getUserInfo() == null
              && destination.getFragment() == null,
          "Defina o destino HTTPS aprovado da própria candidata.");
      return Preparation.ready(
          "Candidata comercial, checkout e acesso conferidos para esta versão.");
    } catch (IllegalStateException | IllegalArgumentException ex) {
      log.warn(
          "Candidata Opala indisponível antes da reserva. sourceReference={} productId={}",
          sourceReference,
          productId,
          ex);
      return Preparation.waiting(
          "MISSING_EVIDENCE",
          "Dédalo: preparar o contrato comercial da versão deste ciclo, com checkout e acesso "
              + "compatíveis, antes de iniciar os agentes. Motivo: "
              + ex.getMessage());
    } catch (RuntimeException ex) {
      log.error(
          "Falha técnica ao conferir candidata Opala. sourceReference={} productId={}",
          sourceReference,
          productId,
          ex);
      return Preparation.waiting(
          "TECHNICAL_FAILURE",
          "Marketing Hub: recuperar a consulta da candidata comercial antes de reservar trabalho. "
              + "As tarefas e os custos anteriores permanecem preservados.");
    }
  }
}
