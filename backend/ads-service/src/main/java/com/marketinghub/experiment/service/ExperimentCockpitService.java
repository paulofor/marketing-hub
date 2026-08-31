package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsDto;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.funnel.ExperimentFunnelDiagnosticService;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDto;
import com.marketinghub.experiment.service.cockpit.ExperimentCockpitActionDto;
import com.marketinghub.experiment.service.cockpit.ExperimentCockpitBottleneckDto;
import com.marketinghub.experiment.service.cockpit.ExperimentCockpitDto;
import com.marketinghub.experiment.service.cockpit.ExperimentCockpitFunnelStageDto;
import com.marketinghub.experiment.service.cockpit.ExperimentCockpitHealthDto;
import com.marketinghub.experiment.service.cockpit.ExperimentCockpitQuestionDto;
import com.marketinghub.experiment.service.cockpit.ExperimentCockpitScoreboardDto;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Serviço que consolida a leitura comercial de venda de um experimento. */
@Service
public class ExperimentCockpitService {

  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
  private static final long MINIMUM_IMPRESSIONS_TO_JUDGE_AD = 200L;

  private final ExperimentRepository experimentRepository;
  private final ExperimentReadinessService readinessService;
  private final ExperimentDiagnosticsService diagnosticsService;
  private final ExperimentFunnelService funnelService;
  private final ExperimentFunnelDiagnosticService funnelDiagnosticService;

  /** Inicializa o cockpit com fontes canônicas de experimento, prontidão, funil e diagnóstico. */
  public ExperimentCockpitService(
      ExperimentRepository experimentRepository,
      ExperimentReadinessService readinessService,
      ExperimentDiagnosticsService diagnosticsService,
      ExperimentFunnelService funnelService,
      ExperimentFunnelDiagnosticService funnelDiagnosticService) {
    this.experimentRepository = experimentRepository;
    this.readinessService = readinessService;
    this.diagnosticsService = diagnosticsService;
    this.funnelService = funnelService;
    this.funnelDiagnosticService = funnelDiagnosticService;
  }

  /** Monta a visão consolidada do cockpit para um experimento. */
  public ExperimentCockpitDto getCockpit(Long experimentId) {
    Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
    if (experiment.getExperimentType() == ExperimentType.FAKE_EXPERIMENT) {
      return buildFakeCockpit(experiment, experimentId);
    }
    ExperimentReadinessSummaryDto readiness = readinessService.summarize(experimentId);
    ExperimentLandingAnalyticsDto analytics = funnelService.summarizeLandingAnalytics(experimentId);
    List<ExperimentFunnelStageDto> funnelStages = funnelService.summarize(experimentId);
    List<ExperimentFunnelStageDto> commercialFunnelStages =
        commercialFunnelFor(experiment, funnelStages);
    ExperimentFunnelDiagnosticsResponseDto funnelDiagnostics =
        funnelDiagnosticService.diagnose(experimentId);
    ExperimentDiagnosticsDto experimentDiagnostics = diagnosticsService.diagnose(experimentId);
    BigDecimal revenue = funnelService.approvedRevenue(experimentId);
    ExperimentCockpitScoreboardDto scoreboard =
        buildScoreboard(experiment, commercialFunnelStages, analytics, revenue);
    List<String> metricIntegrityIssues = diagnoseMetricIntegrity(analytics, funnelStages);
    ExperimentCockpitBottleneckDto bottleneck =
        diagnoseBottleneck(
            experiment, readiness, funnelDiagnostics, scoreboard, metricIntegrityIssues);
    return new ExperimentCockpitDto(
        experiment.getId(),
        experiment.getName(),
        valueOf(experiment.getStatus()),
        valueOf(experiment.getExperimentType()),
        valueOf(experiment.getCampaignObjective()),
        scoreboard,
        buildQuestion(experiment),
        buildHealth(experiment, readiness, experimentDiagnostics, metricIntegrityIssues),
        commercialFunnelStages.stream()
            .sorted(Comparator.comparingInt(ExperimentFunnelStageDto::getOrder))
            .map(this::toCockpitStage)
            .toList(),
        bottleneck,
        buildLearnings(experiment, funnelDiagnostics),
        buildNextActions(experiment.getId(), bottleneck));
  }

