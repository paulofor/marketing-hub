package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand.Action;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: definir as transições comerciais e os limites verificáveis do ciclo v1. */
public final class LearningCycleRules {
  public static final String PROCESS_CODE = "value-chain-learning-sales-cycle";
  public static final List<String> STAGES =
      List.of(
          "LEARNING",
          "PLANNING",
          "ADJUSTMENT",
          "VALIDATION",
          "AUTHORIZATION",
          "PUBLICATION",
          "MEASUREMENT",
          "DECISION",
          "SCALE_AUTHORIZATION");
  public static final List<String> VIDEO_STAGES =
      List.of("VIDEO_BRIEF", "CAMPAIGN_VIDEO", "PDE_ENTRY_VIDEO", "VIDEO_APPROVAL");
  private static final Map<String, String> VIDEO_LABELS =
      Map.of(
          "VIDEO_BRIEF", "Definir os dois vídeos",
          "CAMPAIGN_VIDEO", "Produzir vídeo do criativo de campanha",
          "PDE_ENTRY_VIDEO", "Produzir vídeo de entrada do PDE",
          "VIDEO_APPROVAL", "Revisar e integrar os dois vídeos");
  private static final Map<String, String> LABELS =
      Map.of(
          "LEARNING",
          "Aprendizado e hipótese",
          "PLANNING",
          "Planejar o experimento",
          "ADJUSTMENT",
          "Ajustar produto e comunicação",
          "VALIDATION",
          "Homologar a mesma versão",
          "AUTHORIZATION",
          "Autorizar orçamento e janela",
          "PUBLICATION",
          "Conferir publicação",
          "MEASUREMENT",
          "Medir vendas e valor entregue",
          "DECISION",
          "Decisão comercial",
          "SCALE_AUTHORIZATION",
          "Autorizar expansão");

  /** Impede instâncias de uma política determinística sem estado. */
  private LearningCycleRules() {}

  /** Retorna o nome humano da etapa persistida. */
  public static String label(String stage) {
    return LABELS.getOrDefault(stage, VIDEO_LABELS.getOrDefault(stage, stage));
  }

  /** Expõe somente movimentos compatíveis com a etapa, antes das validações de evidência. */
  public static List<Action> actions(String stage) {
    return switch (stage) {
      case "LEARNING", "PLANNING" -> List.of(Action.COMPLETE, Action.STOP);
      case "ADJUSTMENT",
          "VALIDATION",
          "AUTHORIZATION",
          "PUBLICATION",
          "VIDEO_BRIEF",
          "CAMPAIGN_VIDEO",
          "PDE_ENTRY_VIDEO",
          "VIDEO_APPROVAL" ->
          List.of(Action.COMPLETE, Action.REWORK, Action.STOP);
      case "MEASUREMENT" ->
          List.of(
              Action.MEASURE,
              Action.FIX_MEASUREMENT,
              Action.REWORK,
              Action.STOP,
              Action.INCONCLUSIVE);
      case "DECISION" ->
          List.of(
              Action.ADJUST,
              Action.REWORK,
              Action.CONTINUE,
              Action.FIX_MEASUREMENT,
              Action.SCALE,
              Action.STOP,
              Action.INCONCLUSIVE);
      case "SCALE_AUTHORIZATION" -> List.of(Action.AUTHORIZE_SCALE, Action.STOP);
      default -> List.of();
    };
  }

  /** Nomeia os comandos administrativos sem prometer execução externa. */
  public static String actionLabel(Action action, String stage) {
    return switch (action) {
      case COMPLETE ->
          "AUTHORIZATION".equals(stage) ? "Registrar autorização" : "Concluir etapa com evidência";
      case REWORK -> "Devolver para correção";
      case MEASURE -> "Registrar leitura de resultados";
      case ADJUST -> "Encerrar ciclo e preparar sucessor";
      case CONTINUE -> "Continuar coleta autorizada";
      case FIX_MEASUREMENT -> "Corrigir medição neste ciclo";
      case SCALE -> "Solicitar escala";
      case AUTHORIZE_SCALE -> "Registrar nova autorização";
      case STOP -> "Encerrar ciclo";
      case INCONCLUSIVE -> "Encerrar como inconclusivo";
    };
  }

  /** Calcula a progressão comum somente depois que o service comprovar a etapa. */
  public static String next(String stage) {
    int index = STAGES.indexOf(stage);
    require(index >= 0 && index < 6, "Esta etapa exige uma decisão específica.");
    return STAGES.get(index + 1);
  }

  /** Acrescenta entregas audiovisuais somente à versão do BPM que as declara. */
  public static String next(String stage, boolean videoWorkflow) {
    if (videoWorkflow) {
      if ("ADJUSTMENT".equals(stage)) return "VIDEO_BRIEF";
      int index = VIDEO_STAGES.indexOf(stage);
      if (index >= 0)
        return index == VIDEO_STAGES.size() - 1 ? "VALIDATION" : VIDEO_STAGES.get(index + 1);
    }
    return next(stage);
  }

