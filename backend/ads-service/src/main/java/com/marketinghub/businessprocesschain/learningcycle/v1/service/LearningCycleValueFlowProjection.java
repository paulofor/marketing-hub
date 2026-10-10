package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleValueFlow.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitPrototypeCapabilities;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.kit.KitPrivateArtifactRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: resumir provas persistidas sem executar agentes ou recomputar números
 * comerciais.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleValueFlowProjection {
  private final KitPrototypeCapabilities capabilities;
  private final KitPrivateArtifactRepository artifacts;
  private final AgentTaskRepository tasks;
  private final LearningSalesCycleEventRepository events;
  private final ProcessRunRepository runs;
  private final LearningCyclePrototypeContext prototype;
  private final LearningCycleJson json;
  private final LearningCycleVideoBudget videoBudget;
  private final LearningCycleVideoEvidence videoEvidence;
  private static final Set<String> ACTIVE = Set.of("PENDING", "RUNNING", "IN_PROGRESS", "QUEUED");

  /**
   * Resolve atividade, pausa, teto e aceite das peças exatas sem autorizar consumo pela leitura.
   */
  public LearningCycleValueFlow resolve(
      LearningSalesCycle cycle, LearningCycleProcessContext.Work work) {
    var capability = capabilities.resolve(cycle);
    var compositions = artifacts.summaries(cycle.getId());
    boolean registered = prototype.resolve(cycle).isPresent();
    var pausedRun =
        !"OPEN".equals(cycle.getStatus()) || work == null
            ? Optional.<ProcessRun>empty()
            : runs.findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                    cycle.getProductId(),
                    work.processDefinitionId(),
                    cycle.getChainDefinitionId(),
                    cycle.getId(),
                    "experiment:" + cycle.getExperimentId())
                .filter(r -> "PAUSED".equals(r.getStatus()));
    boolean paused = pausedRun.isPresent();
    var snapshots =
        tasks.findFunctionalSnapshots(
            "experiment:" + cycle.getExperimentId(),
            Set.of(
                "pde-commercial-plan-offer",
                "pde-construction-approval",
                "pde-communication-sales-journey"),
            cycle.getCreatedAt());
    var latest =
        new LinkedHashMap<String, com.marketinghub.agenttask.AgentTaskFunctionalSnapshot>();
    snapshots.stream()
        .sorted(
            Comparator.comparing(com.marketinghub.agenttask.AgentTaskFunctionalSnapshot::id)
                .reversed())
        .forEach(t -> latest.putIfAbsent(t.processCode() + ":" + t.processActivityId(), t));
    boolean active =
        compositions.stream().anyMatch(c -> Set.of("QUEUED", "RUNNING").contains(c.getStatus()))
            || latest.values().stream().anyMatch(t -> ACTIVE.contains(t.status()))
            || work != null && "IN_PROGRESS".equals(work.state());
    int ready =
        capability.available() && "OPEN".equals(cycle.getStatus())
            ? (int)
                artifacts
                    .acceptedManifests(
                        cycle.getId(),
                        cycle.getProductId(),
                        cycle.getExperimentId(),
                        cycle.getProductVersion(),
                        capability.profileCode())
                    .stream()
                    .map(json::read)
                    .filter(
                        manifest ->
                            com.marketinghub.pde.kit.privateprototype.v1.service.KitArtifactContract
                                    .PACKAGE_CONTRACT_VERSION
                                    .equals(manifest.path("packageContractVersion").asText())
                                && manifest.path("files").size() == 36)
                    .count()
            : (int) compositions.stream().filter(c -> "READY".equals(c.getStatus())).count();
    boolean implementationPending =
        "OPEN".equals(cycle.getStatus()) && capability.available() && !registered;
    boolean videoBrief =
        !paused
            && work == null
            && "OPEN".equals(cycle.getStatus())
            && "VIDEO_BRIEF".equals(cycle.getStage());
    boolean videoBudgetMissing = videoBrief && videoBudget.current(cycle) == null;
    var videoReview =
        !paused && "OPEN".equals(cycle.getStatus()) && "VIDEO_APPROVAL".equals(cycle.getStage())
            ? videoReview(cycle)
            : null;
    String situation =
        paused
            ? "A continuidade automática deste ciclo está pausada. As entregas aceitas foram preservadas."
            : videoBrief
                ? videoBudgetMissing
                    ? "A preparação dos vídeos aguarda sua decisão sobre o teto de produção e revisão."
                    : "O teto dos vídeos está registrado; falta concluir o briefing e a avaliação financeira."
                : videoReview != null
                    ? videoReview.situation()
                    : implementationPending
                        ? (active
                            ? "A implementação privada está em composição."
                            : ready > 0
                                ? "O pacote está utilizável; a prova da versão aguarda registro para homologação."
                                : "Os contratos estão prontos; falta comprovar a implementação privada.")
                        : registered && work != null
                            ? "A versão privada está registrada e aguarda a entrega de "
                                + work.responsible()
                                + "."
                            : work == null
                                ? cycle.getStatus().equals("OPEN")
                                    ? "O ciclo aguarda sua próxima passagem autorizada."
                                    : "Este ciclo está encerrado; suas provas e custos foram preservados."
                                : work.reason();
    String blocker =
        paused
            ? "A próxima atividade aguarda a retomada da execução #"
                + pausedRun.orElseThrow().getId()
                + ". "
                + pausedRun.orElseThrow().getReason()
            : videoBrief
                ? videoBudgetMissing
                    ? "Falta um teto próprio desta versão para o anúncio e a demonstração. O orçamento de preparação por IA não autoriza vídeos ou mídia."
                    : "Os vídeos desta versão ainda precisam de produção, revisão e integração antes da homologação comercial."
                : videoReview != null
                    ? videoReview.blocker()
                    : implementationPending
                        ? "A versão precisa comprovar uso, arquivos íntegros, recuperação e isolamento antes da revisão independente."
                        : work != null
                            ? work.reason()
                            : "AUTHORIZATION".equals(cycle.getStage())
                                ? "A autorização comercial própria ainda precisa ser registrada."
                                : "As provas de venda e contribuição dependem do experimento comercial autorizado e da conciliação.";
    Instant since =
        active || !"OPEN".equals(cycle.getStatus())
            ? null
            : latest.values().stream()
                .map(t -> t.deliveredAt() == null ? t.createdAt() : t.deliveredAt())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(cycle.getCreatedAt());
    var ledger = events.findByCycleIdOrderByRevisionAsc(cycle.getId());
    if (since != null) {
      var delivery =
          compositions.stream()
              .map(c -> c.getFinishedAt())
              .filter(Objects::nonNull)
              .max(Comparator.naturalOrder())
              .orElse(since);
      var registration =
          ledger.stream()
              .filter(e -> "REGISTER_PROTOTYPE".equals(e.getAction()))
              .map(e -> e.getCreatedAt())
              .max(Comparator.naturalOrder())
              .orElse(since);
      if (delivery.isAfter(since)) since = delivery;
      if (registration.isAfter(since)) since = registration;
      if (videoReview != null) {
        var videoDelivery =
            ledger.stream()
                .filter(e -> "COMPLETE".equals(e.getAction()))
                .filter(e -> Set.of("CAMPAIGN_VIDEO", "PDE_ENTRY_VIDEO").contains(e.getFromStage()))
                .map(LearningSalesCycleEvent::getCreatedAt)
                .filter(Objects::nonNull)
                .filter(
                    time ->
                        cycle.getVersionChangedAt() == null
                            || !time.isBefore(cycle.getVersionChangedAt()))
                .max(Comparator.naturalOrder())
                .orElse(since);
        if (videoDelivery.isAfter(since)) since = videoDelivery;
      }
    }
    var target =
        ledger.stream()
            .filter(e -> "DEFINE_CONTRIBUTION_TARGET".equals(e.getAction()))
            .reduce((a, b) -> b)
            .map(
                e ->
                    json.read(e.getEvidenceJson())
                        .path("minimumContributionPercent")
                        .decimalValue())
            .orElse(null);
    var measure =
        ledger.stream()
            .filter(
                e ->
                    "MEASURE".equals(e.getAction())
                        || "RECONCILE_MEASUREMENT".equals(e.getAction())
                        || "RECONCILE".equals(e.getAction())
                        || "MEASUREMENT".equals(e.getToStage()))
            .filter(
                e ->
                    json.read(e.getEvidenceJson()).has("netSales")
                        && json.read(e.getEvidenceJson()).path("dataValid").asBoolean(false)
                        && json.read(e.getEvidenceJson()).path("testDataExcluded").asBoolean(false))
            .reduce((a, b) -> b)
            .orElse(null);
    var scenarios = new ArrayList<Scenario>();
    var outputs = new ArrayList<Deliverable>();
    for (var task : latest.values()) {
      var result = task.resultJson() == null ? null : json.read(task.resultJson());
      boolean completed = "COMPLETED".equals(task.status());
      String output =
          !completed
              ? "Ainda não produziu uma saída aceita."
              : Set.of("journey", "deliverables", "access", "productArchitecture")
                      .contains(task.processActivityId())
                  ? "Contrato estruturado. A especificação não comprova implementação."
                  : "technicalHomologation".equals(task.processActivityId())
                      ? "Parecer técnico independente com suas evidências."
                      : "Artefato estruturado disponível na atividade; não comprova venda.";
      outputs.add(
          new Deliverable(
              task.id(),
              task.processActivityId(),
              task.agentKey(),
              task.status(),
              output,
              task.deliveredAt()));
      if (completed && "economics".equals(task.processActivityId()) && result != null)
        for (var s : result.path("scenarios")) {
          if (!s.path("priceBrl").isNumber() || !s.path("variableCostBrl").isNumber()) continue;
          var price = s.path("priceBrl").decimalValue();
          var cost = s.path("variableCostBrl").decimalValue();
          var remainder = price.subtract(cost);
          scenarios.add(
              new Scenario(
                  s.path("name").asText(),
                  price,
                  cost,
                  remainder,
                  price.signum() > 0
                      ? remainder
                          .multiply(BigDecimal.valueOf(100))
                          .divide(price, 2, RoundingMode.HALF_UP)
                      : null,
                  s.path("risk").asText(),
                  task.id()));
        }
    }
    Boolean decision;
    if (paused) decision = Boolean.TRUE;
    else if (videoBrief) decision = videoBudgetMissing;
    else if (videoReview != null) decision = videoReview.decisionNeeded();
    else if (implementationPending) decision = Boolean.FALSE;
    else if ("AUTHORIZATION".equals(cycle.getStage())) decision = Boolean.TRUE;
    else if (work != null && Set.of("BLOCKED", "WAITING_INPUT").contains(work.state()))
      decision = null;
    else decision = Boolean.FALSE;
    return new LearningCycleValueFlow(
        situation,
        blocker,
        paused
            ? "Operador da execução"
            : videoBrief
                ? videoBudgetMissing
                    ? "Você · responsável pelo orçamento dos vídeos"
                    : "Marketing Hub · preparação dos vídeos"
                : videoReview != null
                    ? videoReview.responsible()
                    : implementationPending
                        ? "Dédalo · compositor de kits"
                        : work == null ? "Marketing Hub" : work.responsible(),
        videoBrief
            ? "Apolo · produção após briefing e avaliação de Plutus"
            : videoReview != null
                ? "Backend · integração; Psique e Têmis · homologação após o aceite"
                : (paused || implementationPending) && work != null ? work.responsible() : null,
        active,
        decision,
        paused
            ? "Confira o motivo da pausa e os limites registrados antes de retomar pela atividade. A disponibilidade técnica não libera novos gastos."
            : videoBrief
                ? videoBudgetMissing
                    ? "Informe em Financeiro dos vídeos um único teto total em US$ para produzir e revisar as duas peças. Nenhum valor foi escolhido automaticamente."
                    : "O teto vigente será reutilizado na avaliação. Ele não inicia produção nem autoriza campanha, cobrança ou mídia."
                : videoReview != null
                    ? videoReview.decisionReason()
                    : implementationPending
                        ? "Nenhuma nova data ou meta de margem é necessária para esta implementação privada."
                        : Boolean.TRUE.equals(decision)
                            ? "Confira as opções e os limites no formulário de autorização deste ciclo."
                            : decision == null
                                ? "A atividade contém a causa e a ação necessária; consulte sua pendência."
                                : "Nenhuma decisão nova foi identificada nesta passagem.",
        paused
            ? "Retomada autorizada da execução #"
                + pausedRun.orElseThrow().getId()
                + " e aceite da saída da atividade "
                + work.activityName()
                + "."
            : videoBrief
                ? videoBudgetMissing
                    ? "Teto registrado no mesmo ciclo; depois, briefing de Íris e avaliação de Plutus antes da produção de Apolo."
                    : "Briefing concluído com a referência financeira vigente, seguido da avaliação de Plutus e produção governada."
                : videoReview != null
                    ? videoReview.acceptance()
                    : implementationPending
                        ? "Pacote íntegro, primeira aplicação, retomada e limites comprovados; depois Psique revisa a mesma versão."
                        : work == null
                            ? "Aceite persistido da próxima passagem e métricas conciliadas da mesma versão."
                            : "Saída utilizável aceita na atividade "
                                + work.activityName()
                                + " e retorno ao processo pai.",
        since,
        since == null ? null : Math.max(0, Duration.between(since, Instant.now()).getSeconds()),
        Instant.now(),
        capability,
        registered,
        ready,
        outputs,
        scenarios,
        target,
        measure == null ? null : json.read(measure.getEvidenceJson()),
        measure == null ? null : "Evento #" + measure.getId() + " · " + measure.getCreatedAt());
  }

  /** Mantém juntos os fatos da revisão sem transformá-los em execução ou aprovação comercial. */
  private record VideoReviewGuidance(
      String situation,
      String blocker,
      String responsible,
      Boolean decisionNeeded,
      String decisionReason,
      String acceptance) {}

  /** Reutiliza o gate das peças selecionadas e mantém divergência de evidência como pendência. */
  private VideoReviewGuidance videoReview(LearningSalesCycle cycle) {
    try {
      if (videoEvidence.awaitingApproval(cycle))
        return new VideoReviewGuidance(
            "Os dois vídeos estão produzidos e aguardam seu aceite de uso.",
            "Falta concluir a seleção humana das peças antes da integração e da homologação. O teto financeiro não substitui esse aceite.",
            "Você · seleção de uso dos vídeos",
            Boolean.TRUE,
            "Abra Ver aprovações dos vídeos, assista às duas peças com som e escolha aprovar o uso ou pedir ajustes. Não é necessário autorizar novamente os limites já registrados.",
            "Aceites dos dois vídeos selecionados neste ciclo; depois, integração e homologação da mesma versão, antes da campanha.");
      return new VideoReviewGuidance(
          "O aceite dos dois vídeos está registrado; a integração e a homologação ainda precisam ser comprovadas.",
          "O conjunto com os vídeos precisa de integração, homologação e liberação comercial antes da campanha.",
          "Backend · integração e encaminhamento da homologação",
          Boolean.FALSE,
          "Os aceites das peças serão reutilizados. Nenhuma nova seleção de uso foi identificada; o processo precisa comprovar sua continuação.",
          "Recibo de integração e pareceres da mesma versão. Aceite de vídeo não conclui homologação nem ativa mídia.");
    } catch (RuntimeException ex) {
      log.warn(
          "Conferência das peças do ciclo indisponível productId={} cycleId={} experimentId={} version={}",
          cycle.getProductId(),
          cycle.getId(),
          cycle.getExperimentId(),
          cycle.getProductVersion(),
          ex);
      return new VideoReviewGuidance(
          "As evidências dos vídeos precisam de conferência antes da integração.",
          "Não foi possível confirmar as peças selecionadas desta versão. Corrija sua evidência na atividade antes de pedir aceite ou integrar.",
          "Marketing Hub · conferência dos vínculos audiovisuais",
          null,
          "Confira a pendência da atividade. A leitura não confirmou que falta uma decisão humana nem que as peças estão aprovadas.",
          "Seleções e evidências válidas dos dois vídeos no mesmo produto, ciclo, experimento e versão.");
    }
  }
}