  /** Monta uma leitura de teste para validar cockpit, PDE, vídeo e métricas sem campanha real. */
  private ExperimentCockpitDto buildFakeCockpit(Experiment experiment, Long experimentId) {
    ExperimentLandingAnalyticsDto analytics = funnelService.summarizeLandingAnalytics(experimentId);
    List<ExperimentFunnelStageDto> funnelStages = funnelService.summarize(experimentId);
    ExperimentCockpitScoreboardDto scoreboard =
        buildScoreboard(experiment, funnelStages, analytics, BigDecimal.ZERO);
    ExperimentCockpitBottleneckDto bottleneck =
        bottleneck(
            "FAKE_PDE_VIDEO_METRICAS",
            "Teste fake ativo para validar PDE, vídeo e métricas",
            "info",
            "Este experimento não representa resposta real de mercado; os números devem vir de acessos técnicos atribuídos ao próprio experimento.",
            "Permite validar se o PDE separa corretamente os acessos do experimento sem gastar mídia nem matar uma oferta por falha técnica.",
            "Acessar o PDE com UTM do experimento, executar vídeo, avançar até checkout simulado e comparar as métricas exibidas.");
    return new ExperimentCockpitDto(
        experiment.getId(),
        experiment.getName(),
        valueOf(experiment.getStatus()),
        valueOf(experiment.getExperimentType()),
        valueOf(experiment.getCampaignObjective()),
        scoreboard,
        buildQuestion(experiment),
        new ExperimentCockpitHealthDto(
            "SIMULATED",
            "Experimento fake: leitura comercial bloqueada",
            "Use este cockpit apenas para validar acesso, vídeo e métricas; não interprete como venda, rejeição ou aprovação de oferta.",
            List.of("Campanha real desabilitada para este tipo de experimento.")),
        funnelStages.stream()
            .sorted(Comparator.comparingInt(ExperimentFunnelStageDto::getOrder))
            .map(this::toCockpitStage)
            .toList(),
        bottleneck,
        List.of(
            "Métricas fake devem ser segregadas por UTM do experimento; 78 e 79 não podem compartilhar a mesma contagem.",
            "Nenhuma métrica deste experimento deve alimentar decisão de escala, pausa ou descarte comercial."),
        buildNextActions(experiment.getId(), bottleneck));
  }

  /** Calcula o placar financeiro e de conversão a partir de métricas persistidas. */
  private ExperimentCockpitScoreboardDto buildScoreboard(
      Experiment experiment,
      List<ExperimentFunnelStageDto> funnelStages,
      ExperimentLandingAnalyticsDto analytics,
      BigDecimal revenue) {
    ExperimentCampaignMetric metric = experiment.getCampaignMetric();
    BigDecimal spend = money(metric != null ? metric.getSpend() : null);
    boolean awaitingVerifiedExposure = isAwaitingVerifiedExposure(metric);
    long measuredPageViews =
        awaitingVerifiedExposure
            ? 0L
            : Math.max(
                analytics.pageViews(),
                stageTotal(funnelStages, ExperimentFunnelStage.VISUALIZACAO_FORM));
    long leads = stageTotal(funnelStages, ExperimentFunnelStage.ENVIO_FORM);
    long partialVideoViews = stageTotal(funnelStages, ExperimentFunnelStage.VIDEO_VISTO_PARCIAL);
    long completeVideoViews = stageTotal(funnelStages, ExperimentFunnelStage.VIDEO_VISTO_COMPLETO);
    long checkoutAccesses = stageTotal(funnelStages, ExperimentFunnelStage.ACESSO_CHECKOUT);
    long purchases = stageTotal(funnelStages, ExperimentFunnelStage.COMPRA);
    Long impressions = metric != null ? metric.getImpressions() : null;
    Long clicks = metric != null ? metric.getClicks() : null;
    return new ExperimentCockpitScoreboardDto(
        spend,
        revenue,
        money(revenue).subtract(money(spend)),
        divide(revenue, spend),
        impressions,
        clicks,
        percentage(clicks, impressions),
        clicks != null && clicks > 0 && metric != null ? metric.getCpc() : null,
        measuredPageViews,
        partialVideoViews,
        completeVideoViews,
        leads,
        checkoutAccesses,
        purchases,
        divide(spend, leads),
        divide(spend, checkoutAccesses),
        divide(spend, purchases));
  }