  /** Bloqueia coleta fora da autorização e resultados inválidos ou desatualizados. */
  public static String collectionBlocker(LearningSalesCycle cycle, JsonNode metrics, Instant now) {
    if (!metrics.path("dataValid").asBoolean(false))
      return "Corrija e reconcilie a medição antes de continuar.";
    if (!metrics.path("testDataExcluded").asBoolean(false))
      return "Separe os dados de teste da leitura comercial.";
    if (now.isBefore(cycle.getWindowStart()) || !now.isBefore(cycle.getWindowEnd()))
      return "Janela autorizada encerrada ou ainda não iniciada.";
    if (metrics.path("spendBrl").decimalValue().compareTo(cycle.getBudgetLimitBrl()) > 0
        || (metrics.path("spendBrl").decimalValue().compareTo(cycle.getBudgetLimitBrl()) == 0
            && cycle.getBudgetLimitBrl().signum() > 0))
      return "Teto autorizado atingido. Encerre ou registre resultado inconclusivo.";
    if (parseInstant(metrics, "observedAt").isBefore(now.minusSeconds(86400)))
      return "Atualize a leitura dos resultados antes de decidir continuidade ou escala.";
    return null;
  }

  /** Exige contribuição positiva e provas de uso antes de solicitar ampliação. */
  public static String scaleBlocker(
      LearningSalesCycle cycle, JsonNode metrics, JsonNode brief, Instant now) {
    if (!metrics.path("dataValid").asBoolean(false)
        || !metrics.path("testDataExcluded").asBoolean(false))
      return "Corrija os dados e separe o tráfego de teste antes de escalar.";
    if (parseInstant(metrics, "observedAt").isBefore(now.minusSeconds(86400)))
      return "Atualize a leitura dos resultados antes de solicitar escala.";
    if (metrics.path("sessions").asLong() < brief.path("sampleTarget").asLong())
      return "Amostra mínima declarada ainda não atingida.";
    if (metrics.path("netSales").asLong() < brief.path("minimumNetSales").asLong())
      return "Vendas líquidas mínimas ainda não comprovadas.";
    if (metrics.path("contributionBrl").decimalValue().signum() <= 0)
      return "Contribuição precisa ser positiva após custos e reembolsos.";
    if (!metrics.path("deliveryVerified").asBoolean()
        || !metrics.path("useVerified").asBoolean()
        || !metrics.path("satisfactionVerified").asBoolean())
      return "Comprove entrega, uso e satisfação antes de escalar.";
    return null;
  }

  /** Valida uma fotografia cumulativa do experimento com fontes e denominadores explícitos. */
  public static void validateMetrics(LearningSalesCycle cycle, JsonNode evidence, Instant now) {
    require(
        evidence.path("experimentId").canConvertToLong()
            && evidence.path("experimentId").asLong() == cycle.getExperimentId(),
        "A leitura deve pertencer ao experimento deste ciclo.");
    require(
        "BRL".equals(text(evidence, "currency")),
        "As métricas monetárias deste contrato usam BRL.");
    require(
        evidence.path("testDataExcluded").isBoolean() && evidence.path("dataValid").isBoolean(),
        "Declare a qualidade dos dados e a segregação do tráfego de teste.");
    for (String field :
        List.of("sessions", "starts", "firstResults", "checkouts", "netSales", "refunds")) {
      require(
          evidence.path(field).isIntegralNumber()
              && evidence.path(field).canConvertToLong()
              && evidence.path(field).asLong(-1) >= 0,
          "Informe uma contagem válida para " + field + ".");
    }
    for (String field : List.of("spendBrl", "revenueBrl", "contributionBrl")) {
      require(
          evidence.path(field).isNumber() && evidence.path(field).decimalValue().precision() <= 14,
          "Informe um valor monetário válido para " + field + ".");
    }
    require(
        evidence.path("spendBrl").decimalValue().signum() >= 0
            && evidence.path("revenueBrl").decimalValue().signum() >= 0,
        "Gasto e receita não podem ser negativos.");
    require(
        evidence
                .path("contributionBrl")
                .decimalValue()
                .compareTo(evidence.path("revenueBrl").decimalValue())
            <= 0,
        "A contribuição não pode superar a receita líquida registrada.");
    text(evidence, "source");
    Instant start = parseInstant(evidence, "periodStart");
    Instant end = parseInstant(evidence, "periodEnd");
    Instant observed = parseInstant(evidence, "observedAt");
    require(
        !start.isBefore(cycle.getWindowStart())
            && end.isAfter(start)
            && !end.isAfter(cycle.getWindowEnd())
            && !end.isAfter(observed)
            && !observed.isAfter(now),
        "Período e horário da leitura devem estar dentro do ciclo e não podem estar no futuro.");
  }

  /** Extrai um campo textual obrigatório sem aceitar tipo ou tamanho arbitrário. */
  public static String text(JsonNode data, String field) {
    require(
        data.path(field).isTextual()
            && !data.path(field).asText().isBlank()
            && data.path(field).asText().length() <= 4000,
        "Informe " + field + " com evidência clara.");
    return data.path(field).asText().trim();
  }

  /** Converte um instante ISO, retornando um erro contratual legível quando inválido. */
  public static Instant parseInstant(JsonNode data, String field) {
    String value = text(data, field);
    try {
      return Instant.parse(value);
    } catch (java.time.format.DateTimeParseException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Data inválida: " + field + ".", ex);
    }
  }

  /** Recusa uma condição funcional sem executar a transição parcialmente. */
  public static void require(boolean condition, String message) {
    if (!condition) throw new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
