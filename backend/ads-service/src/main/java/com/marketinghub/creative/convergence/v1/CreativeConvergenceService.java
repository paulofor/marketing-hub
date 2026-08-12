package com.marketinghub.creative.convergence.v1;

import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskByAgentRequest;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.dto.CreativeAgentReviewResultRequest;
import com.marketinghub.geralanding.agent.v1.LandingGenerationAgentExecutionService;
import com.marketinghub.repository.jpa.creative.convergence.CreativeConvergenceCycleRepository;
import com.marketinghub.repository.jpa.creative.convergence.CreativeConvergenceTaskRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: coordenar a convergência entre Aprovador, mídia e GeraLanding. */
@Service
@RequiredArgsConstructor
public class CreativeConvergenceService {
  private static final int MAX_ITERATIONS = 8;
  private static final int MAX_REPEATED_ISSUES = 2;
  private static final BigDecimal MAX_CYCLE_COST_USD = new BigDecimal("5.00");

  private final CreativeConvergenceCycleRepository cycleRepository;
  private final CreativeConvergenceTaskRepository taskRepository;
  private final AgentTaskService agentTaskService;
  private final LandingGenerationAgentExecutionService landingAgentExecutionService;

  /** Registra o parecer, distribui correções e encerra ciclos sem progresso ou acima do custo. */
  @Transactional
  public void registerReview(Creative creative, CreativeAgentReviewResultRequest review) {
    Long rootCreativeId = rootCreativeId(creative);
    CreativeConvergenceCycle cycle =
        cycleRepository
            .findFirstByRootCreativeIdAndStatusOrderByIdDesc(
                rootCreativeId, ConvergenceCycleStatus.ACTIVE)
            .orElseGet(() -> newCycle(creative, rootCreativeId));
    Instant now = Instant.now();
    int score = averageScore(review);
    cycle.setIterationCount(Objects.requireNonNullElse(cycle.getIterationCount(), 0) + 1);
    cycle.setLastScore(score);
    cycle.setBestScore(Math.max(Objects.requireNonNullElse(cycle.getBestScore(), 0), score));
    cycle.setCostUsd(
        Objects.requireNonNullElse(cycle.getCostUsd(), BigDecimal.ZERO)
            .add(Objects.requireNonNullElse(review.costUsd(), BigDecimal.ZERO)));
    cycle.setUpdatedAt(now);

    if (review.decision() == CreativeAgentReviewStatus.APPROVED) {
      taskRepository.findByCycleIdOrderByIdAsc(cycle.getId()).stream()
          .filter(task -> task.getStatus() == ConvergenceTaskStatus.PENDING)
          .forEach(
              task -> {
                task.setStatus(ConvergenceTaskStatus.VERIFIED);
                task.setCompletedAt(now);
                task.setEvidenceJson("{\"verifiedByCreativeId\":" + creative.getId() + "}");
                taskRepository.save(task);
              });
      cycle.setStatus(ConvergenceCycleStatus.APPROVED);
      cycle.setStopReason(null);
      cycleRepository.save(cycle);
      return;
    }

    List<CreativeAgentReviewResultRequest.ConvergenceCorrectionTarget> targets =
        Objects.requireNonNullElse(review.correctionTargets(), List.of());
    if (targets.isEmpty()) {
      cycle.setStatus(ConvergenceCycleStatus.FAILED);
      cycle.setStopReason("Parecer sem correções verificáveis e responsáveis definidos");
      cycleRepository.save(cycle);
      return;
    }

    Set<String> currentFingerprints =
        targets.stream().map(this::fingerprint).collect(java.util.stream.Collectors.toSet());
    taskRepository.findByCycleIdOrderByIdAsc(cycle.getId()).stream()
        .filter(task -> task.getStatus() == ConvergenceTaskStatus.PENDING)
        .filter(task -> !currentFingerprints.contains(task.getFingerprint()))
        .forEach(
            task -> {
              task.setStatus(ConvergenceTaskStatus.VERIFIED);
              task.setCompletedAt(now);
              task.setEvidenceJson("{\"verifiedByCreativeId\":" + creative.getId() + "}");
              taskRepository.save(task);
            });

    boolean landingDispatched = false;
    for (var target : targets) {
      validateTarget(target);
      ConvergenceTaskTarget executor = ConvergenceTaskTarget.valueOf(target.target());
      String fingerprint = fingerprint(target);
      boolean repeated = taskRepository.existsByCycleIdAndFingerprint(cycle.getId(), fingerprint);
      taskRepository.save(
          CreativeConvergenceTask.builder()
              .cycleId(cycle.getId())
              .creativeId(creative.getId())
              .target(executor)
              .status(repeated ? ConvergenceTaskStatus.REPEATED : ConvergenceTaskStatus.PENDING)
              .issueCode(target.issueCode().trim())
              .requirement(target.requirement().trim())
              .acceptanceCriterion(target.acceptanceCriterion().trim())
              .fingerprint(fingerprint)
              .createdAt(now)
              .build());
      if (repeated) {
        cycle.setRepeatedIssueCount(
            Objects.requireNonNullElse(cycle.getRepeatedIssueCount(), 0) + 1);
      } else if (executor == ConvergenceTaskTarget.LANDING && !landingDispatched) {
        dispatchLandingCorrection(creative, cycle, target);
        landingDispatched = true;
      }
    }
    applyStopGates(cycle);
    cycleRepository.save(cycle);
  }