  /** Monta a pergunta comercial principal do experimento. */
  private ExperimentCockpitQuestionDto buildQuestion(Experiment experiment) {
    return new ExperimentCockpitQuestionDto(
        firstText(experiment.getSinglePain(), experiment.getHypothesis()),
        experiment.getFunnelPromise(),
        valueOf(experiment.getProductAiSubtype()),
        firstText(experiment.getFreeReward(), valueOf(experiment.getExperimentType())),
        experiment.getPrimaryCta(),
        experiment.getPrimaryVariable(),
        experiment.getPrimaryMetric());
  }

  /** Resume a prontidão operacional que define se a leitura de mercado pode ser interpretada. */
  private ExperimentCockpitHealthDto buildHealth(
      Experiment experiment,
      ExperimentReadinessSummaryDto readiness,
      ExperimentDiagnosticsDto diagnostics,
      List<String> metricIntegrityIssues) {
    List<String> blockers =
        new ArrayList<>(
            readiness.issues().stream()
                .map(issue -> firstText(issue.description(), issue.recommendation(), issue.title()))
                .filter(StringUtils::hasText)
                .toList());
    blockers.addAll(metricIntegrityIssues);
    if (blockers.isEmpty()) {
      if (isDirectOneToOne(experiment)) {
        return new ExperimentCockpitHealthDto(
            "READY",
            "Canal individual pronto para operação",
            "O piloto DIRECT_ONE_TO_ONE opera sem campanha Meta ou orçamento diário. A leitura comercial começa nos contatos consentidos e mantém o tráfego de QA fora das métricas humanas.",
            blockers);
      }
      return new ExperimentCockpitHealthDto(
          "READY",
          firstText(diagnostics.headline(), "Experimento pronto para leitura comercial"),
          firstText(
              diagnostics.description(),
              "Os bloqueios conhecidos de publicação e mensuração não estão impedindo a leitura."),
          blockers);
    }
    return new ExperimentCockpitHealthDto(
        "BLOCKED",
        "Execução ainda não deve ser interpretada como rejeição do mercado",
        "Corrija os bloqueios antes de decidir se a oferta vende ou não vende.",
        blockers);
  }

