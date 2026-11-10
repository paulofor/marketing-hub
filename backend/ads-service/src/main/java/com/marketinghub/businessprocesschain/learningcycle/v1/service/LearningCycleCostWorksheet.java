package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.CycleFinancialReview;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: versionar e calcular a planilha de custos usando o ledger do próprio ciclo. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleCostWorksheet {
  private static final String ACTION = "SAVE_COST_WORKSHEET";
  private static final String[] CODES = {
    "AGENTS", "PRODUCTION", "MEDIA", "DELIVERY", "SALES_DEDUCTIONS", "FIXED_OPERATION"
  };
  private static final String[] LABELS = {
    "Agentes",
    "Produção de imagens, áudio e vídeo",
    "Mídia",
    "Entrega e suporte",
    "Despesas e deduções da venda",
    "Operação fixa"
  };
  private static final String[] DESCRIPTIONS = {
    "Pesquisa, estratégia, construção e revisões. Inclui tentativas anteriores do produto.",
    "Geração de peças, locução, montagem e novas tentativas. Exclua revisões já contadas nos agentes.",
    "Pagamento às plataformas para divulgar anúncios.",
    "Personalização, IA utilizada para atender cada cliente e atendimento; informe o total do período.",
    "Taxas de pagamento, impostos, comissões e reembolsos; cada dedução entra uma única vez.",
    "Servidores, domínio, ferramentas e assinaturas atribuídos ao produto no período."
  };
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleJson json;
  private final ObjectMapper mapper;
  private final LearningCycleCostCeilings ceilings;

  /** Compõe seis grupos, subtotais separados e lacunas sem converter ausência em zero. */
  public CostWorksheet read(
      LearningSalesCycle cycle, CycleFinancialReview review, String productionBlocker) {
    var records = events.findByCycleIdAndActionOrderByRevisionDesc(cycle.getId(), ACTION);
    var current = records.stream().filter(e -> eligible(e, cycle)).findFirst().orElse(null);
    var saved = current == null ? null : decode(current);
    var ceilingHistory = ceilings.history(cycle);
    var authorizedMedia = mediaApproved(cycle, review.observedAt());
    List<CostWorksheet.Row> rows = new ArrayList<>();
    List<String> gaps = new ArrayList<>();
    if (saved == null) gaps.add("O sistema ainda não possui previsão completa para esta versão.");
    for (int i = 0; i < CODES.length; i++) {
      String code = CODES[i];
      var input =
          saved == null
              ? null
              : saved.lines().stream().filter(l -> code.equals(l.code())).findFirst().orElse(null);
      var ceiling =
          ceilingHistory.stream()
              .filter(r -> r.current() && code.equals(r.code()))
              .findFirst()
              .orElse(null);
      BigDecimal approved =
          switch (i) {
            case 0 ->
                review.authorization() == null ? null : review.authorization().totalLimitUsd();
            case 1 ->
                review.videoBudgetUsd() != null
                    ? review.videoBudgetUsd()
                    : ceiling == null ? null : ceiling.approved();
            case 2 ->
                authorizedMedia != null
                    ? authorizedMedia
                    : ceiling == null ? null : ceiling.approved();
            default ->
                ceiling != null
                    ? ceiling.approved()
                    : saved != null && saved.approveOperatingBudgets() && input != null
                        ? input.planned()
                        : null;
          };
      String executionBlocker =
          i == 1
              ? productionBlocker
              : i == 2 && !review.mediaDecision().available()
                  ? review.mediaDecision().reason()
                  : null;
      boolean operational =
          i == 1 ? review.videoBudgetUsd() != null : i == 2 && authorizedMedia != null;
      String approvalBlocker =
          !review.canAuthorize()
              ? review.authorizationBlocker()
              : operational && executionBlocker != null
                  ? "O limite operacional vigente está protegido nesta etapa. A alteração será feita nesta planilha quando o processo permitir."
                  : null;
      String approvalScope =
          (i == 1 || i == 2) && executionBlocker != null || i > 2 ? "PLANNING" : "EXECUTION_LIMIT";
      String approval =
          switch (i) {
            case 0 ->
                approved == null
                    ? "Sem aprovação vigente"
                    : "Recibo de preparação #" + review.authorization().eventId();
            case 1, 2 ->
                operational
                    ? "Limite operacional registrado; execução depende dos demais gates"
                    : approved != null
                        ? "Teto de planejamento salvo; aplicação operacional ainda não autorizada"
                        : "Teto ainda não registrado; você pode decidir aqui";
            default ->
                approved == null
                    ? "Orçamento ainda não aprovado"
                    : "Aprovado para planejamento; sem autorização de execução";
          };
      if (i == 0 && review.authorization() != null && review.authorization().carriedForward())
        approval =
            "Teto acumulado mantido do ciclo #"
                + review.authorization().sourceCycleId()
                + " · recibo #"
                + review.authorization().eventId()
                + ". Mudar de ciclo não exige nova aprovação do teto nem renova o saldo.";
      else if (i > 0 && !operational && ceiling != null && ceiling.carriedForward())
        approval =
            "Teto acumulado mantido do ciclo #"
                + ceiling.sourceCycleId()
                + " · recibo #"
                + ceiling.eventId()
                + ". Mudar de ciclo não exige nova aprovação do teto nem renova o saldo.";
      var forecast = input == null ? null : input.forecast();
      if (forecast == null)
        gaps.add(LABELS[i] + ": previsão total ainda não disponível no sistema.");
      var reported = input == null ? null : input.reportedCost();
      BigDecimal estimated = i == 0 ? review.knownPreparationUsd() : null;
      BigDecimal observed = reported == null ? estimated : reported;
      if (observed != null && forecast != null && forecast.compareTo(observed) < 0)
        gaps.add(LABELS[i] + ": previsão menor que o custo conhecido; revise a fonte e o total.");
      rows.add(
          new CostWorksheet.Row(
              code,
              LABELS[i],
              i < 2 ? "USD" : "BRL",
              i == 0
                  ? "Acumulado do produto até o fim do período"
                  : "Experimento atual no período informado",
              DESCRIPTIONS[i],
              input == null ? null : input.planned(),
              reported,
              estimated,
              approved,
              approval,
              forecast,
              i == 1 || approved == null || forecast == null ? null : approved.subtract(forecast),
              input == null ? null : input.source(),
              i == 0
                  ? "Estimativas de tarefas dos agentes; "
                      + review.unreportedTasks()
                      + " tarefas sem custo. Não é fatura conciliada."
                  : "Custo ainda não consolidado pelo sistema; ausência não é zero.",
              approvalBlocker == null,
              approvalBlocker,
              approvalScope,
              executionBlocker == null
                  ? null
                  : i == 1
                      ? "Teto decidido aqui. A aplicação à produção dos dois vídeos aguarda a etapa e o escopo compatíveis; confirme nesta planilha quando disponível. Imagens avulsas não recebem autorização por este teto."
                      : "Teto decidido aqui. A aplicação aos anúncios aguarda a etapa Autorizar orçamento; confirme nesta planilha quando disponível. Nenhuma campanha é ativada ao salvar."));
    }
    var totals = List.of(total("USD", rows), total("BRL", rows));
    BigDecimal result = null;
    if (saved == null || saved.expectedRevenueBrl() == null)
      gaps.add("Receita bruta prevista ainda não disponível no sistema.");
    BigDecimal dollars = totals.getFirst().forecast();
    boolean needsExchange = dollars != null && dollars.signum() > 0;
    if (needsExchange && (saved == null || saved.usdBrl() == null))
      gaps.add("O sistema ainda não possui câmbio com fonte para calcular o resultado em R$.");
    if (gaps.isEmpty())
      result =
          saved
              .expectedRevenueBrl()
              .subtract(totals.get(1).forecast())
              .subtract(dollars.multiply(needsExchange ? saved.usdBrl() : BigDecimal.ZERO))
              .setScale(2, RoundingMode.HALF_UP);
    var history =
        records.stream()
            .map(
                e -> {
                  var snapshot = decode(e);
                  return new CostWorksheet.Receipt(
                      e.getId(),
                      e.getRevision(),
                      e.getCreatedAt(),
                      e.getOperatorName(),
                      snapshot.productVersion(),
                      current != null && Objects.equals(e.getId(), current.getId()),
                      snapshot.approveOperatingBudgets(),
                      snapshot);
                })
            .toList();
    return new CostWorksheet(
        "COST_WORKSHEET_V1",
        current == null ? null : current.getId(),
        cycle.getRevision(),
        "OPEN".equals(cycle.getStatus()),
        saved,
        rows,
        totals,
        result,
        gaps,
        history,
        ceilingHistory);
  }

  /** Confere recibo e janela de mídia sem promover o teto cadastrado a aprovação. */
  private BigDecimal mediaApproved(LearningSalesCycle cycle, Instant now) {
    if (cycle.getWindowEnd() == null || !now.isBefore(cycle.getWindowEnd())) return null;
    return events.findByCycleIdAndActionOrderByRevisionDesc(cycle.getId(), "COMPLETE").stream()
        .filter(event -> "AUTHORIZATION".equals(event.getFromStage()))
        .filter(
            event ->
                cycle.getVersionChangedAt() == null
                    || !event.getCreatedAt().isBefore(cycle.getVersionChangedAt()))
        .map(event -> json.read(event.getEvidenceJson()))
        .filter(
            data ->
                data.path("confirmed").asBoolean()
                    && Objects.equals(
                        cycle.getProductVersion(), data.path("productVersion").asText()))
        .filter(
            data ->
                data.path("budgetLimitBrl").isNumber()
                    && cycle.getBudgetLimitBrl() != null
                    && data.path("budgetLimitBrl")
                            .decimalValue()
                            .compareTo(cycle.getBudgetLimitBrl())
                        == 0)
        .map(data -> data.path("budgetLimitBrl").decimalValue())
        .findFirst()
        .orElse(null);
  }

  /** Preserva recibo idempotente sob o lock do serviço, sem avançar etapa ou disparar gasto. */
  public void save(LearningSalesCycle cycle, CostWorksheetRequest request, Instant now) {
    require(request != null && request.requestKey() != null, "Informe a identidade da gravação.");
    require(
        Objects.equals(request.chainDefinitionId(), cycle.getChainDefinitionId())
            && Objects.equals(request.experimentId(), cycle.getExperimentId())
            && Objects.equals(request.productVersion(), cycle.getProductVersion()),
        "A planilha pertence a outro contexto ou versão.");
    String input = json.write(request);
    var replay = events.findByCycleIdAndRequestKey(cycle.getId(), request.requestKey().toString());
    if (replay.isPresent()) {
      require(
          ACTION.equals(replay.get().getAction())
              && json.read(input).equals(json.read(replay.get().getRequestJson())),
          "A chave já foi usada com outros valores.");
      return;
    }
    require(
        Objects.equals(request.expectedRevision(), cycle.getRevision()),
        "O ciclo mudou. Atualize os valores antes de salvar.");
    require(
        "OPEN".equals(cycle.getStatus()),
        "Ciclo encerrado: planilha disponível apenas para consulta.");
    validate(request);
    cycle.setRevision(cycle.getRevision() + 1);
    cycle.setUpdatedAt(now);
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRevision(cycle.getRevision());
    event.setRequestKey(request.requestKey().toString());
    event.setRequestJson(input);
    event.setEvidenceJson(input);
    event.setAction(ACTION);
    event.setFromStage(cycle.getStage());
    event.setToStage(cycle.getStage());
    event.setOperatorName(request.operatorName().trim());
    event.setSummary(
        request.approveOperatingBudgets()
            ? "Planilha salva; orçamentos de entrega, deduções e operação aprovados para planejamento. Sem liberação de gasto."
            : "Planilha de custos e previsões salva, sem alteração de autorizações de gasto.");
    event.setEvidenceReference(
        "internal://learning-cycles/" + cycle.getId() + "/cost-worksheet/" + request.requestKey());
    event.setCreatedAt(now);
    events.saveAndFlush(event);
    log.info(
        "Planilha financeira salva productId={} cycleId={} eventId={} revision={} operatingBudgetsApproved={}",
        cycle.getProductId(),
        cycle.getId(),
        event.getId(),
        cycle.getRevision(),
        request.approveOperatingBudgets());
  }

  /** Valida moeda implícita, fontes, cobertura, período e confirmação administrativa. */
  private void validate(CostWorksheetRequest request) {
    require(
        request.periodStart() != null
            && request.periodEnd() != null
            && !request.periodEnd().isBefore(request.periodStart()),
        "Informe início e fim válidos do período.");
    require(
        text(request.operatorName(), 160) && text(request.justification(), 4000),
        "Informe responsável e justificativa com as premissas de receita e custos.");
    amount(request.expectedRevenueBrl());
    require(
        request.lines() != null
            && request.lines().size() == 6
            && request.lines().stream().allMatch(Objects::nonNull)
            && new HashSet<>(request.lines().stream().map(CostWorksheetRequest.Line::code).toList())
                .equals(new HashSet<>(List.of(CODES))),
        "Informe exatamente os seis grupos padrão, sem duplicação.");
    for (var line : request.lines()) {
      amount(line.planned());
      amount(line.reportedCost());
      amount(line.forecast());
      require(
          line.source() == null || line.source().length() <= 2000,
          "Fonte da linha excede 2000 caracteres.");
      if (line.planned() != null || line.reportedCost() != null || line.forecast() != null)
        require(
            text(line.source(), 2000),
            "Informe fonte e premissas para cada grupo com valores, inclusive zero.");
      if (request.approveOperatingBudgets()
          && List.of("DELIVERY", "SALES_DEDUCTIONS", "FIXED_OPERATION").contains(line.code()))
        require(
            line.planned() != null,
            "Informe os três orçamentos de entrega, deduções e operação antes de aprovar.");
    }
    if (request.usdBrl() != null)
      require(
          request.usdBrl().signum() > 0
              && request.usdBrl().scale() <= 6
              && request.usdBrl().compareTo(new BigDecimal("10000")) <= 0
              && text(request.exchangeSource(), 1000)
              && request.exchangeCheckedOn() != null
              && !request
                  .exchangeCheckedOn()
                  .isAfter(java.time.LocalDate.now(java.time.ZoneOffset.UTC)),
          "Informe câmbio positivo, fonte e data de consulta válida.");
  }

  /** Recusa valores negativos, precisão excessiva e magnitudes incompatíveis com a planilha. */
  private void amount(BigDecimal value) {
    require(
        value == null
            || (value.signum() >= 0
                && value.scale() <= 2
                && value.compareTo(new BigDecimal("999999999.99")) <= 0),
        "Valores devem ser não negativos, com até duas casas decimais.");
  }

  /** Exige evidência curta e não vazia antes de aceitar declaração administrativa. */
  private boolean text(String value, int max) {
    return value != null && !value.isBlank() && value.length() <= max;
  }

  /** Impede adoção silenciosa de premissas de outra versão, experimento ou cadeia. */
  private boolean eligible(LearningSalesCycleEvent event, LearningSalesCycle cycle) {
    var request = decode(event);
    return Objects.equals(request.productVersion(), cycle.getProductVersion())
        && Objects.equals(request.experimentId(), cycle.getExperimentId())
        && Objects.equals(request.chainDefinitionId(), cycle.getChainDefinitionId())
        && (cycle.getVersionChangedAt() == null
            || !event.getCreatedAt().isBefore(cycle.getVersionChangedAt()));
  }

  /** Recupera a evidência estruturada, preservando contexto e stack trace em caso de corrupção. */
  private CostWorksheetRequest decode(LearningSalesCycleEvent event) {
    try {
      return mapper.treeToValue(json.read(event.getEvidenceJson()), CostWorksheetRequest.class);
    } catch (Exception ex) {
      log.error(
          "Falha ao ler planilha cycleId={} eventId={}", event.getCycleId(), event.getId(), ex);
      throw new IllegalStateException("A planilha persistida não pôde ser lida.", ex);
    }
  }

  /** Soma somente valores conhecidos da mesma moeda e informa as lacunas de cada coluna. */
  private CostWorksheet.Total total(String currency, List<CostWorksheet.Row> rows) {
    var selected = rows.stream().filter(r -> currency.equals(r.currency())).toList();
    return new CostWorksheet.Total(
        currency,
        sum(selected, CostWorksheet.Row::planned),
        sum(selected, CostWorksheet.Row::reportedCost),
        sum(selected, CostWorksheet.Row::forecast),
        missing(selected, CostWorksheet.Row::planned),
        missing(selected, CostWorksheet.Row::reportedCost),
        missing(selected, CostWorksheet.Row::forecast));
  }

  /** Mantém subtotal desconhecido quando nenhuma parcela tem fonte monetária. */
  private BigDecimal sum(
      List<CostWorksheet.Row> rows, Function<CostWorksheet.Row, BigDecimal> field) {
    return rows.stream().map(field).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(null);
  }

  /** Conta linhas ausentes sem fabricar valor zero. */
  private int missing(List<CostWorksheet.Row> rows, Function<CostWorksheet.Row, BigDecimal> field) {
    return (int) rows.stream().map(field).filter(Objects::isNull).count();
  }
}
