package com.marketinghub.planning.service;

import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto.Entry;
import com.marketinghub.planning.dto.CommercialPlanOperationalFlowDto;
import com.marketinghub.planning.dto.CommercialPlanOperationalFlowDto.SpecialistDecision;
import com.marketinghub.planning.dto.CommercialPlanOperationalFlowDto.Stage;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: traduzir o estado persistido do plano em um fluxo operacional enxuto. */
@Service
public class CommercialPlanOperationalFlowService {
  private static final List<String> TERMINAL_SUCCESS = List.of("COMPLETED", "APPROVED", "DONE");
  private static final List<String> ACTIVE =
      List.of("PENDING", "RUNNING", "PROCESSING", "IN_PROGRESS");
  private final CommercialPlanAgentActivityService activityService;

  /** Configura a fonte auditável usada para resumir o trabalho dos especialistas. */
  public CommercialPlanOperationalFlowService(CommercialPlanAgentActivityService activityService) {
    this.activityService = activityService;
  }

  /** Monta o fluxo e mantém publicação ou gasto bloqueados sem homologação comprovada. */
  @Transactional(readOnly = true)
  public CommercialPlanOperationalFlowDto view(CommercialPlan plan) {
    CommercialPlanAgentActivityDto activity = activityService.activity(plan);
    boolean offerReady = hasText(plan.getMainOffer()) && hasText(plan.getTargetAudience());
    boolean experimentReady = plan.getExperiment() != null || !plan.getExperiments().isEmpty();
    Entry homologation = latestByType(activity.entries(), "JOURNEY_HOMOLOGATION");
    boolean homologated = homologation != null && isSuccess(homologation.status());
    boolean homologating = homologation != null && isActive(homologation.status());
    boolean published = positive(plan.getActualExperimentsPublished());
    boolean measured = positive(plan.getActualRevenue()) || positive(plan.getActualTotalCost());

    String currentStage;
    String status;
    String nextAction;
    String blocker = null;
    if (!offerReady) {
      currentStage = "CHOOSE_OFFER";
      status = "AJUSTE_NECESSARIO";
      nextAction = "Definir oferta e público antes de mobilizar especialistas.";
      blocker = "Oferta ou público ainda não está definido.";
    } else if (!experimentReady) {
      currentStage = "PREPARE_EXPERIMENT";
      status = "AJUSTE_NECESSARIO";
      nextAction = "Preparar um experimento mensurável para a oferta escolhida.";
      blocker = "Plano ainda não possui experimento vinculado.";
    } else if (!homologated) {
      currentStage = "HOMOLOGATE_JOURNEY";
      status = homologating ? "EM_ANDAMENTO" : "BLOQUEADO";
      nextAction =
          homologating
              ? "Aguardar Íris materializar a landing e concluir a homologação técnica."
              : "Solicitar a homologação de tracking, pagamento e entrega.";
      blocker = homologating ? null : "Jornada essencial ainda não homologada.";
    } else if (!published) {
      currentStage = "PUBLISH_TEST";
      status = "APROVADO";
      nextAction = "Publicar o teste somente após a aprovação humana de gasto e publicação.";
    } else if (!measured) {
      currentStage = "MEASURE_SALES";
      status = "EM_ANDAMENTO";
      nextAction = "Medir eventos e vendas reais do experimento publicado.";
    } else {
      currentStage = "ADJUST_OR_SCALE";
      status = "EM_ANDAMENTO";
      nextAction =
          hasText(plan.getNextAction())
              ? plan.getNextAction()
              : "Ajustar ou escalar conforme vendas e custo reais.";
    }

    return new CommercialPlanOperationalFlowDto(
        plan.getId(),
        currentStage,
        status,
        nextAction,
        blocker,
        "Tempo entre criação do plano e experimento homologado/publicável.",
        "Continuar com jornada homologada; ajustar bloqueio técnico; parar investimento sem tracking, pagamento ou entrega comprovados.",
        List.of(
            stage("CHOOSE_OFFER", "Escolher oferta", offerReady, currentStage),
            stage("PREPARE_EXPERIMENT", "Preparar experimento", experimentReady, currentStage),
            stage("HOMOLOGATE_JOURNEY", "Homologar jornada", homologated, currentStage),
            stage("PUBLISH_TEST", "Publicar teste", published, currentStage),
            stage("MEASURE_SALES", "Medir vendas", measured, currentStage),
            stage("ADJUST_OR_SCALE", "Ajustar ou escalar", false, currentStage)),
        List.of(
            decision(activity.entries(), "Atena", "Oferta, público e criativos"),
            decision(activity.entries(), "Plutus", "Orçamento, margem e limite de perda"),
            decision(activity.entries(), "Dédalo", "PDE, jornada pós-compra e provas reais"),
            decision(activity.entries(), "Íris", "Comunicação, landing e homologação técnica"),
            decision(activity.entries(), "Hermes", "Resultados e aprendizados")));
  }

  /** Define o estado visual de uma etapa sem permitir inferência no frontend. */
  private Stage stage(String code, String label, boolean completed, String currentStage) {
    return new Stage(
        code, label, completed ? "CONCLUIDO" : code.equals(currentStage) ? "ATUAL" : "PENDENTE");
  }

  /** Resume a atuação mais recente no vocabulário operacional único. */
  private SpecialistDecision decision(List<Entry> entries, String nickname, String responsibility) {
    Entry entry = latest(entries, nickname);
    if (entry == null) {
      return new SpecialistDecision(
          nickname,
          responsibility,
          "AJUSTE_NECESSARIO",
          "Trabalho será acionado quando a etapa exigir esta especialidade.");
    }
    String decision =
        isSuccess(entry.status())
            ? "APROVADO"
            : isActive(entry.status()) ? "EM_ANDAMENTO" : "BLOQUEADO";
    String action =
        hasText(entry.difficulty())
            ? entry.difficulty()
            : hasText(entry.detail()) ? entry.detail() : "Nenhuma ação adicional informada.";
    return new SpecialistDecision(nickname, responsibility, decision, action);
  }

  /** Localiza o registro mais recente, pois a atividade já chega ordenada pelo backend. */
  private Entry latest(List<Entry> entries, String nickname) {
    return entries.stream()
        .filter(entry -> nickname.equals(entry.agentNickname()))
        .findFirst()
        .orElse(null);
  }

  /** Localiza a evidência mais recente de um tipo funcional específico. */
  private Entry latestByType(List<Entry> entries, String recordType) {
    return entries.stream()
        .filter(entry -> recordType.equals(entry.recordType()))
        .findFirst()
        .orElse(null);
  }

  /** Reconhece conclusão funcional persistida. */
  private boolean isSuccess(String status) {
    return status != null && TERMINAL_SUCCESS.contains(status.toUpperCase());
  }

  /** Reconhece trabalho ativo persistido. */
  private boolean isActive(String status) {
    return status != null && ACTIVE.contains(status.toUpperCase());
  }

  /** Confirma valor textual útil. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Confirma métrica realizada positiva. */
  private boolean positive(Number value) {
    return value != null && new BigDecimal(value.toString()).compareTo(BigDecimal.ZERO) > 0;
  }
}