  /** Decide o gargalo principal priorizando falha técnica, compra e etapa mais próxima da venda. */
  private ExperimentCockpitBottleneckDto diagnoseBottleneck(
      Experiment experiment,
      ExperimentReadinessSummaryDto readiness,
      ExperimentFunnelDiagnosticsResponseDto funnelDiagnostics,
      ExperimentCockpitScoreboardDto scoreboard,
      List<String> metricIntegrityIssues) {
    if (!metricIntegrityIssues.isEmpty()) {
      return bottleneck(
          "INTEGRIDADE_METRICAS_DIVERGENTE",
          "Fontes do funil estão divergentes",
          "danger",
          metricIntegrityIssues.getFirst(),
          "O sistema não pode otimizar comunicação nem liberar amostra com contagens contraditórias.",
          "Reconciliar analytics, visitantes e funil antes de interpretar conversão.");
    }
    if (!readiness.issues().isEmpty()) {
      return bottleneck(
          "EXECUCAO_INVALIDA",
          "Execução inválida para leitura comercial",
          "danger",
          "Há pendências de publicação, tracking ou entrega antes do mercado ser julgado.",
          "Evita matar uma oferta por falha operacional.",
          "Corrigir prontidão e mensuração antes de gastar mais.");
    }
    if (scoreboard.impressions() != null && scoreboard.impressions() == 0) {
      return bottleneck(
          "AGUARDANDO_PRIMEIRA_IMPRESSAO",
          "Campanha publicada, aguardando a primeira impressão",
          "secondary",
          "A Meta ainda não confirmou exposição nem gasto da campanha.",
          "Eventos anteriores à primeira impressão permanecem na auditoria técnica, mas não podem orientar conversão, criativo ou oferta.",
          "Manter a campanha atual e iniciar a leitura comercial somente após a primeira impressão sincronizada.");
    }
    Optional<ExperimentFunnelStageDiagnosticDto> technicalIssue =
        funnelDiagnostics.diagnostics().stream()
            .filter(ExperimentFunnelStageDiagnosticDto::technicalIssueSuspected)
            .findFirst();
    if (technicalIssue.isPresent()) {
      ExperimentFunnelStageDiagnosticDto issue = technicalIssue.get();
      return bottleneck(
          "FALHA_TECNICA_FUNIL",
          "Possível falha técnica no funil",
          "danger",
          issue.message(),
          "Os números entre etapas podem estar distorcendo a decisão de venda.",
          "Corrigir tracking, eventos e sequência do funil.");
    }
    if (scoreboard.purchases() > 0) {
      return bottleneck(
          "VENDA_CONFIRMADA",
          "Venda confirmada",
          "success",
          "O experimento já registrou compra aprovada no funil.",
          "Existe sinal real de receita para comparar margem e escala.",
          "Analisar margem, criativos e próxima duplicação controlada.");
    }
    if (scoreboard.checkoutAccesses() > 0) {
      return bottleneck(
          "CHECKOUT_SEM_COMPRA",
          "Checkout recebeu intenção, mas não virou compra",
          "warning",
          "Houve acesso ao checkout sem compra aprovada registrada.",
          "A promessa criou intenção, mas preço, prova, garantia ou checkout podem estar travando"
              + " receita.",
          "Revisar preço, prova, garantia, urgência e experiência do checkout.");
    }
    if (scoreboard.leads() > 0) {
      return bottleneck(
          "LEAD_SEM_AVANCO_PARA_CHECKOUT",
          "Lead entrou, mas não avançou para compra",
          "warning",
          "O funil capturou leads, porém ainda não gerou acesso ao checkout.",
          "A captura funciona, mas a ponte para monetização está fraca.",
          "Ajustar amostra, e-mail, CTA de compra e oferta de entrada.");
    }
    if (scoreboard.completeVideoViews() > 0 || scoreboard.partialVideoViews() > 0) {
      return bottleneck(
          "VIDEO_SEM_PROXIMO_PASSO",
          "Vídeo engajou, mas não levou ao próximo passo",
          "warning",
          "Há consumo parcial ou completo do vídeo sem avanço para formulário, login, paywall ou checkout.",
          "A promessa consegue prender atenção, mas a ponte para monetização ainda não está clara ou forte o suficiente.",
          "Reforçar CTA pós-vídeo, promessa de continuidade e transição para diagnóstico, login ou pagamento.");
    }
    if (scoreboard.pageViews() > 0) {
      return bottleneck(
          "PAGINA_SEM_CONVERSAO",
          "Página recebeu tráfego, mas não converteu",
          "warning",
          "Há visualizações de página sem avanço funcional no funil.",
          "O clique chegou, mas primeira dobra, promessa, prova ou formulário não moveram a"
              + " pessoa.",
          "Criar nova primeira dobra e reforçar promessa, prova e CTA.");
    }
    if (scoreboard.clicks() != null && scoreboard.clicks() > 0) {
      return bottleneck(
          "CLIQUE_SEM_PAGINA",
          "Clique não virou sessão medida",
          "danger",
          "A campanha registrou clique, mas o analytics da página não registrou acesso.",
          "Pode haver quebra pós-clique, carregamento ruim ou tracking ausente.",
          "Validar URL, pixel, analytics, carregamento mobile e redirecionamento.");
    }
    if (scoreboard.impressions() != null
        && scoreboard.impressions() >= MINIMUM_IMPRESSIONS_TO_JUDGE_AD) {
      return bottleneck(
          "ANUNCIO_SEM_CLIQUE",
          "Anúncio expôs, mas não trouxe clique",
          "warning",
          "A campanha teve impressões sem clique suficiente para iniciar o funil.",
          "A dor, promessa, criativo ou público ainda não compraram atenção.",
          "Trocar ângulo criativo, visual e segmentação antes de mexer na página.");
    }
    if (scoreboard.impressions() != null && scoreboard.impressions() > 0) {
      return bottleneck(
          "AMOSTRA_INSUFICIENTE_ANUNCIO",
          "Anúncio ainda sem amostra suficiente",
          "secondary",
          "A campanha começou a gerar impressões, mas ainda não atingiu 200 exposições para avaliar ausência de cliques.",
          "Evita trocar um criativo por variação aleatória de uma amostra pequena.",
          "Manter o criativo atual e coletar dados até 200 impressões, preservando o limite financeiro do experimento.");
    }
    if (isDirectOneToOne(experiment)) {
      int sampleSize = experiment.getSampleSize() == null ? 0 : experiment.getSampleSize();
      return bottleneck(
          "AGUARDANDO_CONTATOS_DIRETOS",
          "Piloto direto pronto, ainda sem contatos medidos",
          "secondary",
          "O canal individual está ativo e não depende de campanha Meta, porém ainda não existem eventos humanos da amostra consentida.",
          "A instrumentação pode ser considerada pronta sem transformar QA em visita ou venda; o próximo aprendizado virá dos contatos reais autorizados.",
          sampleSize > 0
              ? "Registrar somente os contatos consentidos da amostra de %d e acompanhar avanço, pagamento, entrega e primeiro uso."
                  .formatted(sampleSize)
              : "Registrar a amostra consentida definida no contrato estratégico e acompanhar avanço, pagamento, entrega e primeiro uso.");
    }
    return bottleneck(
        "SEM_DADOS",
        "Ainda sem sinal comercial",
        "secondary",
        "Não há volume suficiente de exposição, clique ou sessão para decidir.",
        "A prioridade é colocar o experimento validamente diante do mercado.",
        "Publicar ou destravar distribuição e confirmar tracking.");
  }

