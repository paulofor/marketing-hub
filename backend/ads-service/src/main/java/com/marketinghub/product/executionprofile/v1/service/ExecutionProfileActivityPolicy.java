package com.marketinghub.product.executionprofile.v1.service;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.product.executionprofile.v1.ExecutionProfile;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: aplicar especialização e gates da ficha à leitura e ao comando BPM canônicos.
 */
@Component
@RequiredArgsConstructor
public class ExecutionProfileActivityPolicy {
  private final ExecutionProfileContext context;
  private final com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository
      processes;

  /** Permite a versão publicada original de uma ficha mesmo após publicação de sua sucessora. */
  public boolean pins(String reference, BusinessProcessDefinition process) {
    return "RETIRED".equals(process.getStatus()) && context.pins(reference, process.getId());
  }

  /**
   * Confere o contexto explícito de leitura ou comando sem substituir o ciclo pela versão atual.
   */
  public void requireReference(
      Long productId,
      String reference,
      BusinessProcessDefinition process,
      Long chainId,
      Long cycleId) {
    context
        .bound(productId, reference)
        .ifPresent(
            profile -> {
              context.requireScope(
                  productId, reference, chainId == null ? profile.getChainId() : chainId, cycleId);
              if (context.composition(profile).stream()
                  .noneMatch(p -> p.id().equals(process.getId())))
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A definição não pertence à ficha desta execução.");
            });
  }

  /** Revalida o mesmo contrato mostrado na tela antes de permitir a execução de uma atividade. */
  @Transactional(readOnly = true)
  public void require(
      Long productId, String reference, BusinessProcessDefinition process, String activityId) {
    context
        .bound(productId, reference)
        .ifPresent(
            profile -> {
              String reason = blocker(profile, process, activityId);
              if (reason != null) throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
              var work =
                  ExecutionProfileRules.work(context.contract(profile)).stream()
                      .filter(
                          w ->
                              w.processCode().equals(process.getProcessCode())
                                  && w.activityId().equals(activityId))
                      .findFirst();
              if (work.isPresent() && !work.get().applicable())
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, work.get().applicabilityReason());
            });
  }

  /**
   * Especializa nomes, critérios, dispensa explícita e bloqueios mantendo gates e provas originais.
   */
  @Transactional(readOnly = true)
  public List<ProductProcessActivityExecutionGroupResponse> decorate(
      Long productId,
      String reference,
      BusinessProcessDefinition process,
      List<ProductProcessActivityExecutionGroupResponse> groups) {
    var profile = context.bound(productId, reference);
    if (profile.isEmpty()) return groups;
    return groups.stream().map(group -> decorate(profile.get(), process, group)).toList();
  }

  /**
   * Mantém a identidade original e incorpora o contrato especializado apenas no contexto vinculado.
   */
  private ProductProcessActivityExecutionGroupResponse decorate(
      ExecutionProfile profile,
      BusinessProcessDefinition process,
      ProductProcessActivityExecutionGroupResponse group) {
    if (!group.selectedVersionActivity() || "NOT_APPLICABLE".equals(group.operationalState()))
      return group;
    var work =
        ExecutionProfileRules.work(context.contract(profile)).stream()
            .filter(
                w ->
                    w.processCode().equals(process.getProcessCode())
                        && w.activityId().equals(group.activityId()))
            .findFirst();
    boolean unstarted =
        !group.objectiveAchieved() && group.taskCount() == 0 && group.activityInstanceId() == null;
    boolean omitted =
        unstarted
            && work.isPresent()
            && !work.get().applicable()
            && group.executionControl() != null
            && !group.executionControl().confirmationRequired()
            && !"HUMAN".equals(group.executionControl().executorType());
    String reason = blocker(profile, process, group.activityId());
    String status =
        omitted
            ? "NOT_APPLICABLE"
            : reason != null && unstarted ? "BLOCKED" : group.operationalState();
    String explanation =
        omitted ? work.get().applicabilityReason() : reason == null ? group.stateReason() : reason;
    var control = group.executionControl();
    if (control != null) {
      Long child = control.targetProcessDefinitionId();
      String navigation = control.navigationUrl();
      if (child != null) {
        var target = processes.findById(child);
        var pinned =
            target.flatMap(
                t ->
                    context.composition(profile).stream()
                        .filter(p -> p.code().equals(t.getProcessCode()))
                        .findFirst());
        if (pinned.isEmpty()) {
          reason = "O subprocesso não pertence à composição congelada da ficha.";
          explanation = reason;
          if (unstarted) status = "BLOCKED";
        } else {
          if (navigation != null)
            navigation =
                navigation.replace(
                    "/processes/" + child + "/", "/processes/" + pinned.get().id() + "/");
          child = pinned.get().id();
        }
      }
      control =
          new ProductProcessActivityExecutionControlResponse(
              control.executorType(),
              control.interactionType(),
              control.actionLabel(),
              control.description(),
              !omitted && reason == null && control.actionAvailable(),
              omitted || reason != null ? explanation : control.availabilityReason(),
              control.confirmationRequired(),
              control.confirmationTitle(),
              control.confirmationMessage(),
              control.confirmationToken(),
              control.workspaceCode(),
              control.workspaceReferenceId(),
              child,
              control.requirements(),
              control.decisionMode(),
              control.auditEvidenceReference(),
              navigation);
    }
    return new ProductProcessActivityExecutionGroupResponse(
        group.activityDefinitionId(),
        group.activityId(),
        work.map(w -> w.name()).orElse(group.activityName()),
        work.map(w -> String.join("; ", w.requirements())).orElse(group.activityObjective()),
        group.activityOwnerName(),
        group.sequenceNumber(),
        true,
        status,
        explanation,
        group.objectiveAchieved(),
        omitted
            ? "EXECUTION_PROFILE_V1:" + profile.getId() + ":" + profile.getRevisionNumber()
            : group.stateEvidence(),
        group.activityInstanceId(),
        group.occurrenceNumber(),
        group.taskCount(),
        group.tasks(),
        !omitted && reason == null && group.executionRequestAvailable(),
        omitted || reason != null ? explanation : group.executionRequestReason(),
        control,
        group.recoveryAction());
  }

  /** Confere definição congelada e checkpoint financeiro sem apagar decisões já registradas. */
  private String blocker(
      ExecutionProfile profile, BusinessProcessDefinition process, String activityId) {
    if (context.composition(profile).stream().noneMatch(p -> p.id().equals(process.getId())))
      return "A versão do processo não corresponde à composição congelada da ficha.";
    String checkpoint = context.checkpoint(process.getProcessCode(), activityId);
    return checkpoint == null ? null : context.financialBlocker(profile, checkpoint);
  }
}