  /** Delega a causa da landing para Dédalo e abre sua execução autônoma sem permitir publicação. */
  private void dispatchLandingCorrection(
      Creative creative,
      CreativeConvergenceCycle cycle,
      CreativeAgentReviewResultRequest.ConvergenceCorrectionTarget target) {
    String sourceReference = "creative-convergence:" + cycle.getId() + ":landing";
    String brief = correctionBrief(target);
    agentTaskService.createOperationalDelegationIfAbsent(
        new CreateAgentTaskByAgentRequest(
            "meta-ad-approver",
            "landing-generator",
            "Corrigir landing do experimento #" + creative.getExperiment().getId(),
            brief,
            "HIGH",
            sourceReference));
    landingAgentExecutionService.enqueueCreativeConvergenceCorrection(
        creative.getExperiment().getId(),
        sourceReference,
        target.issueCode(),
        brief,
        target.acceptanceCriterion());
  }

  /** Retorna o relatório persistido mais recente da linhagem sem depender de logs. */
  @Transactional(readOnly = true)
  public CreativeConvergenceReport report(Creative creative) {
    Long rootCreativeId = rootCreativeId(creative);
    CreativeConvergenceCycle cycle =
        cycleRepository.findFirstByRootCreativeIdOrderByIdDesc(rootCreativeId).orElseThrow();
    List<CreativeConvergenceReport.Task> tasks =
        taskRepository.findByCycleIdOrderByIdAsc(cycle.getId()).stream()
            .map(
                task ->
                    new CreativeConvergenceReport.Task(
                        task.getId(),
                        task.getCreativeId(),
                        task.getTarget(),
                        task.getStatus(),
                        task.getIssueCode(),
                        task.getRequirement(),
                        task.getAcceptanceCriterion(),
                        task.getEvidenceJson()))
            .toList();
    return new CreativeConvergenceReport(
        cycle.getId(),
        cycle.getExperimentId(),
        cycle.getRootCreativeId(),
        cycle.getStatus(),
        cycle.getIterationCount(),
        cycle.getRepeatedIssueCount(),
        cycle.getLastScore(),
        cycle.getBestScore(),
        cycle.getCostUsd(),
        cycle.getStopReason(),
        tasks);
  }

  /** Cria um ciclo segregado pela linhagem do criativo e pelo experimento. */
  private CreativeConvergenceCycle newCycle(Creative creative, Long rootCreativeId) {
    Instant now = Instant.now();
    return cycleRepository.save(
        CreativeConvergenceCycle.builder()
            .experimentId(creative.getExperiment().getId())
            .rootCreativeId(rootCreativeId)
            .status(ConvergenceCycleStatus.ACTIVE)
            .iterationCount(0)
            .repeatedIssueCount(0)
            .bestScore(0)
            .costUsd(BigDecimal.ZERO)
            .createdAt(now)
            .updatedAt(now)
            .build());
  }

  /** Aplica limites por progresso, custo e iterações sem liberar publicação. */
  private void applyStopGates(CreativeConvergenceCycle cycle) {
    if (cycle.getCostUsd().compareTo(MAX_CYCLE_COST_USD) > 0) {
      cycle.setStatus(ConvergenceCycleStatus.BLOCKED_COST);
      cycle.setStopReason("Custo máximo do ciclo de convergência atingido");
    } else if (cycle.getRepeatedIssueCount() >= MAX_REPEATED_ISSUES) {
      cycle.setStatus(ConvergenceCycleStatus.BLOCKED_NO_PROGRESS);
      cycle.setStopReason("A mesma falha reapareceu sem evidência de progresso");
    } else if (cycle.getIterationCount() >= MAX_ITERATIONS) {
      cycle.setStatus(ConvergenceCycleStatus.BLOCKED_NO_PROGRESS);
      cycle.setStopReason("Limite seguro de iterações atingido");
    }
  }

  /** Valida que cada correção tenha domínio, código, requisito e aceite objetivos. */
  private void validateTarget(CreativeAgentReviewResultRequest.ConvergenceCorrectionTarget target) {
    if (target == null
        || !StringUtils.hasText(target.target())
        || !StringUtils.hasText(target.issueCode())
        || !StringUtils.hasText(target.requirement())
        || !StringUtils.hasText(target.acceptanceCriterion())) {
      throw new IllegalArgumentException("Correção de convergência incompleta");
    }
    try {
      ConvergenceTaskTarget.valueOf(target.target());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "Executor de convergência inválido: " + target.target(), ex);
    }
  }

  /** Calcula uma medida simples e comparável da qualidade comercial da versão. */
  private int averageScore(CreativeAgentReviewResultRequest review) {
    return (Objects.requireNonNullElse(review.attentionScore(), 0)
            + Objects.requireNonNullElse(review.clarityScore(), 0)
            + Objects.requireNonNullElse(review.desireScore(), 0)
            + Objects.requireNonNullElse(review.credibilityScore(), 0)
            + Objects.requireNonNullElse(review.actionScore(), 0))
        / 5;
  }

  /** Resolve o primeiro criativo da linhagem para manter um único ciclo entre versões. */
  private Long rootCreativeId(Creative creative) {
    Creative current = creative;
    while (current.getSourceCreative() != null) {
      current = current.getSourceCreative();
    }
    return current.getId();
  }

  /** Produz impressão estável para detectar a reaparição da mesma pendência. */
  private String fingerprint(CreativeAgentReviewResultRequest.ConvergenceCorrectionTarget target) {
    String normalized =
        String.join(
                "|",
                target.target(),
                target.issueCode(),
                target.requirement(),
                target.acceptanceCriterion())
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ");
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(normalized.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }

  /** Monta o briefing rastreável entregue à etapa oficial de copy da landing. */
  private String correctionBrief(
      CreativeAgentReviewResultRequest.ConvergenceCorrectionTarget target) {
    return "Correção solicitada pelo Aprovador Meta. Código: "
        + target.issueCode().trim()
        + ". Requisito: "
        + target.requirement().trim()
        + ". Critério de aceite: "
        + target.acceptanceCriterion().trim();
  }
}