  /** Bloqueia leitura comercial quando as fontes canônicas de pageview não fecham entre si. */
  private List<String> diagnoseMetricIntegrity(
      ExperimentLandingAnalyticsDto analytics, List<ExperimentFunnelStageDto> funnelStages) {
    if (analytics == null
        || analytics.visitors() == null
        || analytics.visitors().visitors() == null
        || analytics.visitors().probableVisitors() == 0) {
      return List.of();
    }
    long visitorPageViews =
        analytics.visitors().visitors().stream()
            .mapToLong(visitor -> visitor.validPageViews())
            .sum();
    long funnelPageViews = stageTotal(funnelStages, ExperimentFunnelStage.VISUALIZACAO_FORM);
    if (analytics.pageViews() == visitorPageViews && analytics.pageViews() == funnelPageViews) {
      return List.of();
    }
    return List.of(
        "Analytics registra %d pageviews humanas, visitantes normalizados registram %d e o funil registra %d."
            .formatted(analytics.pageViews(), visitorPageViews, funnelPageViews));
  }

  /** Cria um DTO de gargalo com os textos comerciais do cockpit. */
  private ExperimentCockpitBottleneckDto bottleneck(
      String code,
      String title,
      String severity,
      String diagnosis,
      String commercialImpact,
      String recommendedFocus) {
    return new ExperimentCockpitBottleneckDto(
        code, title, severity, diagnosis, commercialImpact, recommendedFocus);
  }

  /** Transforma etapa do funil no contrato enxuto do cockpit. */
  private ExperimentCockpitFunnelStageDto toCockpitStage(ExperimentFunnelStageDto stage) {
    return new ExperimentCockpitFunnelStageDto(
        stage.getStage().name(),
        stage.getLabel(),
        stage.getOrder(),
        stage.getTotalCount(),
        stage.getUniqueCount(),
        stage.getLastEventAt(),
        stage.getSource());
  }

  /** Exclui da leitura comercial eventos anteriores à primeira impressão sem apagar a auditoria. */
  private List<ExperimentFunnelStageDto> normalizeFunnelForCommercialReading(
      ExperimentCampaignMetric metric, List<ExperimentFunnelStageDto> stages) {
    if (!isAwaitingVerifiedExposure(metric)) {
      return stages;
    }
    return stages.stream().map(this::zeroPreExposureStage).toList();
  }

  /** Cria uma etapa comercial zerada enquanto os eventos permanecem no analytics detalhado. */
  private ExperimentFunnelStageDto zeroPreExposureStage(ExperimentFunnelStageDto source) {
    ExperimentFunnelStageDto stage = new ExperimentFunnelStageDto();
    stage.setStage(source.getStage());
    stage.setLabel(source.getLabel());
    stage.setOrder(source.getOrder());
    stage.setAutoCount(0L);
    stage.setManualCount(0L);
    stage.setTotalCount(0L);
    stage.setUniqueCount(source.getUniqueCount() == null ? null : 0L);
    stage.setLastEventAt(null);
    stage.setSource(
        "Aguardando primeira impressão confirmada pela Meta; eventos pré-exposição ficam no Analytics técnico.");
    return stage;
  }

  /** Indica que a Meta ainda não confirmou exposição para abrir a leitura comercial. */
  private boolean isAwaitingVerifiedExposure(ExperimentCampaignMetric metric) {
    return metric != null && (metric.getImpressions() == null || metric.getImpressions() <= 0);
  }

  /** Gera aprendizados persistidos e diagnósticos úteis para comparação de versões. */
  private List<String> buildLearnings(
      Experiment experiment, ExperimentFunnelDiagnosticsResponseDto diagnostics) {
    List<String> learnings = new ArrayList<>();
    if (StringUtils.hasText(experiment.getLearnedLessons())) {
      for (String line : experiment.getLearnedLessons().split("\\R")) {
        if (StringUtils.hasText(line)) {
          learnings.add(line.trim());
        }
      }
    }
    diagnostics.diagnostics().stream()
        .filter(
            diagnostic ->
                diagnostic.status() == FunnelDiagnosticStatus.STATISTICALLY_FAILED
                    || diagnostic.status() == FunnelDiagnosticStatus.WEAK_SIGNAL
                    || diagnostic.status() == FunnelDiagnosticStatus.TECHNICAL_ISSUE_SUSPECTED)
        .map(diagnostic -> diagnostic.stageLabel() + ": " + diagnostic.message())
        .forEach(learnings::add);
    if (learnings.isEmpty()) {
      learnings.add("Ainda não há aprendizado comercial persistido para este experimento.");
    }
    return learnings.stream().limit(8).toList();
  }

  /** Define ações recomendadas coerentes com o gargalo calculado pelo backend. */
  private List<ExperimentCockpitActionDto> buildNextActions(
      Long experimentId, ExperimentCockpitBottleneckDto bottleneck) {
    String experimentRoute = "/experiments/" + experimentId;
    return switch (bottleneck.code()) {
      case "VENDA_CONFIRMADA" ->
          List.of(
              action(
                  "ANALISAR_ESCALA",
                  "Analisar escala",
                  "Há compra registrada; a próxima decisão deve proteger margem antes de aumentar"
                      + " gasto.",
                  experimentRoute),
              action(
                  "DUPLICAR_VARIAVEL",
                  "Duplicar com nova variável",
                  "Use o aprendizado vencedor e teste uma mudança controlada.",
                  experimentRoute + "/edit"));
      case "CHECKOUT_SEM_COMPRA" ->
          List.of(
              action(
                  "REVISAR_OFERTA",
                  "Revisar oferta e preço",
                  "A intenção chegou ao checkout; a alavanca provável está em prova, preço,"
                      + " garantia ou atrito.",
                  experimentRoute + "/edit"),
              action(
                  "INSPECIONAR_FUNIL",
                  "Inspecionar funil",
                  "Confirme se compra, checkout e eventos finais estão sendo registrados"
                      + " corretamente.",
                  experimentRoute));
      case "PAGINA_SEM_CONVERSAO" ->
          List.of(
              action(
                  "NOVA_PRIMEIRA_DOBRA",
                  "Gerar nova primeira dobra",
                  "O clique chegou, mas a página não moveu a pessoa para a próxima ação.",
                  experimentRoute),
              action(
                  "REVISAR_PROMESSA",
                  "Revisar promessa e CTA",
                  "Fortaleça dor, transformação percebida e clareza do próximo passo.",
                  experimentRoute + "/edit"));
      case "VIDEO_SEM_PROXIMO_PASSO" ->
          List.of(
              action(
                  "REFORCAR_CTA_POS_VIDEO",
                  "Reforçar CTA pós-vídeo",
                  "O vídeo reteve atenção; a próxima alavanca é transformar esse momento em clique para diagnóstico, login ou compra.",
                  experimentRoute + "/edit"),
              action(
                  "CRIAR_PONTE_DE_CONTINUIDADE",
                  "Criar ponte de continuidade",
                  "Explicite o que a pessoa ganha imediatamente ao avançar depois do vídeo.",
                  experimentRoute));
      case "ANUNCIO_SEM_CLIQUE" ->
          List.of(
              action(
                  "TROCAR_CRIATIVO",
                  "Trocar criativo e ângulo",
                  "A oferta ainda não comprou atenção suficiente no feed.",
                  experimentRoute),
              action(
                  "REVISAR_PUBLICO",
                  "Revisar público",
                  "Valide se a segmentação conversa com a dor prioritária.",
                  experimentRoute + "/adset-workflow"));
      case "AMOSTRA_INSUFICIENTE_ANUNCIO" ->
          List.of(
              action(
                  "CONTINUAR_COLETA",
                  "Continuar coleta sem alterações",
                  "A amostra ainda não sustenta trocar criativo ou público.",
                  experimentRoute),
              action(
                  "ACOMPANHAR_ENTREGA",
                  "Acompanhar entrega",
                  "Reavalie o anúncio ao atingir 200 impressões ou se surgir bloqueio técnico ou financeiro.",
                  experimentRoute + "/facebook-api-logs"));
      case "AGUARDANDO_PRIMEIRA_IMPRESSAO" ->
          List.of(
              action(
                  "AGUARDAR_PRIMEIRA_IMPRESSAO",
                  "Aguardar a primeira impressão",
                  "A campanha está publicada, mas ainda não existe exposição confirmada para avaliar desempenho.",
                  experimentRoute + "/facebook-api-logs"),
              action(
                  "PRESERVAR_CONFIGURACAO_INICIAL",
                  "Preservar campanha e oferta",
                  "Não altere criativo, público ou página antes de existir sinal real da Meta.",
                  experimentRoute));
      case "AGUARDANDO_CONTATOS_DIRETOS" ->
          List.of(
              action(
                  "ACOMPANHAR_AMOSTRA_DIRETA",
                  "Acompanhar amostra consentida",
                  "O canal já está ativo; registre somente contatos aderentes e seus resultados reais, sem exigir campanha ou impressão Meta.",
                  experimentRoute),
              action(
                  "CONFERIR_EVENTOS_DIRETOS",
                  "Conferir eventos do canal direto",
                  "Preserve a separação entre QA, contato humano, checkout, pagamento, entrega e primeiro uso.",
                  experimentRoute));
      case "EXECUCAO_INVALIDA", "FALHA_TECNICA_FUNIL", "CLIQUE_SEM_PAGINA" ->
          List.of(
              action(
                  "CORRIGIR_TRACKING",
                  "Corrigir execução e tracking",
                  "A leitura comercial ainda pode estar contaminada por falha operacional.",
                  experimentRoute),
              action(
                  "VER_LOGS_FACEBOOK",
                  "Ver logs da campanha",
                  "Use evidência operacional antes de gastar ou matar a oferta.",
                  experimentRoute + "/facebook-api-logs"));
      default ->
          List.of(
              action(
                  "DESTRAVAR_DISTRIBUICAO",
                  "Destravar distribuição",
                  "O experimento precisa chegar validamente ao mercado para gerar aprendizado.",
                  experimentRoute),
              action(
                  "REVISAR_PRONTIDAO",
                  "Revisar prontidão",
                  "Confirme ativos, destino e mensuração antes de publicar.",
                  experimentRoute));
    };
  }

  /** Cria uma ação recomendada do cockpit. */
  private ExperimentCockpitActionDto action(
      String code, String label, String rationale, String targetRoute) {
    return new ExperimentCockpitActionDto(code, label, rationale, targetRoute);
  }

  /** Seleciona somente as etapas comerciais que pertencem ao canal vigente do experimento. */
  private List<ExperimentFunnelStageDto> commercialFunnelFor(
      Experiment experiment, List<ExperimentFunnelStageDto> stages) {
    if (isDirectOneToOne(experiment)) {
      return stages.stream()
          .filter(stage -> stage.getStage() != ExperimentFunnelStage.VISUALIZACAO_ANUNCIO)
          .filter(stage -> stage.getStage() != ExperimentFunnelStage.ACESSO_FORM_LEAD)
          .toList();
    }
    return normalizeFunnelForCommercialReading(experiment.getCampaignMetric(), stages);
  }

  /** Identifica o piloto individual que não utiliza mídia paga ou campanha Meta. */
  private boolean isDirectOneToOne(Experiment experiment) {
    return experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE;
  }

  /** Retorna o total de uma etapa do funil. */
  private long stageTotal(List<ExperimentFunnelStageDto> stages, ExperimentFunnelStage stage) {
    return stages.stream()
        .filter(item -> item.getStage() == stage)
        .mapToLong(ExperimentFunnelStageDto::getTotalCount)
        .findFirst()
        .orElse(0L);
  }

  /** Calcula percentual entre dois inteiros opcionais. */
  private BigDecimal percentage(Long numerator, Long denominator) {
    if (numerator == null || denominator == null || denominator == 0) {
      return null;
    }
    return BigDecimal.valueOf(numerator)
        .multiply(ONE_HUNDRED)
        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
  }

  /** Divide dinheiro por uma contagem inteira. */
  private BigDecimal divide(BigDecimal value, long denominator) {
    if (value == null || denominator <= 0) {
      return null;
    }
    return value.divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
  }

  /** Divide dois valores monetários opcionais. */
  private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
    if (numerator == null || denominator == null || BigDecimal.ZERO.compareTo(denominator) == 0) {
      return null;
    }
    return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
  }

  /** Normaliza valor monetário ausente para zero em operações aritméticas. */
  private BigDecimal money(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  /** Retorna o primeiro texto preenchido. */
  private String firstText(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  /** Converte enums e objetos opcionais para texto. */
  private String valueOf(Object value) {
    return value != null ? value.toString() : null;
  }
}
